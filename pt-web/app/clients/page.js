"use client";

import { useState } from "react";
import Link from "next/link";
import { Users, Search, ChevronRight } from "lucide-react";
import { useAuthGuard } from "../../lib/useAuthGuard";
import { useClients, daysUntil, membershipState } from "../../lib/useClients";
import AppShell from "../../components/AppShell";
import Avatar from "../../components/Avatar";
import { SkeletonList } from "../../components/Skeleton";
import { useToast } from "../../components/Toast";

export default function ClientsPage() {
  const { profile, loading: authLoading } = useAuthGuard();
  const toast = useToast();
  const [search, setSearch] = useState("");

  const { clients, memberships, loading } = useClients(profile?.id, () =>
    toast.error("Errore nel caricamento degli allievi.")
  );

  const filteredClients = clients.filter((c) => {
    const q = search.trim().toLowerCase();
    if (!q) return true;
    return c.full_name?.toLowerCase().includes(q) || c.email?.toLowerCase().includes(q);
  });

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
        <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-2xl font-bold sm:text-3xl">Allievi</h1>
            <p className="mt-1 text-sm text-white/50">{clients.length} collegati al tuo profilo</p>
          </div>
          <div className="relative">
            <Search size={15} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-white/35" />
            <input
              type="search"
              placeholder="Cerca per nome o email…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="input-field w-64 py-2.5 pl-9"
            />
          </div>
        </div>

        {loading ? (
          <SkeletonList count={5} />
        ) : filteredClients.length === 0 ? (
          <div className="glass-card p-10 text-center">
            <Users size={28} className="mx-auto mb-3 text-white/25" />
            <p className="text-white/60">
              {clients.length === 0 ? "Nessun allievo ancora collegato." : "Nessun risultato per questa ricerca."}
            </p>
            {clients.length === 0 && (
              <p className="mt-1 text-sm text-white/40">
                Condividi il tuo codice invito dalla Dashboard per far collegare i primi allievi.
              </p>
            )}
          </div>
        ) : (
          <div className="space-y-2">
            {filteredClients.map((client) => {
              const membership = memberships[client.id];
              const status = membershipState(membership);
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
                    {status === "expired" && (
                      <span className="rounded-full bg-red-500/15 px-2.5 py-1 text-xs font-medium text-red-300">
                        Scaduto
                      </span>
                    )}
                    {status === "expiring" && (
                      <span className="rounded-full bg-amber-500/15 px-2.5 py-1 text-xs font-medium text-amber-300">
                        In scadenza · {daysUntil(membership.end_date)}gg
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
