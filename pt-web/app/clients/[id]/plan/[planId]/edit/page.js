"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { Loader2 } from "lucide-react";
import { supabase } from "../../../../../../lib/supabaseClient";
import { useAuthGuard } from "../../../../../../lib/useAuthGuard";
import AppShell from "../../../../../../components/AppShell";
import PlanEditor from "../../../../../../components/PlanEditor";

export default function EditPlanPage() {
  const { id, planId } = useParams();
  const { profile: pt, loading: authLoading } = useAuthGuard();
  const [clientName, setClientName] = useState("");
  const [plan, setPlan] = useState(null);
  const [rows, setRows] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function load() {
      const [{ data: clientRow }, { data: planRow }, { data: exerciseRows }] = await Promise.all([
        supabase.from("profiles").select("full_name").eq("id", id).single(),
        supabase.from("workout_plans").select("*").eq("id", planId).single(),
        supabase
          .from("plan_exercises")
          .select("*, exercise:exercises(id, name, muscle_group)")
          .eq("plan_id", planId)
          .order("order_index"),
      ]);
      setClientName(clientRow?.full_name || "");
      setPlan(planRow || null);
      setRows(
        (exerciseRows || []).map((r) => ({
          exercise: r.exercise,
          sets: r.target_sets,
          reps: r.target_reps,
          weight: r.target_weight_kg ?? "",
          rest: r.rest_seconds,
        }))
      );
      setLoading(false);
    }
    load();
  }, [id, planId]);

  if (authLoading || loading) {
    return (
      <AppShell profile={pt}>
        <div className="flex justify-center py-20">
          <Loader2 className="animate-spin text-white/30" />
        </div>
      </AppShell>
    );
  }

  if (!plan) {
    return (
      <AppShell profile={pt} back={{ href: `/clients/${id}`, label: "Torna all'allievo" }}>
        <p className="text-white/50">Scheda non trovata.</p>
      </AppShell>
    );
  }

  return (
    <AppShell profile={pt} back={{ href: `/clients/${id}`, label: `Torna a ${clientName || "allievo"}` }}>
      <PlanEditor clientId={id} clientName={clientName} pt={pt} existingPlan={plan} existingRows={rows} />
    </AppShell>
  );
}
