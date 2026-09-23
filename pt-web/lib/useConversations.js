"use client";

import { useCallback, useEffect, useState } from "react";
import { supabase } from "./supabaseClient";

/**
 * One row per client the PT has ever exchanged a message with, newest conversation first, each
 * carrying its last message and how many of the PT's incoming messages are still unread - the
 * "Messaggi" inbox and the sidebar badge are both built off this so they can't drift apart.
 * Subscribes to realtime inserts/updates on messages addressed to the PT so a new message (or
 * the client detail page marking one read) shows up without a manual refresh.
 */
export function useConversations(ptId) {
  const [conversations, setConversations] = useState([]);
  const [loading, setLoading] = useState(true);

  const reload = useCallback(async () => {
    if (!ptId) return;
    setLoading(true);
    const [{ data: clientRows }, { data: messageRows }] = await Promise.all([
      supabase.from("profiles").select("id, full_name, email").eq("pt_id", ptId),
      supabase
        .from("messages")
        .select("id, sender_id, recipient_id, content, created_at, read_at")
        .or(`sender_id.eq.${ptId},recipient_id.eq.${ptId}`)
        .order("created_at", { ascending: false }),
    ]);

    const byClient = {};
    for (const m of messageRows || []) {
      const clientId = m.sender_id === ptId ? m.recipient_id : m.sender_id;
      if (!byClient[clientId]) byClient[clientId] = { last: m, unread: 0 };
      if (m.recipient_id === ptId && !m.read_at) byClient[clientId].unread += 1;
    }

    const list = (clientRows || [])
      .filter((c) => byClient[c.id])
      .map((c) => ({ client: c, last: byClient[c.id].last, unread: byClient[c.id].unread }))
      .sort((a, b) => new Date(b.last.created_at) - new Date(a.last.created_at));

    setConversations(list);
    setLoading(false);
  }, [ptId]);

  useEffect(() => {
    reload();
  }, [reload]);

  useEffect(() => {
    if (!ptId) return;
    const channel = supabase
      .channel(`inbox-${ptId}`)
      .on(
        "postgres_changes",
        { event: "*", schema: "public", table: "messages", filter: `recipient_id=eq.${ptId}` },
        () => reload()
      )
      .subscribe();
    return () => supabase.removeChannel(channel);
  }, [ptId, reload]);

  const totalUnread = conversations.reduce((sum, c) => sum + c.unread, 0);

  return { conversations, totalUnread, loading, reload };
}
