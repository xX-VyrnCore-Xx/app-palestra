// Sends the post-registration welcome email via Resend.
//
// Called by the app right after signUp() succeeds. Best-effort: registration must never fail or
// block on this, so the caller fires it and ignores the result (same pattern as send-push). Needs
// the RESEND_API_KEY secret set on this project (supabase secrets set RESEND_API_KEY=... or via
// the dashboard's Edge Functions > Secrets page) and a sender address on a domain verified in
// Resend; until both are set this quietly no-ops rather than erroring, since Supabase Auth's own
// built-in email already covers verification/reset mail - this is purely a nicer branded welcome.
import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

interface WelcomeEmailRequest {
  fullName: string;
  role: "PT" | "ALLIEVO";
}

function corsHeaders(): HeadersInit {
  return {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  };
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders(), "Content-Type": "application/json" },
  });
}

function welcomeHtml(fullName: string, role: "PT" | "ALLIEVO"): string {
  const firstName = fullName.trim().split(" ")[0] || "a bordo";
  const roleLine = role === "PT"
    ? "Da qui puoi creare le schede per i tuoi allievi e seguirne i progressi in tempo reale."
    : "Il tuo personal trainer potrà assegnarti schede e seguire i tuoi allenamenti da qui.";
  return `
    <div style="font-family:-apple-system,Segoe UI,Roboto,sans-serif;max-width:480px;margin:0 auto;padding:32px 24px;background:#0f0f14;color:#f5f5f7;border-radius:16px">
      <h1 style="font-size:22px;margin:0 0 12px">Benvenuto su Vibe Fitness, ${firstName}!</h1>
      <p style="font-size:15px;line-height:1.5;color:#c8c8d0">${roleLine}</p>
      <p style="font-size:13px;line-height:1.5;color:#8a8a95;margin-top:24px">Se non hai creato tu questo account, ignora pure questa email.</p>
    </div>`;
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders() });
  if (req.method !== "POST") return jsonResponse({ error: "Method not allowed" }, 405);

  const authHeader = req.headers.get("Authorization");
  if (!authHeader) return jsonResponse({ error: "Missing Authorization header" }, 401);

  const resendApiKey = Deno.env.get("RESEND_API_KEY");
  const fromAddress = Deno.env.get("RESEND_FROM_ADDRESS");
  // Not configured yet: a no-op, not an error - Supabase Auth's own email still covers the
  // functional part (verification/reset); this is an optional branded add-on.
  if (!resendApiKey || !fromAddress) return jsonResponse({ skipped: "resend not configured" }, 200);

  const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
  const userClient = createClient(supabaseUrl, anonKey, {
    global: { headers: { Authorization: authHeader } },
  });
  const { data: userData, error: userError } = await userClient.auth.getUser();
  if (userError || !userData.user?.email) return jsonResponse({ error: "Invalid session" }, 401);

  let body: WelcomeEmailRequest;
  try {
    body = await req.json();
  } catch {
    return jsonResponse({ error: "Invalid JSON body" }, 400);
  }
  if (!body.fullName || !body.role) return jsonResponse({ error: "Missing fullName/role" }, 400);

  const resendResponse = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${resendApiKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      from: fromAddress,
      to: userData.user.email,
      subject: "Benvenuto su Vibe Fitness",
      html: welcomeHtml(body.fullName, body.role),
    }),
  });

  if (!resendResponse.ok) {
    console.error("Resend error", resendResponse.status, await resendResponse.text());
    return jsonResponse({ error: "Email delivery failed" }, 502);
  }

  return jsonResponse({ sent: true });
});
