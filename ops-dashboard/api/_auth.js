// Shared cookie-signing helpers for the two API functions - no npm dependencies, just Node's
// built-in crypto, since a demo ops dashboard doesn't need a real session store: a single shared
// password gates access, and a signed cookie (HMAC, not just a plain flag) is what stops someone
// from forging "authenticated=true" by hand once they see the cookie name.
const crypto = require("crypto");

const COOKIE_NAME = "vibe_ops_session";
const SESSION_TTL_MS = 12 * 60 * 60 * 1000; // 12h - a demo session shouldn't outlive a workday

function sign(payload) {
  const secret = process.env.SESSION_SECRET;
  return crypto.createHmac("sha256", secret).update(payload).digest("hex");
}

function issueSessionCookie() {
  const payload = `ok.${Date.now()}`;
  const signature = sign(payload);
  const value = `${payload}.${signature}`;
  // httpOnly: JS on the page can't read it (nothing to steal via XSS). Secure: HTTPS only, which
  // every Vercel deployment is. SameSite=Strict: never sent cross-site, so no CSRF vector either.
  return `${COOKIE_NAME}=${value}; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=${SESSION_TTL_MS / 1000}`;
}

function clearSessionCookie() {
  return `${COOKIE_NAME}=; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=0`;
}

function isAuthenticated(req) {
  const cookieHeader = req.headers.cookie || "";
  const match = cookieHeader.split(";").map((c) => c.trim()).find((c) => c.startsWith(`${COOKIE_NAME}=`));
  if (!match) return false;
  const value = match.slice(COOKIE_NAME.length + 1);
  const lastDot = value.lastIndexOf(".");
  if (lastDot === -1) return false;
  const payload = value.slice(0, lastDot);
  const signature = value.slice(lastDot + 1);
  const expected = sign(payload);
  // Constant-time compare - a naive `signature === expected` leaks timing info an attacker could
  // use to guess the signature byte by byte. Overkill for a demo, cheap to do right.
  const sigBuf = Buffer.from(signature, "hex");
  const expBuf = Buffer.from(expected, "hex");
  if (sigBuf.length !== expBuf.length || !crypto.timingSafeEqual(sigBuf, expBuf)) return false;
  const issuedAtMatch = payload.match(/^ok\.(\d+)$/);
  if (!issuedAtMatch) return false;
  const issuedAt = Number(issuedAtMatch[1]);
  return Date.now() - issuedAt < SESSION_TTL_MS;
}

module.exports = { issueSessionCookie, clearSessionCookie, isAuthenticated };
