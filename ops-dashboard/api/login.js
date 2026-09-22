const { issueSessionCookie } = require("./_auth");
const crypto = require("crypto");

// Constant-time string compare so a wrong-password response doesn't leak, via timing, how many
// leading characters were correct - the same care taken for the cookie signature check.
function safeEqual(a, b) {
  const aBuf = Buffer.from(a);
  const bBuf = Buffer.from(b);
  if (aBuf.length !== bBuf.length) return false;
  return crypto.timingSafeEqual(aBuf, bBuf);
}

module.exports = async function handler(req, res) {
  if (req.method !== "POST") {
    res.status(405).json({ error: "Method not allowed" });
    return;
  }
  const expected = process.env.ADMIN_DASHBOARD_PASSWORD;
  if (!expected) {
    res.status(503).json({ error: "Dashboard non configurata" });
    return;
  }
  let body = req.body;
  if (typeof body === "string") {
    try { body = JSON.parse(body); } catch { body = {}; }
  }
  const password = (body && body.password) || "";
  if (!safeEqual(password, expected)) {
    res.status(401).json({ error: "Password errata" });
    return;
  }
  res.setHeader("Set-Cookie", issueSessionCookie());
  res.status(200).json({ ok: true });
};
