// Sends an FCM push notification to a single user's registered device.
//
// Called by the app right after a chat message (or plan update) is written, so the peer gets
// notified even if the app isn't running - Supabase Realtime only updates data while the
// process is alive. Auth'd as the calling user (so we know who they claim to be), but reads/
// writes only ever touch the service-role client, since a sender has no RLS access to the
// recipient's profile row.
import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

interface PushRequestBody {
  recipientId: string;
  type: "chat_message" | "plan_update";
  title: string;
  body: string;
}

interface ServiceAccount {
  client_email: string;
  private_key: string;
  project_id: string;
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

function base64UrlEncode(bytes: ArrayBuffer | Uint8Array): string {
  const arr = bytes instanceof Uint8Array ? bytes : new Uint8Array(bytes);
  let binary = "";
  for (const byte of arr) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function pemToPkcs8(pem: string): ArrayBuffer {
  const contents = pem
    .replace(/-----BEGIN PRIVATE KEY-----/, "")
    .replace(/-----END PRIVATE KEY-----/, "")
    .replace(/\s+/g, "");
  const binary = atob(contents);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
  return bytes.buffer;
}

/** Exchanges the Firebase service account for a short-lived OAuth2 access token (FCM v1 scope). */
async function getAccessToken(account: ServiceAccount): Promise<string> {
  const key = await crypto.subtle.importKey(
    "pkcs8",
    pemToPkcs8(account.private_key),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );

  const now = Math.floor(Date.now() / 1000);
  const header = base64UrlEncode(new TextEncoder().encode(JSON.stringify({ alg: "RS256", typ: "JWT" })));
  const claim = base64UrlEncode(
    new TextEncoder().encode(
      JSON.stringify({
        iss: account.client_email,
        scope: "https://www.googleapis.com/auth/firebase.messaging",
        aud: "https://oauth2.googleapis.com/token",
        iat: now,
        exp: now + 3600,
      }),
    ),
  );
  const unsigned = `${header}.${claim}`;
  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    key,
    new TextEncoder().encode(unsigned),
  );
  const jwt = `${unsigned}.${base64UrlEncode(signature)}`;

  const tokenResponse = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: jwt,
    }),
  });
  if (!tokenResponse.ok) throw new Error(`Token exchange failed: ${await tokenResponse.text()}`);
  const tokenJson = await tokenResponse.json();
  return tokenJson.access_token as string;
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders() });
  if (req.method !== "POST") return jsonResponse({ error: "Method not allowed" }, 405);

  const authHeader = req.headers.get("Authorization");
  if (!authHeader) return jsonResponse({ error: "Missing Authorization header" }, 401);

  const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
  const serviceAccountJson = Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON");

  // Not configured yet: a no-op, not an error - push is a best-effort enhancement over Realtime.
  if (!serviceAccountJson) return jsonResponse({ skipped: "push not configured" }, 200);

  const userClient = createClient(supabaseUrl, anonKey, {
    global: { headers: { Authorization: authHeader } },
  });
  const { data: userData, error: userError } = await userClient.auth.getUser();
  if (userError || !userData.user) return jsonResponse({ error: "Invalid session" }, 401);

  let body: PushRequestBody;
  try {
    body = await req.json();
  } catch {
    return jsonResponse({ error: "Invalid JSON body" }, 400);
  }
  if (!body.recipientId || !body.title || !body.body) {
    return jsonResponse({ error: "Missing recipientId/title/body" }, 400);
  }

  const serviceClient = createClient(supabaseUrl, serviceRoleKey);
  // Fan out to every device this user is signed into (phone, tablet, ...), not just the last one
  // to register - device_tokens holds one row per user+device instead of profiles' single column.
  const { data: devices } = await serviceClient
    .from("device_tokens")
    .select("fcm_token")
    .eq("user_id", body.recipientId);
  const tokens = (devices ?? []).map((d) => d.fcm_token as string).filter(Boolean);
  if (tokens.length === 0) return jsonResponse({ skipped: "recipient has no registered device" }, 200);

  const account: ServiceAccount = JSON.parse(serviceAccountJson);
  const accessToken = await getAccessToken(account);

  const staleTokens: string[] = [];
  let sentCount = 0;
  for (const token of tokens) {
    const fcmResponse = await fetch(
      `https://fcm.googleapis.com/v1/projects/${account.project_id}/messages:send`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${accessToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          message: {
            token,
            data: {
              type: body.type,
              title: body.title,
              body: body.body,
              senderName: body.title,
              conversationId: userData.user.id,
            },
          },
        }),
      },
    );

    if (fcmResponse.ok) {
      sentCount++;
      continue;
    }
    const detail = await fcmResponse.text();
    console.error("FCM error", fcmResponse.status, detail);
    // UNREGISTERED/INVALID_ARGUMENT means the app was uninstalled or the token rotated - prune it
    // so future sends don't keep paying the round trip for a dead device.
    if (fcmResponse.status === 404 || detail.includes("UNREGISTERED")) {
      staleTokens.push(token);
    }
  }

  if (staleTokens.length > 0) {
    await serviceClient
      .from("device_tokens")
      .delete()
      .eq("user_id", body.recipientId)
      .in("fcm_token", staleTokens);
  }

  if (sentCount === 0) return jsonResponse({ error: "Push delivery failed" }, 502);
  return jsonResponse({ sent: true, devices: sentCount });
});
