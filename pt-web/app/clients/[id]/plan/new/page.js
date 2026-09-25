"use client";

import { Suspense, useEffect, useState } from "react";
import { useParams, useSearchParams } from "next/navigation";
import { Loader2 } from "lucide-react";
import { supabase } from "../../../../../lib/supabaseClient";
import { useAuthGuard } from "../../../../../lib/useAuthGuard";
import AppShell from "../../../../../components/AppShell";
import PlanEditor from "../../../../../components/PlanEditor";

function NewPlanForm() {
  const { id } = useParams();
  const searchParams = useSearchParams();
  const programId = searchParams.get("programId");
  const weekIndex = searchParams.get("week") ? Number(searchParams.get("week")) : null;
  const { profile: pt, loading: authLoading } = useAuthGuard();
  const [clientName, setClientName] = useState("");

  useEffect(() => {
    supabase
      .from("profiles")
      .select("full_name")
      .eq("id", id)
      .single()
      .then(({ data }) => setClientName(data?.full_name || ""));
  }, [id]);

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
      <PlanEditor clientId={id} clientName={clientName} pt={pt} programId={programId} weekIndex={weekIndex} />
    </AppShell>
  );
}

export default function NewPlanPage() {
  return (
    <Suspense fallback={null}>
      <NewPlanForm />
    </Suspense>
  );
}
