const { isAuthenticated } = require("./_auth");

// Supabase free-tier ceilings this dashboard warns against - see docs/supabase_schema.sql and the
// project's own plan (checked via the Supabase dashboard: org "VyrnCore IT" is on the Free plan).
// Update these if the project is ever upgraded to Pro.
const FREE_TIER_DB_BYTES = 500 * 1024 * 1024;
const FREE_TIER_STORAGE_BYTES = 1024 * 1024 * 1024;
const WARN_THRESHOLD = 0.7; // start flagging at 70% of the ceiling, not at the last minute

module.exports = async function handler(req, res) {
  if (!isAuthenticated(req)) {
    res.status(401).json({ error: "Non autenticato" });
    return;
  }

  const supabaseUrl = process.env.SUPABASE_URL;
  const anonKey = process.env.SUPABASE_ANON_KEY;
  if (!supabaseUrl || !anonKey) {
    res.status(503).json({ error: "Supabase non configurato" });
    return;
  }

  let upstream;
  try {
    upstream = await fetch(`${supabaseUrl}/rest/v1/rpc/get_admin_stats`, {
      method: "POST",
      headers: {
        apikey: anonKey,
        Authorization: `Bearer ${anonKey}`,
        "Content-Type": "application/json",
      },
      body: "{}",
    });
  } catch (err) {
    res.status(502).json({ error: "Impossibile raggiungere Supabase" });
    return;
  }
  if (!upstream.ok) {
    res.status(502).json({ error: `Supabase ha risposto ${upstream.status}` });
    return;
  }
  const stats = await upstream.json();

  const dbUsageRatio = stats.db_size_bytes / FREE_TIER_DB_BYTES;
  const storageUsageRatio = stats.storage_size_bytes / FREE_TIER_STORAGE_BYTES;

  const warnings = [];
  if (dbUsageRatio >= WARN_THRESHOLD) {
    warnings.push({
      level: dbUsageRatio >= 0.9 ? "critical" : "warning",
      message: `Database al ${(dbUsageRatio * 100).toFixed(0)}% del limite free tier (500 MB) - valuta l'upgrade a Pro o una pulizia.`,
    });
  }
  if (storageUsageRatio >= WARN_THRESHOLD) {
    warnings.push({
      level: storageUsageRatio >= 0.9 ? "critical" : "warning",
      message: `Storage file al ${(storageUsageRatio * 100).toFixed(0)}% del limite free tier (1 GB) - controlla allegati chat e avatar.`,
    });
  }
  const cacheAgeMinutes = (Date.now() - new Date(stats.computed_at).getTime()) / 60_000;
  if (cacheAgeMinutes > 30) {
    warnings.push({
      level: "warning",
      message: `Le statistiche non si aggiornano da ${Math.round(cacheAgeMinutes)} minuti - controlla il cron job "refresh-admin-stats" su Supabase.`,
    });
  }

  res.status(200).json({
    ...stats,
    db_usage_ratio: dbUsageRatio,
    storage_usage_ratio: storageUsageRatio,
    free_tier_db_bytes: FREE_TIER_DB_BYTES,
    free_tier_storage_bytes: FREE_TIER_STORAGE_BYTES,
    warnings,
  });
};
