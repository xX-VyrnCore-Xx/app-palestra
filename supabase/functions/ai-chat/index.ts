// AI assistant proxy for Vibe Fitness.
//
// Keeps the NVIDIA NIM API key server-side only (never shipped in the Android app, which would
// let it be extracted from the APK). Two distinct assistants - PT and Allievo - with separate
// system prompts, each only ever answering with data scoped to the calling user via RLS.
// Enforces a global sliding-window rate limit (NIM account is capped at 40 requests/minute).
import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

// Reverted from an unverified 405B model slug that broke every request (NIM rejected it
// outright - there is no safe way to confirm a NIM catalog model id from this environment,
// so "bigger model" needs a slug the account owner has actually confirmed in their NVIDIA
// console rather than a guess). This 70B id is the one previously confirmed working.
const NIM_MODEL = "meta/llama-3.3-70b-instruct";
const NIM_URL = "https://integrate.api.nvidia.com/v1/chat/completions";
const RATE_LIMIT_PER_MINUTE = 40;
const MAX_HISTORY_MESSAGES = 20;

// The one tool the assistant can call: re-fetch the caller's own real, current stats mid-
// conversation (e.g. after several turns, when the context injected at the start has scrolled
// out of relevance) instead of relying only on what was preloaded into the system prompt.
const TOOLS = [
  {
    type: "function",
    function: {
      name: "get_my_stats",
      description:
        "Restituisce statistiche reali e aggiornate sull'utente che sta chattando (allievo o PT): " +
        "allenamenti recenti, aderenza, o stato dei clienti. Usalo se hai bisogno di dati più " +
        "aggiornati di quelli già forniti all'inizio della conversazione.",
      parameters: { type: "object", properties: {}, required: [] },
    },
  },
];

const PT_SYSTEM_PROMPT = `Sei l'assistente AI per Personal Trainer di Vibe Fitness, un'app di gestione allenamenti.
Aiuti il PT a progettare e adattare schede di allenamento, interpretare i progressi degli allievi,
dare consigli tecnici su esercizi, volume, intensità e periodizzazione, e rispondere a domande sulle
sedi ViBE e sui corsi di gruppo disponibili (vedi contesto sedi/corsi qui sotto).
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
rispondi a domande generali su allenamento e stile di vita attivo, e a domande sulle sedi ViBE e sui
corsi di gruppo disponibili (vedi contesto sedi/corsi qui sotto).
Regole importanti:
- Sei motivante ma onesto, mai giudicante.
- Non dai diagnosi mediche o piani nutrizionali clinici: per problemi di salute, dolori o infortuni
  suggerisci sempre di sentire il proprio Personal Trainer e, se serve, un medico.
- Rispondi in italiano, in modo semplice e chiaro.
- Rispetta la privacy: non chiedere né conservare dati personali superflui.`

interface ChatRequestBody {
  message: string;
}

