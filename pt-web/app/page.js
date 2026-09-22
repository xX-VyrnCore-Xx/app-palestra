"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Users, Copy, Check, Search, AlertTriangle, ChevronRight, Share2 } from "lucide-react";
import { supabase } from "../lib/supabaseClient";
import { useAuthGuard } from "../lib/useAuthGuard";
import AppShell from "../components/AppShell";
import Avatar from "../components/Avatar";
import StatCard from "../components/StatCard";
import { SkeletonList } from "../components/Skeleton";
import { useToast } from "../components/Toast";

const INVITE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"; // no 0/O, 1/I/L - matches the app

function randomInviteCode() {
  return Array.from({ length: 6 }, () => INVITE_ALPHABET[Math.floor(Math.random() * INVITE_ALPHABET.length)]).join("");
}

function daysUntil(dateStr) {
  return Math.ceil((new Date(dateStr) - new Date()) / 86400000);
}

export default function DashboardPage() {
  const { profile, loading, setProfile } = useAuthGuard();
  const toast = useToast();
  const [clients, setClients] = useState([]);
  const [memberships, setMemberships] = useState({}); // userId -> latest membership row
  const [clientsLoading, setClientsLoading] = useState(true);
  const [copied, setCopied] = useState(false);
  const [search, setSearch] = useState("");

  useEffect(() => {
    if (!profile) return;
    loadClients(profile.id);
    if (!profile.invite_code) ensureInviteCode(profile.id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [profile]);

  async function loadClients(ptId) {
    setClientsLoading(true);
    const { data: clientRows, error } = await supabase
      .from("profiles")
      .select("id, full_name, email, injuries")
      .eq("pt_id", ptId)
      .order("full_name");

    if (error) {
      toast.error("Errore nel caricamento degli allievi.");
      setClientsLoading(false);
      return;
    }

    setClients(clientRows || []);

    if (clientRows?.length) {
      const { data: membershipRows } = await supabase
        .from("memberships")
        .select("user_id, end_date")
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
    }
    setClientsLoading(false);
  }

  async function ensureInviteCode(ptId) {
    for (let i = 0; i < 5; i++) {
      const candidate = randomInviteCode();
      const { error } = await supabase.from("profiles").update({ invite_code: candidate }).eq("id", ptId);
      if (!error) {
        setProfile((p) => ({ ...p, invite_code: candidate }));
        return;
      }
    }
    toast.error("Non sono riuscito a generare un codice invito. Riprova più tardi.");
  }

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

  const filteredClients = clients.filter((c) => {
    const q = search.trim().toLowerCase();
    if (!q) return true;
    return c.full_name?.toLowerCase().includes(q) || c.email?.toLowerCase().includes(q);
  });

  const expiringSoonCount = clients.filter((c) => {
    const m = memberships[c.id];
    if (!m) return false;
    const d = daysUntil(m.end_date);
    return d <= 7;
  }).length;

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
        <h1 className="text-2xl font-bold">Ciao, {profile.full_name?.split(" ")[0] || "PT"} 👋</h1>
        <p className="mt-1 text-sm text-white/50">Ecco un riepilogo dei tuoi allievi.</p>

        <div className="mt-6 grid grid-cols-2 gap-3 sm:grid-cols-3">
          <StatCard icon={Users} label="Allievi totali" value={clients.length} />
          <StatCard
            icon={AlertTriangle}
            label="Abbonamenti in scadenza"
            value={expiringSoonCount}
            accent={expiringSoonCount > 0 ? "text-amber-300" : "text-white"}
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

        <div className="mb-3 mt-8 flex items-center justify-between">
          <h2 className="text-lg font-semibold">I tuoi allievi</h2>
          <div className="relative">
            <Search size={15} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-white/35" />
            <input
              type="search"
              placeholder="Cerca…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="input-field w-44 py-2 pl-9"
            />
          </div>
        </div>

        {clientsLoading ? (
          <SkeletonList />
        ) : filteredClients.length === 0 ? (
          <div className="glass-card p-10 text-center">
            <Users size={28} className="mx-auto mb-3 text-white/25" />
            <p className="text-white/60">
              {clients.length === 0
                ? "Nessun allievo ancora collegato."
                : "Nessun risultato per questa ricerca."}
            </p>
            {clients.length === 0 && (
              <p className="mt-1 text-sm text-white/40">
                Condividi il tuo codice invito qui sopra per far collegare i primi allievi.
              </p>
            )}
          </div>
        ) : (
          <div className="space-y-2">
            {filteredClients.map((client) => {
              const membership = memberships[client.id];
              const expiring = membership && daysUntil(membership.end_date) <= 7;
              const expired = membership && daysUntil(membership.end_date) < 0;
              return (
                <Link
                  key={client.id}
                  href={`/clients/${client.id}`}
                  className="glass-card flex items-center gap-3 p-3.5 transition hover:border-brand-orange/40 hover:bg-white/[0.06]"
                >
                  <Avatar name={client.full_name} size={40} />
                  <div className="min-w-0 flex-1">
                    <p className="truncate font-medium">{client.full_name}</p>
                    <p className="truncate text-sm text-white/50">{client.email}</p>
                  </div>
                  <div className="flex shrink-0 items-center gap-1.5">
                    {client.injuries && (
                      <span className="rounded-full bg-amber-500/15 px-2.5 py-1 text-xs font-medium text-amber-300">
                        Infortuni
                      </span>
                    )}
                    {expired && (
                      <span className="rounded-full bg-red-500/15 px-2.5 py-1 text-xs font-medium text-red-300">
                        Scaduto
                      </span>
                    )}
                    {!expired && expiring && (
                      <span className="rounded-full bg-amber-500/15 px-2.5 py-1 text-xs font-medium text-amber-300">
                        In scadenza
                      </span>
                    )}
                    <ChevronRight size={18} className="text-white/25" />
                  </div>
                </Link>
              );
            })}
          </div>
        )}
      </div>
    </AppShell>
  );
}
