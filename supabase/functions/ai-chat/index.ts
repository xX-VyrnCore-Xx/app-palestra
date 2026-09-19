// AI assistant proxy for Vibe Fitness.
//
// Keeps the NVIDIA NIM API key server-side only (never shipped in the Android app, which would
// let it be extracted from the APK). Two distinct assistants - PT and Allievo - with separate
// system prompts, each only ever answering with data scoped to the calling user via RLS.
// Enforces a global sliding-window rate limit (NIM account is capped at 40 requests/minute).
import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const NIM_MODEL = "meta/llama-3.3-70b-instruct";
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

/** Real, current data about the signed-in allievo, injected as extra system context so the
 * assistant's advice is grounded in their actual training instead of generic. Every query below
 * runs through userClient (RLS-scoped to this user), so it can never leak another user's rows. */
// deno-lint-ignore no-explicit-any
async function buildAllievoContext(userClient: any, userId: string): Promise<string | null> {
  const [{ data: sessions }, { data: plans }, { data: metrics }] = await Promise.all([
    userClient
      .from("workout_sessions")
      .select("started_at, ended_at")
      .eq("user_id", userId)
      .order("started_at", { ascending: false })
      .limit(30),
    userClient
      .from("workout_plans")
      .select("name, created_at")
      .eq("assigned_to_user_id", userId)
      .order("created_at", { ascending: false })
      .limit(1),
    userClient
      .from("body_metrics")
      .select("date, weight_kg")
      .eq("user_id", userId)
      .order("date", { ascending: false })
      .limit(1),
  ]);

  const completed = (sessions ?? []).filter((s: { ended_at: string | null }) => s.ended_at);
  const weekAgo = Date.now() - 7 * 24 * 3600_000;
  const workoutsThisWeek = completed.filter((s: { ended_at: string }) => new Date(s.ended_at).getTime() > weekAgo).length;
  const lastSession = completed[0];
  const daysSinceLastSession = lastSession
    ? Math.floor((Date.now() - new Date(lastSession.ended_at).getTime()) / 86_400_000)
    : null;
  const currentPlan = plans?.[0]?.name ?? null;
  const latestWeight = metrics?.[0]?.weight_kg ?? null;

  const lines = [
    "Contesto allievo (dati reali e aggiornati, usali per personalizzare la risposta senza doverli chiedere di nuovo):",
    currentPlan ? `- Scheda attuale: ${currentPlan}` : "- Nessuna scheda assegnata al momento",
    `- Allenamenti completati negli ultimi 7 giorni: ${workoutsThisWeek}`,
    daysSinceLastSession === null
      ? "- Non ha ancora completato nessun allenamento"
      : daysSinceLastSession === 0
        ? "- Ultimo allenamento: oggi"
        : `- Ultimo allenamento: ${daysSinceLastSession} giorni fa`,
    latestWeight ? `- Ultimo peso corporeo registrato: ${latestWeight} kg` : null,
  ].filter((line): line is string => line !== null);

  return lines.join("\n");
}

/** Real, current roster data for the signed-in PT - who's active, who's gone quiet - so the
 * assistant can answer questions like "chi non si allena da una settimana?" without the PT having
 * to look it up themself. Scoped to the PT's own clients via RLS on both queries. */
// deno-lint-ignore no-explicit-any
async function buildPtContext(userClient: any, ptId: string): Promise<string | null> {
  const { data: clients } = await userClient
    .from("profiles")
    .select("id, full_name")
    .eq("pt_id", ptId);
  if (!clients || clients.length === 0) {
    return "Contesto PT: nessuna recluta arruolata ancora.";
  }

  const clientIds = clients.map((c: { id: string }) => c.id);
  const { data: sessions } = await userClient
    .from("workout_sessions")
    .select("user_id, ended_at")
    .in("user_id", clientIds)
    .not("ended_at", "is", null);

  const lastActiveByClient = new Map<string, number>();
  for (const s of sessions ?? []) {
    const ts = new Date(s.ended_at as string).getTime();
    const prev = lastActiveByClient.get(s.user_id as string) ?? 0;
    if (ts > prev) lastActiveByClient.set(s.user_id as string, ts);
  }

  const weekAgo = Date.now() - 7 * 24 * 3600_000;
  const inactive: string[] = [];
  let activeThisWeek = 0;
  for (const client of clients as { id: string; full_name: string }[]) {
    const lastActive = lastActiveByClient.get(client.id);
    if (lastActive && lastActive > weekAgo) {
      activeThisWeek++;
    } else {
      inactive.push(client.full_name);
    }
  }

  const lines = [
    "Contesto PT (dati reali e aggiornati sul tuo plotone, usali per rispondere senza dover chiedere di nuovo):",
    `- Reclute totali: ${clients.length}`,
    `- Attive negli ultimi 7 giorni: ${activeThisWeek}`,
    inactive.length > 0
      ? `- Ferme da più di 7 giorni (o mai attive): ${inactive.join(", ")}`
      : "- Tutte le reclute si sono allenate negli ultimi 7 giorni",
  ];
  return lines.join("\n");
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
  const contextBlock = profile.role === "PT"
    ? await buildPtContext(userClient, userId)
    : await buildAllievoContext(userClient, userId);

  const nimMessages = [
    { role: "system", content: systemPrompt },
    ...(contextBlock ? [{ role: "system", content: contextBlock }] : []),
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
