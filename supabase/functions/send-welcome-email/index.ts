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
    ? "Da qui crei le schede per i tuoi allievi e segui i loro progressi in tempo reale."
    : "Il tuo personal trainer ti assegnerà le schede e seguirà i tuoi allenamenti da qui.";
  const checklist = role === "PT"
    ? [
      "Genera il tuo codice invito dal Profilo e condividilo con i tuoi allievi",
      "Crea la prima scheda, anche con l'aiuto dell'assistente AI",
      "Tieni d'occhio chi è attivo e chi si è fermato dalla tua dashboard",
    ]
    : [
      "Collega il tuo PT con il codice invito che ti ha dato",
      "Registra il tuo primo allenamento non appena ricevi una scheda",
      "Esplora sedi e corsi ViBE dalle azioni rapide della Home",
    ];
  const checklistHtml = checklist
    .map((item) => `
      <tr>
        <td style="padding:6px 0;vertical-align:top;width:20px;color:#f76b15;font-weight:700">&#8226;</td>
        <td style="padding:6px 0;color:#c8c8d0;font-size:14px;line-height:1.5">${item}</td>
      </tr>`)
    .join("");

  return `
    <div style="font-family:-apple-system,Segoe UI,Roboto,sans-serif;max-width:480px;margin:0 auto;padding:36px 28px;background:#0f0f14;color:#f5f5f7;border-radius:20px">
      <p style="font-size:22px;font-weight:800;color:#f76b15;margin:0 0 24px;letter-spacing:-0.5px">ViBE</p>
      <h1 style="font-size:21px;margin:0 0 12px;font-weight:700">Benvenuto, ${firstName}!</h1>
      <p style="font-size:15px;line-height:1.6;color:#c8c8d0;margin:0 0 22px">${roleLine}</p>
      <table role="presentation" style="width:100%;border-collapse:collapse;margin-bottom:8px">
        ${checklistHtml}
      </table>
      <p style="font-size:13px;line-height:1.6;color:#8a8a95;margin-top:28px">
        Se non hai creato tu questo account, ignora pure questa email: non è stato attivato nulla.
      </p>
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
