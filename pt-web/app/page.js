"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { supabase } from "../lib/supabaseClient";
import { useAuthGuard } from "../lib/useAuthGuard";

const INVITE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"; // no 0/O, 1/I/L - matches the app

function randomInviteCode() {
  return Array.from({ length: 6 }, () => INVITE_ALPHABET[Math.floor(Math.random() * INVITE_ALPHABET.length)]).join("");
}

export default function DashboardPage() {
  const router = useRouter();
  const { profile, loading, setProfile } = useAuthGuard();
  const [clients, setClients] = useState([]);
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
    const { data } = await supabase
      .from("profiles")
      .select("id, full_name, email, injuries")
      .eq("pt_id", ptId)
      .order("full_name");
    setClients(data || []);
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
  }

  async function handleSignOut() {
    await supabase.auth.signOut();
    router.replace("/login");
  }

  function copyInviteCode() {
    if (!profile?.invite_code) return;
    navigator.clipboard.writeText(profile.invite_code).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    });
  }

  const filteredClients = clients.filter((c) => {
    const q = search.trim().toLowerCase();
    if (!q) return true;
    return c.full_name?.toLowerCase().includes(q) || c.email?.toLowerCase().includes(q);
  });

  if (loading) return <CenteredSpinner />;

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <header className="mb-8 flex items-center justify-between">
        <div>
          <p className="text-xs uppercase tracking-wide text-white/40">Gestionale PT</p>
          <h1 className="text-2xl font-bold">Ciao, {profile.full_name?.split(" ")[0] || "PT"}</h1>
        </div>
        <button onClick={handleSignOut} className="btn-secondary">
          Esci
        </button>
      </header>

      <section className="glass-card mb-6 p-5">
        <p className="label-text">Il tuo codice invito</p>
        <div className="flex items-center justify-between gap-3">
          <span className="text-3xl font-bold tracking-widest text-brand-orange">
            {profile.invite_code || "…"}
          </span>
          <button onClick={copyInviteCode} className="btn-secondary" disabled={!profile.invite_code}>
            {copied ? "Copiato!" : "Copia"}
          </button>
        </div>
        <p className="mt-2 text-sm text-white/50">
          Dallo ai tuoi allievi per collegarli al volo, in registrazione o dal loro profilo.
        </p>
      </section>

      <section>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-lg font-semibold">I tuoi allievi ({clients.length})</h2>
          <input
            type="search"
            placeholder="Cerca…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="input-field w-40 py-2"
          />
        </div>

        {clientsLoading ? (
          <CenteredSpinner small />
        ) : filteredClients.length === 0 ? (
          <div className="glass-card p-8 text-center text-white/50">
            {clients.length === 0
              ? "Nessun allievo ancora collegato. Condividi il tuo codice invito per iniziare."
              : "Nessun risultato per questa ricerca."}
          </div>
        ) : (
          <div className="space-y-2">
            {filteredClients.map((client) => (
              <Link
                key={client.id}
                href={`/clients/${client.id}`}
                className="glass-card flex items-center justify-between p-4 transition hover:border-brand-orange/40 hover:bg-white/[0.06]"
              >
                <div>
                  <p className="font-medium">{client.full_name}</p>
                  <p className="text-sm text-white/50">{client.email}</p>
                </div>
                <div className="flex items-center gap-2">
                  {client.injuries && (
                    <span className="rounded-full bg-amber-500/15 px-2.5 py-1 text-xs font-medium text-amber-300">
                      Infortuni
                    </span>
                  )}
                  <span className="text-white/30">›</span>
                </div>
              </Link>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

function CenteredSpinner({ small }) {
  return (
    <div className={small ? "flex justify-center py-8" : "flex min-h-screen items-center justify-center"}>
      <div className="h-8 w-8 animate-spin rounded-full border-2 border-white/20 border-t-brand-orange" />
    </div>
  );
}
