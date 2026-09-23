"use client";

import Link from "next/link";
import { MessageCircle } from "lucide-react";
import { useAuthGuard } from "../../lib/useAuthGuard";
import { useConversations } from "../../lib/useConversations";
import AppShell from "../../components/AppShell";
import Avatar from "../../components/Avatar";
import { SkeletonList } from "../../components/Skeleton";

function timeAgo(dateStr) {
  const diffMs = Date.now() - new Date(dateStr).getTime();
  const mins = Math.floor(diffMs / 60000);
  if (mins < 1) return "ora";
  if (mins < 60) return `${mins}m`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours}h`;
  const days = Math.floor(hours / 24);
  if (days < 7) return `${days}g`;
  return new Date(dateStr).toLocaleDateString("it-IT");
}

export default function MessagesPage() {
  const { profile, loading: authLoading } = useAuthGuard();
  const { conversations, loading } = useConversations(profile?.id);

  if (authLoading) {
    return (
      <AppShell profile={null}>
        <SkeletonList count={5} />
      </AppShell>
    );
  }

  return (
    <AppShell profile={profile}>
      <div className="animate-fade-in">
        <h1 className="text-2xl font-bold sm:text-3xl">Messaggi</h1>
        <p className="mt-1 text-sm text-white/50">Le conversazioni con i tuoi allievi, in un unico posto.</p>

        <div className="mt-6">
          {loading ? (
            <SkeletonList count={5} />
          ) : conversations.length === 0 ? (
            <div className="glass-card p-10 text-center">
              <MessageCircle size={28} className="mx-auto mb-3 text-white/25" />
              <p className="text-white/60">Nessuna conversazione ancora.</p>
              <p className="mt-1 text-sm text-white/40">
                I messaggi scambiati con gli allievi dall&apos;app compariranno qui.
              </p>
            </div>
          ) : (
            <div className="space-y-2">
              {conversations.map(({ client, last, unread }) => (
                <Link
                  key={client.id}
                  href={`/clients/${client.id}`}
                  className="glass-card glass-card-interactive flex items-center gap-3 p-3.5"
                >
                  <Avatar name={client.full_name} size={44} />
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center justify-between gap-2">
                      <p className={`truncate text-sm ${unread > 0 ? "font-semibold" : "font-medium"}`}>
                        {client.full_name}
                      </p>
                      <span className="shrink-0 text-xs text-white/35">{timeAgo(last.created_at)}</span>
                    </div>
                    <p className={`mt-0.5 truncate text-sm ${unread > 0 ? "text-white/80" : "text-white/45"}`}>
                      {last.sender_id === profile.id ? "Tu: " : ""}
                      {last.content}
                    </p>
                  </div>
                  {unread > 0 && (
                    <span className="flex h-5 min-w-5 shrink-0 items-center justify-center rounded-full bg-brand-orange px-1.5 text-[11px] font-bold text-white">
                      {unread > 9 ? "9+" : unread}
                    </span>
                  )}
                </Link>
              ))}
            </div>
          )}
        </div>
      </div>
    </AppShell>
  );
}
