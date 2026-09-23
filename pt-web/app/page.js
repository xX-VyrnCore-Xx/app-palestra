"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Users, Copy, Check, AlertTriangle, ChevronRight, Share2, UserPlus, ArrowRight } from "lucide-react";
import { supabase } from "../lib/supabaseClient";
import { useAuthGuard } from "../lib/useAuthGuard";
import { useClients, daysUntil, membershipState } from "../lib/useClients";
import AppShell from "../components/AppShell";
import Avatar from "../components/Avatar";
import StatCard from "../components/StatCard";
import { SkeletonList } from "../components/Skeleton";
import { useToast } from "../components/Toast";

const INVITE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"; // no 0/O, 1/I/L - matches the app

function randomInviteCode() {
  return Array.from({ length: 6 }, () => INVITE_ALPHABET[Math.floor(Math.random() * INVITE_ALPHABET.length)]).join("");
}

export default function DashboardPage() {
  const { profile, loading, setProfile } = useAuthGuard();
  const toast = useToast();
  const [copied, setCopied] = useState(false);

  const { clients, memberships, loading: clientsLoading } = useClients(profile?.id, () =>
    toast.error("Errore nel caricamento degli allievi.")
  );

  async function ensureInviteCode() {
    if (!profile || profile.invite_code) return;
    for (let i = 0; i < 5; i++) {
      const candidate = randomInviteCode();
      const { error } = await supabase.from("profiles").update({ invite_code: candidate }).eq("id", profile.id);
      if (!error) {
        setProfile((p) => ({ ...p, invite_code: candidate }));
        return;
      }
    }
    toast.error("Non sono riuscito a generare un codice invito. Riprova più tardi.");
  }

  useEffect(() => {
    if (profile && !profile.invite_code) ensureInviteCode();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [profile?.id, profile?.invite_code]);

  function copyInviteCode() {
    if (!profile?.invite_code) return;
    navigator.clipboard.writeText(profile.invite_code).then(() => {
      setCopied(true);
      toast.success("Codice copiato negli appunti");
      setTimeout(() => setCopied(false), 1500);
    });
  }

  function shareInviteCode() {
    if (!profile?.invite_code) return;
    const text = `Collegati a me su Vibe Fitness con il codice: ${profile.invite_code}`;
    if (navigator.share) {
      navigator.share({ text }).catch(() => {});
    } else {
      navigator.clipboard.writeText(text);
      toast.success("Messaggio di invito copiato");
    }
  }

  const withStatus = clients
    .map((c) => ({ client: c, membership: memberships[c.id], status: membershipState(memberships[c.id]) }))
    .filter((c) => c.status === "expiring" || c.status === "expired")
    .sort((a, b) => daysUntil(a.membership.end_date) - daysUntil(b.membership.end_date))
    .slice(0, 5);

  const recentClients = [...clients]
    .sort((a, b) => new Date(b.created_at || 0) - new Date(a.created_at || 0))
    .slice(0, 5);

  if (loading) {
    return (
      <AppShell profile={null}>
        <SkeletonList count={4} />
      </AppShell>
    );
  }

  return (
    <AppShell profile={profile}>
      <div className="animate-fade-in">
        <h1 className="text-2xl font-bold sm:text-3xl">Ciao, {profile.full_name?.split(" ")[0] || "PT"} 👋</h1>
        <p className="mt-1 text-sm text-white/50">Ecco come vanno le cose oggi.</p>

        <div className="mt-6 grid grid-cols-2 gap-3 sm:grid-cols-3">
          <StatCard icon={Users} label="Allievi totali" value={clients.length} />
          <StatCard
            icon={AlertTriangle}
            label="Abbonamenti da seguire"
            value={withStatus.length}
            accent={withStatus.length > 0 ? "text-amber-300" : "text-white"}
          />
          <div className="glass-card col-span-2 flex items-center justify-between p-4 sm:col-span-1">
            <div className="min-w-0">
              <p className="label-text mb-0.5">Codice invito</p>
              <p className="text-xl font-bold tracking-widest text-brand-orange">{profile.invite_code || "…"}</p>
            </div>
            <div className="flex shrink-0 gap-1.5">
              <button
                onClick={copyInviteCode}
                className="flex h-9 w-9 items-center justify-center rounded-xl border border-white/10 bg-white/5 transition hover:bg-white/10"
                disabled={!profile.invite_code}
                aria-label="Copia codice"
              >
                {copied ? <Check size={15} className="text-emerald-400" /> : <Copy size={15} />}
              </button>
              <button
                onClick={shareInviteCode}
                className="flex h-9 w-9 items-center justify-center rounded-xl border border-white/10 bg-white/5 transition hover:bg-white/10"
                disabled={!profile.invite_code}
                aria-label="Condividi codice"
              >
                <Share2 size={15} />
              </button>
            </div>
          </div>
        </div>

        <div className="mt-8 grid gap-6 lg:grid-cols-2">
          <section>
            <div className="mb-3 flex items-center justify-between">
              <h2 className="flex items-center gap-2 text-lg font-semibold">
                <AlertTriangle size={17} className="text-amber-300" /> Da seguire
              </h2>
            </div>
            {clientsLoading ? (
              <SkeletonList count={2} />
            ) : withStatus.length === 0 ? (
              <div className="glass-card p-6 text-center text-sm text-white/50">
                Nessun abbonamento in scadenza. Tutto in ordine.
              </div>
            ) : (
              <div className="space-y-2">
                {withStatus.map(({ client, membership, status }) => (
                  <Link
                    key={client.id}
                    href={`/clients/${client.id}`}
                    className="glass-card flex items-center gap-3 p-3.5 transition hover:border-brand-orange/40 hover:bg-white/[0.06]"
                  >
                    <Avatar name={client.full_name} size={36} />
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium">{client.full_name}</p>
                      <p className="truncate text-xs text-white/50">
                        {status === "expired"
                          ? `Scaduto il ${membership.end_date}`
                          : `Scade tra ${daysUntil(membership.end_date)} giorni`}
                      </p>
                    </div>
                    <span
                      className={`rounded-full px-2.5 py-1 text-xs font-medium ${
                        status === "expired" ? "bg-red-500/15 text-red-300" : "bg-amber-500/15 text-amber-300"
                      }`}
                    >
                      {status === "expired" ? "Scaduto" : "In scadenza"}
                    </span>
                  </Link>
                ))}
              </div>
            )}
          </section>

          <section>
            <div className="mb-3 flex items-center justify-between">
              <h2 className="flex items-center gap-2 text-lg font-semibold">
                <UserPlus size={17} className="text-white/50" /> Ultimi collegati
              </h2>
            </div>
            {clientsLoading ? (
              <SkeletonList count={2} />
            ) : recentClients.length === 0 ? (
              <div className="glass-card p-6 text-center text-sm text-white/50">
                Nessun allievo ancora collegato. Condividi il codice invito qui sopra.
              </div>
            ) : (
              <div className="space-y-2">
                {recentClients.map((client) => (
                  <Link
                    key={client.id}
                    href={`/clients/${client.id}`}
                    className="glass-card flex items-center gap-3 p-3.5 transition hover:border-brand-orange/40 hover:bg-white/[0.06]"
                  >
                    <Avatar name={client.full_name} size={36} />
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium">{client.full_name}</p>
                      <p className="truncate text-xs text-white/50">{client.email}</p>
                    </div>
                    <ChevronRight size={16} className="text-white/25" />
                  </Link>
                ))}
              </div>
            )}
          </section>
        </div>

        <Link
          href="/clients"
          className="glass-card mt-6 flex items-center justify-between p-4 transition hover:border-brand-orange/40 hover:bg-white/[0.06]"
        >
          <span className="flex items-center gap-2 text-sm font-medium">
            <Users size={16} className="text-white/50" /> Vedi tutti gli allievi ({clients.length})
          </span>
          <ArrowRight size={16} className="text-white/40" />
        </Link>
      </div>
    </AppShell>
  );
}
