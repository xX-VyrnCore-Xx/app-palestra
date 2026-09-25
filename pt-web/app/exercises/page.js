"use client";

import { useEffect, useMemo, useState } from "react";
import { Dumbbell, Plus, Search, Pencil, Trash2, X, Lock } from "lucide-react";
import { supabase } from "../../lib/supabaseClient";
import { useAuthGuard } from "../../lib/useAuthGuard";
import AppShell from "../../components/AppShell";
import SectionHeader from "../../components/SectionHeader";
import { SkeletonList } from "../../components/Skeleton";
import { useToast } from "../../components/Toast";

const MUSCLE_GROUPS = ["Petto", "Dorso", "Gambe", "Spalle", "Braccia", "Core"];

const EMPTY_FORM = { name: "", muscleGroup: MUSCLE_GROUPS[0], equipment: "", notes: "" };

export default function ExercisesPage() {
  const { profile: pt, loading: authLoading } = useAuthGuard();
  const toast = useToast();

  const [exercises, setExercises] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [groupFilter, setGroupFilter] = useState(null);
  const [editing, setEditing] = useState(null); // null = closed, {} = new, {...exercise} = edit
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);

  async function load() {
    setLoading(true);
    const { data } = await supabase.from("exercises").select("*").order("name");
    setExercises(data || []);
    setLoading(false);
  }

  useEffect(() => {
    load();
  }, []);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    return exercises.filter((ex) => {
      if (groupFilter && ex.muscle_group !== groupFilter) return false;
      if (!q) return true;
      return ex.name.toLowerCase().includes(q) || ex.muscle_group.toLowerCase().includes(q);
    });
  }, [exercises, search, groupFilter]);

  function openNew() {
    setForm(EMPTY_FORM);
    setEditing({});
  }

  function openEdit(ex) {
    setForm({ name: ex.name, muscleGroup: ex.muscle_group, equipment: ex.equipment || "", notes: ex.notes || "" });
    setEditing(ex);
  }

  async function handleSave(e) {
    e.preventDefault();
    if (!form.name.trim()) return;
    setSaving(true);

    const payload = {
      name: form.name.trim(),
      muscle_group: form.muscleGroup,
      equipment: form.equipment.trim() || null,
      notes: form.notes.trim() || null,
    };

    if (editing?.id) {
      const { error } = await supabase.from("exercises").update(payload).eq("id", editing.id);
      setSaving(false);
      if (error) return toast.error("Impossibile salvare le modifiche.");
      toast.success("Esercizio aggiornato");
    } else {
      const { error } = await supabase
        .from("exercises")
        .insert({ ...payload, created_by_user_id: pt.id, is_custom: true });
      setSaving(false);
      if (error) return toast.error("Impossibile creare l'esercizio.");
      toast.success("Esercizio creato");
    }
    setEditing(null);
    load();
  }

  async function handleDelete(ex) {
    const [{ count: planUsage }, { count: sessionUsage }] = await Promise.all([
      supabase.from("plan_exercises").select("id", { count: "exact", head: true }).eq("exercise_id", ex.id),
      supabase.from("set_entries").select("id", { count: "exact", head: true }).eq("exercise_id", ex.id),
    ]);
    if ((planUsage || 0) > 0 || (sessionUsage || 0) > 0) {
      toast.error("Questo esercizio è già usato in una scheda o in un allenamento registrato: non può essere eliminato.");
      return;
    }
    if (!confirm(`Eliminare "${ex.name}"?`)) return;
    const { error } = await supabase.from("exercises").delete().eq("id", ex.id);
    if (error) return toast.error("Impossibile eliminare l'esercizio.");
    toast.success("Esercizio eliminato");
    load();
  }

  if (authLoading) {
    return (
      <AppShell profile={null}>
        <SkeletonList count={5} />
      </AppShell>
    );
  }

  return (
    <AppShell profile={pt}>
      <div className="animate-fade-in">
        <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-2xl font-bold sm:text-3xl">Esercizi</h1>
            <p className="mt-1 text-sm text-white/50">Catalogo condiviso + i tuoi esercizi personalizzati.</p>
          </div>
          <button className="btn-primary w-auto px-5" onClick={openNew}>
            <Plus size={16} /> Nuovo esercizio
          </button>
        </div>

        <div className="mb-4 flex flex-wrap items-center gap-2">
          <div className="relative flex-1 sm:max-w-xs">
            <Search size={15} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-white/35" />
            <input
              className="input-field py-2.5 pl-9"
              placeholder="Cerca esercizio…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          {MUSCLE_GROUPS.map((g) => (
            <button
              key={g}
              onClick={() => setGroupFilter((current) => (current === g ? null : g))}
              className={`rounded-full border px-3 py-1.5 text-xs font-medium transition ${
                groupFilter === g
                  ? "border-brand-orange bg-brand-orange/15 text-brand-orange"
                  : "border-white/10 bg-white/5 text-white/60 hover:bg-white/10"
              }`}
            >
              {g}
            </button>
          ))}
        </div>

        {loading ? (
          <SkeletonList count={6} />
        ) : filtered.length === 0 ? (
          <div className="glass-card p-10 text-center">
            <Dumbbell size={28} className="mx-auto mb-3 text-white/25" />
            <p className="text-white/60">Nessun esercizio trovato.</p>
          </div>
        ) : (
          <div className="space-y-2">
            {filtered.map((ex) => {
              const isMine = ex.is_custom && ex.created_by_user_id === pt.id;
              return (
                <div key={ex.id} className="glass-card glass-card-interactive flex items-center gap-3 p-3.5">
                  <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-white/[0.09] to-white/[0.02] ring-1 ring-white/10">
                    <Dumbbell size={16} className="text-white/60" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <p className="truncate font-medium">{ex.name}</p>
                      {!isMine && <Lock size={12} className="shrink-0 text-white/25" />}
                    </div>
                    <p className="truncate text-xs text-white/50">
                      {ex.muscle_group} {ex.equipment ? `· ${ex.equipment}` : ""}
                    </p>
                  </div>
                  {isMine && (
                    <div className="flex shrink-0 gap-1">
                      <button
                        className="flex h-8 w-8 items-center justify-center rounded-lg text-white/40 transition hover:bg-white/5 hover:text-white"
                        onClick={() => openEdit(ex)}
                        aria-label="Modifica esercizio"
                      >
                        <Pencil size={14} />
                      </button>
                      <button
                        className="flex h-8 w-8 items-center justify-center rounded-lg text-red-400/70 transition hover:bg-red-500/10 hover:text-red-400"
                        onClick={() => handleDelete(ex)}
                        aria-label="Elimina esercizio"
                      >
                        <Trash2 size={14} />
                      </button>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>

      {editing !== null && (
        <div
          className="animate-scrim-in fixed inset-0 z-50 flex items-end justify-center bg-black/60 p-0 backdrop-blur-sm sm:items-center sm:p-4"
          onClick={() => setEditing(null)}
        >
          <div
            className="glass-card animate-modal-in w-full max-w-md rounded-b-none p-5 sm:rounded-b-3xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="mb-4 flex items-center justify-between">
              <h2 className="font-semibold">{editing?.id ? "Modifica esercizio" : "Nuovo esercizio"}</h2>
              <button
                className="flex h-8 w-8 items-center justify-center rounded-lg text-white/40 hover:bg-white/5"
                onClick={() => setEditing(null)}
                aria-label="Chiudi"
              >
                <X size={16} />
              </button>
            </div>
            <form onSubmit={handleSave} className="space-y-3">
              <div>
                <label className="label-text">Nome</label>
                <input
                  className="input-field"
                  value={form.name}
                  onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
                  placeholder="Es. Panca piana"
                  required
                />
              </div>
              <div>
                <label className="label-text">Gruppo muscolare</label>
                <select
                  className="input-field"
                  value={form.muscleGroup}
                  onChange={(e) => setForm((f) => ({ ...f, muscleGroup: e.target.value }))}
                >
                  {MUSCLE_GROUPS.map((g) => (
                    <option key={g} value={g}>
                      {g}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="label-text">Attrezzatura (opzionale)</label>
                <input
                  className="input-field"
                  value={form.equipment}
                  onChange={(e) => setForm((f) => ({ ...f, equipment: e.target.value }))}
                  placeholder="Es. Bilanciere, manubri…"
                />
              </div>
              <div>
                <label className="label-text">Note (opzionale)</label>
                <textarea
                  className="input-field min-h-[60px] resize-none"
                  value={form.notes}
                  onChange={(e) => setForm((f) => ({ ...f, notes: e.target.value }))}
                />
              </div>
              <button type="submit" className="btn-primary" disabled={saving}>
                {saving ? "Salvataggio…" : editing?.id ? "Salva modifiche" : "Crea esercizio"}
              </button>
            </form>
          </div>
        </div>
      )}
    </AppShell>
  );
}
