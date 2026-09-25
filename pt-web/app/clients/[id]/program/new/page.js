"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { Loader2, AlertCircle } from "lucide-react";
import { supabase } from "../../../../../lib/supabaseClient";
import { useAuthGuard } from "../../../../../lib/useAuthGuard";
import AppShell from "../../../../../components/AppShell";

export default function NewProgramPage() {
  const { id } = useParams();
  const router = useRouter();
  const { profile: pt, loading: authLoading } = useAuthGuard();
  const [clientName, setClientName] = useState("");
  const [name, setName] = useState("");
  const [totalWeeks, setTotalWeeks] = useState(4);
  const [weeklyIncrement, setWeeklyIncrement] = useState(0);
  const [startAt, setStartAt] = useState(() => new Date().toISOString().slice(0, 10));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    supabase
      .from("profiles")
      .select("full_name")
      .eq("id", id)
      .single()
      .then(({ data }) => setClientName(data?.full_name || ""));
  }, [id]);

  async function handleSave(e) {
    e.preventDefault();
    setError(null);
    if (!name.trim()) return setError("Dai un nome al programma.");
    if (totalWeeks < 1) return setError("Il programma deve durare almeno 1 settimana.");

    setSaving(true);
    const { data: program, error: insertError } = await supabase
      .from("programs")
      .insert({
        name: name.trim(),
        created_by_pt_id: pt.id,
        assigned_to_user_id: id,
        total_weeks: totalWeeks,
        weekly_increment_percent: weeklyIncrement,
        start_at: startAt,
      })
      .select()
      .single();
    setSaving(false);

    if (insertError || !program) {
      setError(insertError?.message || "Errore nel salvataggio.");
      return;
    }

    // A program with no weeks yet is useless - go straight into building week 1.
    router.push(`/clients/${id}/plan/new?programId=${program.id}&week=1`);
  }

  if (authLoading) {
    return (
      <AppShell profile={pt}>
        <div className="flex justify-center py-20">
          <Loader2 className="animate-spin text-white/30" />
        </div>
      </AppShell>
    );
  }

  return (
    <AppShell profile={pt} back={{ href: `/clients/${id}`, label: `Torna a ${clientName || "allievo"}` }}>
      <div className="mx-auto max-w-lg animate-fade-in">
        <h1 className="mb-2 text-2xl font-bold">Nuovo programma per {clientName}</h1>
        <p className="mb-6 text-sm text-white/50">
          Un programma raggruppa più schede in settimane progressive. Dopo averlo creato costruisci subito la scheda della
          settimana 1; le settimane successive si aggiungono dal profilo dell&apos;allievo.
        </p>

        <form onSubmit={handleSave} className="glass-card space-y-4 p-5">
          <div>
            <label className="label-text">Nome programma</label>
            <input className="input-field" value={name} onChange={(e) => setName(e.target.value)} placeholder="Es. Ipertrofia 8 settimane" />
          </div>
          <div className="flex gap-3">
            <div className="flex-1">
              <label className="label-text">Durata (settimane)</label>
              <input
                type="number"
                min={1}
                max={52}
                className="input-field"
                value={totalWeeks}
                onChange={(e) => setTotalWeeks(Number(e.target.value) || 1)}
              />
            </div>
            <div className="flex-1">
              <label className="label-text">Incremento peso/settimana %</label>
              <input
                type="number"
                min={0}
                max={100}
                step="0.5"
                className="input-field"
                value={weeklyIncrement}
                onChange={(e) => setWeeklyIncrement(Number(e.target.value) || 0)}
              />
            </div>
          </div>
          <div>
            <label className="label-text">Data inizio</label>
            <input type="date" className="input-field" value={startAt} onChange={(e) => setStartAt(e.target.value)} required />
          </div>

          {error && (
            <p className="flex items-start gap-2 rounded-xl border border-red-500/30 bg-red-500/10 px-3 py-2.5 text-sm text-red-300">
              <AlertCircle size={16} className="mt-0.5 shrink-0" /> {error}
            </p>
          )}

          <button type="submit" className="btn-primary" disabled={saving}>
            {saving && <Loader2 size={16} className="animate-spin" />}
            {saving ? "Creazione…" : "Crea programma e vai alla settimana 1"}
          </button>
        </form>
      </div>
    </AppShell>
  );
}