// Real ViBE Fitness club roster and group-class catalog (kept in sync with the Kotlin copy in
// data/locations/VibeCatalog.kt - both are static content sourced from vibefitness.it, so this
// string doubles as the source the assistant reads from when asked "che corsi ci sono" or "dov'è
// la palestra più vicina a Monza", without a network fetch or a DB round trip on every message.
const LOCATIONS_CONTEXT = `Sedi e corsi ViBE Fitness (dati statici, sempre veri, usali se pertinenti):
17 club in Lombardia e Piemonte:
- Torino: Alpignano
- Milano: Sesto San Giovanni, Paderno Dugnano Calderara, Milano Certosa, Paderno Dugnano Comasina
- Monza e Brianza: Varedo, Seregno, Lentate sul Seveso, Desio, Busnago, Besana in Brianza, Barlassina, Arcore
- Como: Erba, Como, Arosio, Albese con Cassano
Quasi tutti i club sono aperti 24/7 (eccetto Milano Certosa, Sesto San Giovanni e Busnago, con orari più ampi ma non h24).
Iscrizione annuale include corsi di gruppo illimitati. Corsi disponibili: Stretching & Meditazione, Postural Yoga,
Yoga Dolce, Yoga del Risveglio, Flexibility (bassa intensità); Salsa, Afrostep Coreografico, Heels, Zumba Fitness,
Yoga Dinamico, Salsa Base, Hybrid Workout, Fitness Dance, Fitball Training (media intensità); Body Pump, Abs,
Pole Dance Base, HIIT Functional, Difesa Personale, Boxe (alta intensità).
Nell'app, la sezione "Sedi & Corsi" (icona nelle azioni rapide della Home) mostra l'elenco completo con indirizzo
di massima e un pulsante per aprire la sede in Google Maps: rimanda l'utente lì per i dettagli e le indicazioni.`;

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
  const [{ data: sessions }, { data: plans }, { data: metrics }, { data: profile }] = await Promise.all([
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
    userClient.from("profiles").select("injuries").eq("id", userId).single(),
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
    profile?.injuries ? `- Infortuni/limitazioni riportate: ${profile.injuries} (tienine sempre conto nei consigli)` : null,
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
    .select("id, full_name, injuries")
    .eq("pt_id", ptId);
  if (!clients || clients.length === 0) {
    return "Contesto PT: nessun cliente ancora.";
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
  for (const client of clients as { id: string; full_name: string; injuries: string | null }[]) {
    const lastActive = lastActiveByClient.get(client.id);
    if (lastActive && lastActive > weekAgo) {
      activeThisWeek++;
    } else {
      inactive.push(client.full_name);
    }
  }

  const withInjuries = (clients as { full_name: string; injuries: string | null }[])
    .filter((c) => c.injuries)
    .map((c) => `${c.full_name} (${c.injuries})`);

  const lines = [
    "Contesto PT (dati reali e aggiornati sui tuoi clienti, usali per rispondere senza dover chiedere di nuovo):",
    `- Clienti totali: ${clients.length}`,
    `- Attivi negli ultimi 7 giorni: ${activeThisWeek}`,
    inactive.length > 0
      ? `- Fermi da più di 7 giorni (o mai attivi): ${inactive.join(", ")}`
      : "- Tutti i clienti si sono allenati negli ultimi 7 giorni",
    withInjuries.length > 0
      ? `- Infortuni/limitazioni note: ${withInjuries.join("; ")} - tienine sempre conto se si parla di loro`
      : null,
  ].filter((line): line is string => line !== null);
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
    if (!body.message || typeof body.message !== "string") {
      throw new Error("Message field is missing or invalid");
    }
  } catch (err) {
    console.error("Validation error:", err.message);
    return jsonResponse({ error: "Invalid JSON body or missing message" }, 400);
  }
  const message = body.message.trim();
  if (message.length === 0) return jsonResponse({ error: "Empty message" }, 400);
  if (message.length > 2000) return jsonResponse({ error: "Messaggio troppo lungo (max 2000 caratteri)" }, 400);

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

  // deno-lint-ignore no-explicit-any
  const nimMessages: any[] = [
    { role: "system", content: systemPrompt },
    { role: "system", content: LOCATIONS_CONTEXT },
    ...(contextBlock ? [{ role: "system", content: contextBlock }] : []),
    ...orderedHistory.map((m) => ({ role: m.role, content: m.content })),
    { role: "user", content: message },
  ];

  // deno-lint-ignore no-explicit-any
  async function callNim(withTools: boolean): Promise<any> {
    const res = await fetch(NIM_URL, {
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
        ...(withTools ? { tools: TOOLS, tool_choice: "auto" } : {}),
      }),
    });
    if (!res.ok) {
      const detail = await res.text();
      console.error("NIM error", res.status, detail);
      throw new Error("NIM request failed");
    }
    return await res.json();
  }

  // Some NIM-hosted models reject the "tools" param outright rather than just ignoring it, so a
  // failure here retries once without tools instead of failing the whole request - the assistant
  // degrades to "no live re-fetch of stats mid-chat" rather than refusing to answer at all.
  let nimJson;
  try {
    nimJson = await callNim(true);
  } catch {
    try {
      nimJson = await callNim(false);
    } catch {
      return jsonResponse({ error: "L'assistente AI non è al momento disponibile." }, 502);
    }
  }

  let choice = nimJson.choices?.[0];
  const toolCalls = choice?.message?.tool_calls as
    | { id: string; function: { name: string } }[]
    | undefined;

  if (toolCalls && toolCalls.length > 0) {
    nimMessages.push(choice.message);
    for (const toolCall of toolCalls) {
      const result = toolCall.function.name === "get_my_stats"
        ? (profile.role === "PT"
          ? await buildPtContext(userClient, userId)
          : await buildAllievoContext(userClient, userId)) ?? "Nessun dato disponibile."
        : "Strumento non disponibile.";
      nimMessages.push({ role: "tool", tool_call_id: toolCall.id, content: result });
    }
    try {
      nimJson = await callNim(false);
      choice = nimJson.choices?.[0];
    } catch {
      return jsonResponse({ error: "L'assistente AI non è al momento disponibile." }, 502);
    }
  }

  const reply: string | undefined = choice?.message?.content;
  if (!reply) return jsonResponse({ error: "Risposta AI vuota" }, 502);

  await userClient.from("ai_messages").insert([
    { user_id: userId, role: "user", content: message },
    { user_id: userId, role: "assistant", content: reply },
  ]);

  // Drip-feeds the already-complete reply word by word over SSE, so the app can show it typing
  // out live instead of popping in all at once - the perceived-speed win the streaming ask was
  // after, without the added fragility of piping NIM's raw token stream through a tool-calling
  // round trip.
  const encoder = new TextEncoder();
  const words = reply.split(/(\s+)/).filter((w) => w.length > 0);
  const stream = new ReadableStream({
    async start(controller) {
      for (const word of words) {
        controller.enqueue(encoder.encode(`data: ${JSON.stringify({ content: word })}\n\n`));
        await new Promise((resolve) => setTimeout(resolve, 12));
      }
      controller.enqueue(encoder.encode("data: [DONE]\n\n"));
      controller.close();
    },
  });

  return new Response(stream, {
    headers: { ...corsHeaders(), "Content-Type": "text/event-stream", "Cache-Control": "no-cache" },
  });
});
