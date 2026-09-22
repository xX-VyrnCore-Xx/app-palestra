"use client";

import { useEffect, useMemo, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { Search, ArrowUp, ArrowDown, X, Check, Plus, Loader2, AlertCircle } from "lucide-react";
import { supabase } from "../../../../../lib/supabaseClient";
import { useAuthGuard } from "../../../../../lib/useAuthGuard";
import AppShell from "../../../../../components/AppShell";
import { useToast } from "../../../../../components/Toast";

const CATEGORIES = ["Full Body", "Push", "Pull", "Gambe", "Cardio", "Mobilità"];

export default function NewPlanPage() {
  const { id } = useParams();
  const router = useRouter();
  const { profile: pt, loading: authLoading } = useAuthGuard();
  const toast = useToast();

  const [clientName, setClientName] = useState("");
  const [exercises, setExercises] = useState([]);
  const [exerciseSearch, setExerciseSearch] = useState("");
  const [name, setName] = useState("");
  const [category, setCategory] = useState(CATEGORIES[0]);
  const [draft, setDraft] = useState([]); // [{exercise, sets, reps, weight, rest}]
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    async function load() {
      const [{ data: clientRow }, { data: exerciseRows }] = await Promise.all([
        supabase.from("profiles").select("full_name").eq("id", id).single(),
        supabase.from("exercises").select("id, name, muscle_group").order("name"),
      ]);
      setClientName(clientRow?.full_name || "");
      setExercises(exerciseRows || []);
    }
    load();
  }, [id]);

  const filteredExercises = useMemo(() => {
    const q = exerciseSearch.trim().toLowerCase();
    if (!q) return exercises.slice(0, 30);
    return exercises.filter((e) => e.name.toLowerCase().includes(q) || e.muscle_group.toLowerCase().includes(q)).slice(0, 30);
  }, [exercises, exerciseSearch]);

  const estimatedMinutes = draft.length > 0 ? Math.round((draft.reduce((sum, d) => sum + d.sets, 0) * 3) / 2) : 0;

  function addExercise(exercise) {
    if (draft.some((d) => d.exercise.id === exercise.id)) return;
    setDraft((d) => [...d, { exercise, sets: 3, reps: 10, weight: "", rest: 90 }]);
  }

  function removeExercise(exerciseId) {
    setDraft((d) => d.filter((row) => row.exercise.id !== exerciseId));
  }

  function updateRow(exerciseId, field, value) {
    setDraft((d) => d.map((row) => (row.exercise.id === exerciseId ? { ...row, [field]: value } : row)));
  }

  function moveRow(index, direction) {
    setDraft((d) => {
      const next = [...d];
      const target = index + direction;
      if (target < 0 || target >= next.length) return d;
      [next[index], next[target]] = [next[target], next[index]];
      return next;
    });
  }

  async function handleSave(e) {
    e.preventDefault();
    setError(null);
    if (!name.trim()) return setError("Dai un nome alla scheda.");
    if (draft.length === 0) return setError("Aggiungi almeno un esercizio.");

    setSaving(true);
    const { data: plan, error: planError } = await supabase
      .from("workout_plans")
      .insert({
        name: name.trim(),
        created_by_pt_id: pt.id,
        assigned_to_user_id: id,
        category,
        estimated_minutes: estimatedMinutes,
      })
      .select()
      .single();

    if (planError || !plan) {
      setSaving(false);
      setError(planError?.message || "Errore nel salvataggio.");
      return;
    }

    const rows = draft.map((row, index) => ({
      plan_id: plan.id,
      exercise_id: row.exercise.id,
      order_index: index,
      target_sets: row.sets,
      target_reps: row.reps,
      target_weight_kg: row.weight === "" ? null : Number(row.weight),
      rest_seconds: row.rest,
    }));

    const { error: exerciseError } = await supabase.from("plan_exercises").insert(rows);
    setSaving(false);

    if (exerciseError) {
      setError(exerciseError.message);
      return;
    }

    toast.success("Scheda assegnata");
    router.push(`/clients/${id}`);
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
      <div className="mx-auto max-w-3xl animate-fade-in">
        <h1 className="mb-6 text-2xl font-bold">Nuova scheda per {clientName}</h1>

        <form onSubmit={handleSave} className="space-y-4">
          <div className="glass-card space-y-4 p-5">
            <div className="flex flex-col gap-3 sm:flex-row">
              <div className="flex-1">
                <label className="label-text">Nome scheda</label>
                <input className="input-field" value={name} onChange={(e) => setName(e.target.value)} placeholder="Es. Push Day" />
              </div>
              <div className="sm:w-48">
                <label className="label-text">Categoria</label>
                <select className="input-field" value={category} onChange={(e) => setCategory(e.target.value)}>
                  {CATEGORIES.map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </select>
              </div>
            </div>
            {draft.length > 0 && (
              <p className="text-sm text-white/50">
                {draft.length} esercizi · ~{estimatedMinutes} min stimati
              </p>
            )}
          </div>

          <div className="glass-card p-5">
            <h2 className="mb-3 font-semibold">Esercizi nella scheda</h2>
            {draft.length === 0 ? (
              <p className="mb-4 text-sm text-white/50">Cerca e aggiungi esercizi dalla lista qui sotto.</p>
            ) : (
              <div className="mb-4 space-y-2">
                {draft.map((row, index) => (
                  <div key={row.exercise.id} className="rounded-xl border border-white/10 p-3">
                    <div className="mb-2 flex items-center justify-between">
                      <p className="font-medium">{row.exercise.name}</p>
                      <div className="flex items-center gap-1">
                        <button
                          type="button"
                          className="rounded-lg p-1 text-white/40 transition hover:bg-white/5 hover:text-white disabled:opacity-30"
                          onClick={() => moveRow(index, -1)}
                          disabled={index === 0}
                          aria-label="Sposta su"
                        >
                          <ArrowUp size={15} />
                        </button>
                        <button
                          type="button"
                          className="rounded-lg p-1 text-white/40 transition hover:bg-white/5 hover:text-white disabled:opacity-30"
                          onClick={() => moveRow(index, 1)}
                          disabled={index === draft.length - 1}
                          aria-label="Sposta giù"
                        >
                          <ArrowDown size={15} />
                        </button>
                        <button
                          type="button"
                          className="ml-1 rounded-lg p-1 text-red-400/70 transition hover:bg-red-500/10 hover:text-red-400"
                          onClick={() => removeExercise(row.exercise.id)}
                          aria-label="Rimuovi esercizio"
                        >
                          <X size={15} />
                        </button>
                      </div>
                    </div>
                    <div className="grid grid-cols-4 gap-2">
                      <NumberField label="Serie" value={row.sets} onChange={(v) => updateRow(row.exercise.id, "sets", v)} />
                      <NumberField label="Rip." value={row.reps} onChange={(v) => updateRow(row.exercise.id, "reps", v)} />
                      <NumberField
                        label="Peso kg"
                        value={row.weight}
                        onChange={(v) => updateRow(row.exercise.id, "weight", v)}
                        allowEmpty
                      />
                      <NumberField label="Rec. sec" value={row.rest} onChange={(v) => updateRow(row.exercise.id, "rest", v)} />
                    </div>
                  </div>
                ))}
              </div>
            )}

            <div className="relative mb-3">
              <Search size={15} className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-white/35" />
              <input
                className="input-field pl-9"
                placeholder="Cerca esercizio…"
                value={exerciseSearch}
                onChange={(e) => setExerciseSearch(e.target.value)}
              />
            </div>
            <div className="thin-scroll max-h-56 space-y-1 overflow-y-auto">
              {filteredExercises.map((ex) => {
                const added = draft.some((d) => d.exercise.id === ex.id);
                return (
                  <button
                    type="button"
                    key={ex.id}
                    onClick={() => addExercise(ex)}
                    disabled={added}
                    className="flex w-full items-center justify-between rounded-lg px-3 py-2 text-left text-sm transition hover:bg-white/5 disabled:opacity-40"
                  >
                    <span>
                      {ex.name} <span className="text-white/40">· {ex.muscle_group}</span>
                    </span>
                    {added ? <Check size={15} className="text-emerald-400" /> : <Plus size={15} className="text-white/40" />}
                  </button>
                );
              })}
              {filteredExercises.length === 0 && (
                <p className="px-3 py-2 text-sm text-white/40">Nessun esercizio trovato.</p>
              )}
            </div>
          </div>

          {error && (
            <p className="flex items-start gap-2 rounded-xl border border-red-500/30 bg-red-500/10 px-3 py-2.5 text-sm text-red-300">
              <AlertCircle size={16} className="mt-0.5 shrink-0" /> {error}
            </p>
          )}

          <button type="submit" className="btn-primary" disabled={saving}>
            {saving && <Loader2 size={16} className="animate-spin" />}
            {saving ? "Salvataggio…" : "Salva e assegna scheda"}
          </button>
        </form>
      </div>
    </AppShell>
  );
}

function NumberField({ label, value, onChange, allowEmpty }) {
  return (
    <div>
      <label className="mb-1 block text-[10px] uppercase text-white/40">{label}</label>
      <input
        type="number"
        className="input-field py-1.5 text-sm"
        value={value}
        onChange={(e) => {
          const v = e.target.value;
          if (v === "" && allowEmpty) return onChange("");
          onChange(v === "" ? 0 : Number(v));
        }}
      />
    </div>
  );
}
