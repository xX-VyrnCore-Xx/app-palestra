"use client";

import { useCallback, useEffect, useState } from "react";
import { supabase } from "./supabaseClient";

export function daysUntil(dateStr) {
  return Math.ceil((new Date(dateStr) - new Date()) / 86400000);
}

export function membershipState(membership) {
  if (!membership) return "none";
  const days = daysUntil(membership.end_date);
  if (days < 0) return "expired";
  if (days <= 7) return "expiring";
  return "active";
}

/**
 * Shared by the Dashboard (which only needs a summary) and the Allievi roster (which needs the
 * full searchable list) - one query, one place that knows how to join "latest membership per
 * client" instead of two copies of the same fetch drifting apart.
 */
export function useClients(ptId, onError) {
  const [clients, setClients] = useState([]);
  const [memberships, setMemberships] = useState({}); // userId -> latest membership row
  const [loading, setLoading] = useState(true);

  const reload = useCallback(async () => {
    if (!ptId) return;
    setLoading(true);
    const { data: clientRows, error } = await supabase
      .from("profiles")
      .select("id, full_name, email, injuries, created_at")
      .eq("pt_id", ptId)
      .order("full_name");

    if (error) {
      onError?.("Errore nel caricamento degli allievi.");
      setLoading(false);
      return;
    }

    setClients(clientRows || []);

    if (clientRows?.length) {
      const { data: membershipRows } = await supabase
        .from("memberships")
        .select("user_id, end_date, plan_label")
        .in(
          "user_id",
          clientRows.map((c) => c.id)
        )
        .order("end_date", { ascending: false });

      const latest = {};
      for (const m of membershipRows || []) {
        if (!latest[m.user_id]) latest[m.user_id] = m; // first hit per user is the latest (sorted desc)
      }
      setMemberships(latest);
    } else {
      setMemberships({});
    }
    setLoading(false);
  }, [ptId, onError]);

  useEffect(() => {
    reload();
  }, [reload]);

  return { clients, memberships, loading, reload };
}
