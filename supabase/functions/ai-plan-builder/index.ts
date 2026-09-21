// AI-assisted plan builder for Vibe Fitness PTs.
//
// A PT describes a goal in plain Italian ("scheda push/pull/legs, 4 giorni, ipertrofia") and this
// returns a structured draft (name, category, 4-8 exercises picked from the real catalog with
// sets/reps/rest) the app pre-fills into the Plan Editor for the PT to review and tweak before
// saving - never auto-assigned untouched. PT-only, and only for one of their own clients, so the
// injuries context it reads is never exposed to someone who isn't already allowed to see it.
import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const NIM_MODEL = "meta/llama-3.3-70b-instruct";
const NIM_URL = "https://integrate.api.nvidia.com/v1/chat/completions";
const RATE_LIMIT_PER_MINUTE = 40;

interface PlanBuilderRequestBody {
  goal: string;
  clientId: string;
}

interface CatalogItem {
  id: string;
  name: string;
  muscle_group: string;
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

  const userClient = createClient(supabaseUrl, anonKey, {
    global: { headers: { Authorization: authHeader } },
  });
  const { data: userData, error: userError } = await userClient.auth.getUser();
  if (userError || !userData.user) return jsonResponse({ error: "Invalid session" }, 401);
  const ptId = userData.user.id;

  const { data: ptProfile } = await userClient.from("profiles").select("role").eq("id", ptId).single();
  if (ptProfile?.role !== "PT") return jsonResponse({ error: "Solo un PT può generare una scheda con l'AI." }, 403);

  let body: PlanBuilderRequestBody;
  try {
    body = await req.json();
    if (!body.goal || typeof body.goal !== "string" || !body.clientId || typeof body.clientId !== "string") {
      throw new Error("Missing goal/clientId");
    }
  } catch {
    return jsonResponse({ error: "Invalid JSON body" }, 400);
  }
  const goal = body.goal.trim().slice(0, 500);
  if (goal.length === 0) return jsonResponse({ error: "Descrivi l'obiettivo della scheda." }, 400);

  // RLS on profiles only lets a PT read a row that is either their own or one of their clients',
  // so this also doubles as the "is this really my client" check - no separate query needed.
  const { data: clientProfile } = await userClient
    .from("profiles")
    .select("id, injuries, pt_id")
    .eq("id", body.clientId)
    .single();
  if (!clientProfile || clientProfile.pt_id !== ptId) {
    return jsonResponse({ error: "Cliente non trovato o non tuo." }, 404);
  }

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

  const { data: catalog } = await userClient
    .from("exercises")
    .select("id, name, muscle_group")
    .limit(300);
  const catalogItems = (catalog ?? []) as CatalogItem[];
  if (catalogItems.length === 0) return jsonResponse({ error: "Catalogo esercizi vuoto." }, 500);

  const catalogListing = catalogItems.map((e) => `${e.id}|${e.name}|${e.muscle_group}`).join("\n");
  const injuriesNote = clientProfile.injuries
    ? `L'allievo ha riportato: "${clientProfile.injuries}". Evita esercizi che potrebbero aggravarlo.`
    : "Nessun infortunio noto per questo allievo.";

  const systemPrompt =
    `Sei un personal trainer esperto che genera schede di allenamento strutturate. ` +
    `Rispondi SOLO con un oggetto JSON valido, senza testo prima o dopo, nel formato esatto: ` +
    `{"planName":"...","category":"Full Body|Push|Pull|Gambe|Cardio|Mobilità","exercises":[{"exerciseId":"...","sets":N,"reps":N,"restSeconds":N}]}. ` +
    `Usa SOLO id presenti nel catalogo fornito (formato id|nome|gruppo_muscolare, uno per riga). ` +
    `Scegli tra 4 e 8 esercizi coerenti con l'obiettivo, bilanciando i gruppi muscolari coinvolti. ` +
    `sets tra 2 e 5, reps tra 5 e 20, restSeconds tra 45 e 180. ${injuriesNote}\n\nCatalogo:\n${catalogListing}`;

  const nimRes = await fetch(NIM_URL, {
    method: "POST",
    headers: { Authorization: `Bearer ${nimApiKey}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      model: NIM_MODEL,
      messages: [
        { role: "system", content: systemPrompt },
        { role: "user", content: goal },
      ],
      temperature: 0.4,
      max_tokens: 1000,
    }),
  });
  if (!nimRes.ok) {
    console.error("NIM error", nimRes.status, await nimRes.text());
    return jsonResponse({ error: "L'assistente AI non è al momento disponibile." }, 502);
  }
  const nimJson = await nimRes.json();
  const raw = nimJson.choices?.[0]?.message?.content as string | undefined;
  if (!raw) return jsonResponse({ error: "Risposta AI vuota." }, 502);

  // The model is asked for bare JSON but sometimes wraps it in a ```json fence anyway - strip that.
  const cleaned = raw.trim().replace(/^```json\s*/i, "").replace(/^```\s*/, "").replace(/```\s*$/, "");
  let parsed: { planName?: string; category?: string; exercises?: { exerciseId?: string; sets?: number; reps?: number; restSeconds?: number }[] };
  try {
    parsed = JSON.parse(cleaned);
  } catch {
    console.error("Failed to parse NIM JSON:", raw);
    return jsonResponse({ error: "L'AI ha risposto in un formato inatteso, riprova." }, 502);
  }

  const catalogIds = new Set(catalogItems.map((e) => e.id));
  const validExercises = (parsed.exercises ?? []).filter((e) => e.exerciseId && catalogIds.has(e.exerciseId));
  if (validExercises.length === 0) {
    return jsonResponse({ error: "L'AI non ha proposto esercizi validi dal catalogo, riprova con un obiettivo più specifico." }, 502);
  }

  return jsonResponse({
    planName: parsed.planName ?? "Scheda generata dall'AI",
    category: parsed.category ?? null,
    exercises: validExercises.map((e) => ({
      exerciseId: e.exerciseId,
      sets: Math.min(5, Math.max(2, e.sets ?? 3)),
      reps: Math.min(20, Math.max(5, e.reps ?? 10)),
      restSeconds: Math.min(180, Math.max(45, e.restSeconds ?? 90)),
    })),
  });
});
