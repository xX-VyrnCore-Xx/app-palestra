// AI assistant proxy for Vibe Fitness.
//
// Keeps the NVIDIA NIM API key server-side only (never shipped in the Android app, which would
// let it be extracted from the APK). Two distinct assistants - PT and Allievo - with separate
// system prompts, each only ever answering with data scoped to the calling user via RLS.
// Enforces a global sliding-window rate limit (NIM account is capped at 40 requests/minute).
import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const NIM_MODEL = "meta/llama-3.1-70b-instruct";
const NIM_URL = "https://integrate.api.nvidia.com/v1/chat/completions";
const RATE_LIMIT_PER_MINUTE = 40;
const MAX_HISTORY_MESSAGES = 20;

const PT_SYSTEM_PROMPT = `Sei l'assistente AI per Personal Trainer di Vibe Fitness, un'app di gestione allenamenti.
Aiuti il PT a progettare e adattare schede di allenamento, interpretare i progressi degli allievi,
e dare consigli tecnici su esercizi, volume, intensità e periodizzazione.
Regole importanti:
- Se l'allievo di cui si parla ha infortuni o limitazioni fisiche note, tienine sempre conto e avvisa
  esplicitamente il PT se un esercizio proposto potrebbe essere rischioso.
- Non hai accesso diretto ai dati dell'allievo a meno che non ti vengano forniti nel messaggio: non
  inventare informazioni su allievi specifici.
- Rispondi in italiano, in modo pratico e diretto, da professionista a professionista.
- Non sei un medico: per dubbi clinici seri suggerisci di consultare un professionista sanitario.
- Rispetta la privacy: non chiedere né conservare dati personali superflui.`

const ALLIEVO_SYSTEM_PROMPT = `Sei l'assistente AI personale per l'allievo di Vibe Fitness, un'app di allenamento in palestra.
Aiuti l'utente a capire la propria scheda, la tecnica degli esercizi, la costanza e la motivazione,
e rispondi a domande generali su allenamento e stile di vita attivo.
Regole importanti:
- Sei motivante ma onesto, mai giudicante.
- Non dai diagnosi mediche o piani nutrizionali clinici: per problemi di salute, dolori o infortuni
  suggerisci sempre di sentire il proprio Personal Trainer e, se serve, un medico.
- Rispondi in italiano, in modo semplice e chiaro.
- Rispetta la privacy: non chiedere né conservare dati personali superflui.`

interface ChatRequestBody {
  message: string;
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

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders() });
  if (req.method !== "POST") return jsonResponse({ error: "Method not allowed" }, 405);

  const authHeader = req.headers.get("Authorization");
  if (!authHeader) return jsonResponse({ error: "Missing Authorization header" }, 401);

  const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
  const nimApiKey = Deno.env.get("NVIDIA_NIM_API_KEY");

  if (!nimApiKey) return jsonResponse({ error: "AI assistant not configured" }, 503);

  // Acts as the calling user: every read/write below is RLS-scoped to them.
  const userClient = createClient(supabaseUrl, anonKey, {
    global: { headers: { Authorization: authHeader } },
  });
  const { data: userData, error: userError } = await userClient.auth.getUser();
  if (userError || !userData.user) return jsonResponse({ error: "Invalid session" }, 401);
  const userId = userData.user.id;

  const { data: profile, error: profileError } = await userClient
    .from("profiles")
    .select("role")
    .eq("id", userId)
    .single();
  if (profileError || !profile) return jsonResponse({ error: "Profile not found" }, 404);

  let body: ChatRequestBody;
  try {
    body = await req.json();
  } catch {
    return jsonResponse({ error: "Invalid JSON body" }, 400);
  }
  const message = body.message?.trim();
  if (!message) return jsonResponse({ error: "Empty message" }, 400);

  // Service-role client: only this function touches the rate-limit table (no client RLS policy).
  const serviceClient = createClient(supabaseUrl, serviceRoleKey);

  const oneMinuteAgo = new Date(Date.now() - 60_000).toISOString();
  const { count, error: countError } = await serviceClient
    .from("ai_rate_limit_events")
    .select("id", { count: "exact", head: true })
    .gte("created_at", oneMinuteAgo);
  if (countError) return jsonResponse({ error: "Rate limit check failed" }, 500);
  if ((count ?? 0) >= RATE_LIMIT_PER_MINUTE) {
    return jsonResponse({ error: "L'assistente è occupato, riprova tra qualche secondo." }, 429);
  }
  await serviceClient.from("ai_rate_limit_events").insert({});
  // Best-effort cleanup of old rows so the table doesn't grow unbounded.
  serviceClient.from("ai_rate_limit_events").delete().lt("created_at", oneMinuteAgo).then();

  const { data: history } = await userClient
    .from("ai_messages")
    .select("role, content")
    .eq("user_id", userId)
    .order("created_at", { ascending: false })
    .limit(MAX_HISTORY_MESSAGES);
  const orderedHistory = (history ?? []).reverse();

  const systemPrompt = profile.role === "PT" ? PT_SYSTEM_PROMPT : ALLIEVO_SYSTEM_PROMPT;
  const nimMessages = [
    { role: "system", content: systemPrompt },
    ...orderedHistory.map((m) => ({ role: m.role, content: m.content })),
    { role: "user", content: message },
  ];

  const nimResponse = await fetch(NIM_URL, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${nimApiKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model: NIM_MODEL,
      messages: nimMessages,
      temperature: 0.6,
      max_tokens: 1024,
    }),
  });

  if (!nimResponse.ok) {
    const detail = await nimResponse.text();
    console.error("NIM error", nimResponse.status, detail);
    return jsonResponse({ error: "L'assistente AI non è al momento disponibile." }, 502);
  }

  const nimJson = await nimResponse.json();
  const reply: string | undefined = nimJson.choices?.[0]?.message?.content;
  if (!reply) return jsonResponse({ error: "Risposta AI vuota" }, 502);

  await userClient.from("ai_messages").insert([
    { user_id: userId, role: "user", content: message },
    { user_id: userId, role: "assistant", content: reply },
  ]);

  return jsonResponse({ reply });
});
