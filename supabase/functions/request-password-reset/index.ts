// Password-reset request proxy for Vibe Fitness.
//
// Called by the app when the user taps "Password dimenticata?" - BEFORE they're signed in, so
// this function has no JWT to verify (verify_jwt must stay false when deploying it). Generates
// the recovery link itself via the Admin API and emails it through Resend with a branded template
// instead of Supabase Auth's default plain email; if Resend isn't configured on this project yet,
// falls back to Supabase's own built-in reset email so the feature never silently breaks.
//
// Always responds with the same generic message regardless of whether the email is registered,
// rate-limited, or already sent recently - a differing response would let an attacker enumerate
// which addresses have an account just by trying them here one at a time.
import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const RATE_LIMIT_PER_MINUTE = 20;
const PER_EMAIL_COOLDOWN_SECONDS = 60;
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

interface ResetRequestBody {
  email?: string;
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

/** Never stores the raw email in the rate-limit table - only enough to recognize a repeat
 * request for the same address within the cooldown window. */
async function sha256Hex(input: string): Promise<string> {
  const data = new TextEncoder().encode(input.trim().toLowerCase());
  const digest = await crypto.subtle.digest("SHA-256", data);
  return Array.from(new Uint8Array(digest)).map((b) => b.toString(16).padStart(2, "0")).join("");
}

function resetPasswordHtml(actionLink: string): string {
  return `
    <div style="font-family:-apple-system,Segoe UI,Roboto,sans-serif;max-width:480px;margin:0 auto;padding:36px 28px;background:#0f0f14;color:#f5f5f7;border-radius:20px">
      <p style="font-size:22px;font-weight:800;color:#f76b15;margin:0 0 24px;letter-spacing:-0.5px">ViBE</p>
      <h1 style="font-size:20px;margin:0 0 12px;font-weight:700">Reimposta la tua password</h1>
      <p style="font-size:15px;line-height:1.6;color:#c8c8d0;margin:0 0 24px">
        Hai chiesto di reimpostare la password del tuo account Vibe Fitness. Tocca il pulsante qui
        sotto per sceglierne una nuova - il link è valido per un'ora.
      </p>
      <a href="${actionLink}"
         style="display:inline-block;padding:14px 30px;background:#f76b15;color:#ffffff;
                text-decoration:none;font-weight:700;font-size:15px;border-radius:12px">
        Reimposta password
      </a>
      <p style="font-size:13px;line-height:1.6;color:#8a8a95;margin-top:32px">
        Se non hai richiesto tu questa email, ignorala pure: la tua password resta invariata e
        nessun'altra azione è necessaria.
      </p>
    </div>`;
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders() });
  if (req.method !== "POST") return jsonResponse({ error: "Method not allowed" }, 405);

  let body: ResetRequestBody;
  try {
    body = await req.json();
  } catch {
    return jsonResponse({ error: "Invalid JSON body" }, 400);
  }
  const email = (body.email ?? "").trim().toLowerCase();
  if (!EMAIL_RE.test(email)) return jsonResponse({ error: "Indirizzo email non valido" }, 400);

  const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
  const resendApiKey = Deno.env.get("RESEND_API_KEY");
  const fromAddress = Deno.env.get("RESEND_FROM_ADDRESS");
  // Points straight at the Android app's own deep link (see MainActivity's "vibefitness" intent
  // filter) rather than a hosted web page: Supabase's Edge Function gateway forces
  // `Content-Type: text/plain` and a `sandbox` CSP on every function response, which blocks
  // inline JavaScript - so a function-hosted "set new password" page can never actually run the
  // script that would read the token and call the API. The app itself is a more solid target
  // than fighting that platform restriction with a separately-hosted static site.
  const redirectTo = Deno.env.get("PASSWORD_RESET_REDIRECT_URL") ?? "vibefitness://reset-password";

  const admin = createClient(supabaseUrl, serviceRoleKey);

  const genericResponse = jsonResponse({
    message: "Se l'indirizzo esiste, riceverai a breve un'email con le istruzioni per reimpostare la password.",
  });

  const oneMinuteAgo = new Date(Date.now() - 60_000).toISOString();
  const { count: globalCount, error: globalCountError } = await admin
    .from("password_reset_events")
    .select("id", { count: "exact", head: true })
    .gte("created_at", oneMinuteAgo);
  if (globalCountError) return jsonResponse({ error: "Rate limit check failed" }, 500);
  // Fails closed without revealing that a limit was hit - same generic message either way.
  if ((globalCount ?? 0) >= RATE_LIMIT_PER_MINUTE) return genericResponse;

  const emailHash = await sha256Hex(email);
  const cooldownSince = new Date(Date.now() - PER_EMAIL_COOLDOWN_SECONDS * 1000).toISOString();
  const { count: recentForEmail } = await admin
    .from("password_reset_events")
    .select("id", { count: "exact", head: true })
    .eq("email_hash", emailHash)
    .gte("created_at", cooldownSince);

  await admin.from("password_reset_events").insert({ email_hash: emailHash });
  // Best-effort cleanup so the table doesn't grow unbounded (same pattern as ai_rate_limit_events).
  admin.from("password_reset_events").delete().lt("created_at", new Date(Date.now() - 3600_000).toISOString()).then();

  if ((recentForEmail ?? 0) > 0) return genericResponse; // already sent one for this address recently

  if (resendApiKey && fromAddress) {
    // Preferred path: mint the recovery link ourselves so Supabase's own email is never sent,
    // and deliver it through the branded Resend template instead.
    const { data, error } = await admin.auth.admin.generateLink({
      type: "recovery",
      email,
      options: { redirectTo },
    });
    const actionLink = data?.properties?.action_link;
    if (!error && actionLink) {
      const resendResponse = await fetch("https://api.resend.com/emails", {
        method: "POST",
        headers: { Authorization: `Bearer ${resendApiKey}`, "Content-Type": "application/json" },
        body: JSON.stringify({
          from: fromAddress,
          to: email,
          subject: "Reimposta la tua password ViBE Fitness",
          html: resetPasswordHtml(actionLink),
        }),
      });
      if (!resendResponse.ok) {
        console.error("Resend error", resendResponse.status, await resendResponse.text());
      }
    }
    // If the address isn't registered, generateLink errors here - swallowed on purpose, the
    // caller always gets genericResponse regardless.
    return genericResponse;
  }

  // Resend not configured on this project yet: fall back to Supabase Auth's own built-in reset
  // email (plain, but guaranteed to work) rather than silently doing nothing.
  const anonClient = createClient(supabaseUrl, anonKey);
  await anonClient.auth.resetPasswordForEmail(email, { redirectTo });
  return genericResponse;
});
