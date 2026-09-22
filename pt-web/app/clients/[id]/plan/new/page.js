"use client";

import { useEffect, useMemo, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { supabase } from "../../../../../lib/supabaseClient";
import { useAuthGuard } from "../../../../../lib/useAuthGuard";

const CATEGORIES = ["Full Body", "Push", "Pull", "Gambe", "Cardio", "Mobilità"];

export default function NewPlanPage() {
  const { id } = useParams();
  const router = useRouter();
  const { profile: pt, loading: authLoading } = useAuthGuard();

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

    router.push(`/clients/${id}`);
  }

  if (authLoading) return <CenteredSpinner />;

  return (
    <div className="mx-auto max-w-3xl px-4 py-8">
      <Link href={`/clients/${id}`} className="mb-4 inline-block text-sm text-white/50 hover:text-white/80">
        ‹ Torna a {clientName || "allievo"}
      </Link>

      <h1 className="mb-6 text-2xl font-bold">Nuova scheda per {clientName}</h1>

      <form onSubmit={handleSave} className="space-y-6">
        <div className="glass-card space-y-4 p-5">
          <div className="flex gap-3">
            <div className="flex-1">
              <label className="label-text">Nome scheda</label>
              <input className="input-field" value={name} onChange={(e) => setName(e.target.value)} placeholder="Es. Push Day" />
            </div>
            <div>
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
              {draft.length} esercizi · ~{estimatedMinutes} min
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
                      <button type="button" className="text-white/40 hover:text-white" onClick={() => moveRow(index, -1)}>
                        ↑
                      </button>
                      <button type="button" className="text-white/40 hover:text-white" onClick={() => moveRow(index, 1)}>
                        ↓
                      </button>
                      <button
                        type="button"
                        className="ml-2 text-red-400/70 hover:text-red-400"
                        onClick={() => removeExercise(row.exercise.id)}
                      >
                        Rimuovi
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

          <input
            className="input-field mb-3"
            placeholder="Cerca esercizio…"
            value={exerciseSearch}
            onChange={(e) => setExerciseSearch(e.target.value)}
          />
          <div className="max-h-56 space-y-1 overflow-y-auto">
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
                  <span className="text-white/40">{added ? "✓" : "+"}</span>
                </button>
              );
            })}
          </div>
        </div>

        {error && (
          <p className="rounded-xl border border-red-500/30 bg-red-500/10 px-3 py-2 text-sm text-red-300">{error}</p>
        )}

        <button type="submit" className="btn-primary" disabled={saving}>
          {saving ? "Salvataggio…" : "Salva e assegna scheda"}
        </button>
      </form>
    </div>
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

function CenteredSpinner() {
  return (
    <div className="flex min-h-screen items-center justify-center">
      <div className="h-8 w-8 animate-spin rounded-full border-2 border-white/20 border-t-brand-orange" />
    </div>
  );
}
