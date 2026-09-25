"use client";

import { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import {
  Plus,
  Send,
  CreditCard,
  ShieldAlert,
  StickyNote,
  Dumbbell,
  Trash2,
  Activity,
  Scale,
  Pencil,
  UserMinus,
  Layers,
} from "lucide-react";
import { supabase } from "../../../lib/supabaseClient";
import { useAuthGuard } from "../../../lib/useAuthGuard";
import AppShell from "../../../components/AppShell";
import Avatar from "../../../components/Avatar";
import { SkeletonCard } from "../../../components/Skeleton";
import { useToast } from "../../../components/Toast";

const MEMBERSHIP_STATUS = {
  active: { label: "Attivo", dot: "bg-emerald-400", classes: "bg-emerald-500/15 text-emerald-300" },
  expiring: { label: "In scadenza", dot: "bg-amber-400", classes: "bg-amber-500/15 text-amber-300" },
  expired: { label: "Scaduto", dot: "bg-red-400", classes: "bg-red-500/15 text-red-300" },
  none: { label: "Nessun abbonamento", dot: "bg-white/30", classes: "bg-white/10 text-white/50" },
};

function membershipStatus(m) {
  if (!m) return "none";
  const today = new Date();
  const end = new Date(m.end_date);
  const days = Math.ceil((end - today) / 86400000);
  if (days < 0) return "expired";
  if (days <= 7) return "expiring";
  return "active";
}

export default function ClientDetailPage() {
  const { id } = useParams();
  const router = useRouter();
  const { profile: pt, loading: authLoading } = useAuthGuard();
  const toast = useToast();
  const [confirmRemove, setConfirmRemove] = useState(false);
  const [removing, setRemoving] = useState(false);

  const [client, setClient] = useState(null);
  const [plans, setPlans] = useState([]);
  const [programs, setPrograms] = useState([]);
  const [membership, setMembership] = useState(null);
  const [notes, setNotes] = useState([]);
  const [messages, setMessages] = useState([]);
  const [bodyMetrics, setBodyMetrics] = useState([]);
  const [sessionStats, setSessionStats] = useState({ count: 0, lastAt: null });
  const [loading, setLoading] = useState(true);
  const [injuriesDraft, setInjuriesDraft] = useState("");
  const [savingInjuries, setSavingInjuries] = useState(false);
  const [noteDraft, setNoteDraft] = useState("");
  const [messageDraft, setMessageDraft] = useState("");
  const [showMembershipForm, setShowMembershipForm] = useState(false);
  const [membershipForm, setMembershipForm] = useState({ planLabel: "", startDate: "", endDate: "", notes: "" });

  const loadAll = useCallback(
    async ({ silent } = {}) => {
      if (!pt) return;
      const [
        { data: clientRow },
        { data: planRows },
        { data: programRows },
        { data: membershipRows },
        { data: noteRows },
        { data: messageRows },
        { data: metricRows },
        { count: sessionCount, data: lastSessionRows },
      ] = await Promise.all([
        supabase.from("profiles").select("*").eq("id", id).single(),
        supabase
          .from("workout_plans")
          .select("id, name, category, estimated_minutes, created_at, program_id, week_index")
          .eq("assigned_to_user_id", id)
          .order("created_at", { ascending: false }),
        supabase
          .from("programs")
          .select("*")
          .eq("assigned_to_user_id", id)
          .eq("created_by_pt_id", pt.id)
          .order("start_at", { ascending: false }),
        supabase.from("memberships").select("*").eq("user_id", id).order("end_date", { ascending: false }).limit(1),
        supabase.from("pt_notes").select("*").eq("pt_id", pt.id).eq("client_id", id).order("created_at", { ascending: false }),
        supabase
          .from("messages")
          .select("*")
          .or(`and(sender_id.eq.${pt.id},recipient_id.eq.${id}),and(sender_id.eq.${id},recipient_id.eq.${pt.id})`)
          .order("created_at", { ascending: true })
          .limit(100),
        supabase.from("body_metrics").select("*").eq("user_id", id).order("date", { ascending: false }).limit(5),
        supabase
          .from("workout_sessions")
          .select("started_at", { count: "exact" })
          .eq("user_id", id)
          .order("started_at", { ascending: false })
          .limit(1),
      ]);

      setClient(clientRow || null);
      if (!silent) setInjuriesDraft(clientRow?.injuries || "");
      setPlans(planRows || []);
      setPrograms(programRows || []);
      setMembership((membershipRows && membershipRows[0]) || null);
      setNotes(noteRows || []);
      setMessages(messageRows || []);
      setBodyMetrics(metricRows || []);
      setSessionStats({ count: sessionCount || 0, lastAt: lastSessionRows?.[0]?.started_at || null });
      setLoading(false);

      // Anything the client sent us that we haven't opened yet - opening this page is the read.
      const unreadIds = (messageRows || [])
        .filter((m) => m.recipient_id === pt.id && !m.read_at)
        .map((m) => m.id);
      if (unreadIds.length) {
        supabase
          .from("messages")
          .update({ read_at: new Date().toISOString() })
          .in("id", unreadIds)
          .then(() => {});
      }
    },
    [pt, id]
  );

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  // Realtime instead of polling: new messages and plan/membership edits made from the app show up
  // the moment they happen, matching how the app itself talks to Supabase.
  useEffect(() => {
    if (!pt) return;
    const channel = supabase
      .channel(`client-${id}`)
      .on(
        "postgres_changes",
        { event: "INSERT", schema: "public", table: "messages", filter: `sender_id=eq.${id}` },
        () => loadAll({ silent: true })
      )
      .on(
        "postgres_changes",
        { event: "*", schema: "public", table: "workout_plans", filter: `assigned_to_user_id=eq.${id}` },
        () => loadAll({ silent: true })
      )
      .on(
        "postgres_changes",
        { event: "*", schema: "public", table: "body_metrics", filter: `user_id=eq.${id}` },
        () => loadAll({ silent: true })
      )
      .subscribe();
    return () => supabase.removeChannel(channel);
  }, [pt, id, loadAll]);

  async function saveInjuries() {
    setSavingInjuries(true);
    const { error } = await supabase
      .from("profiles")
      .update({ injuries: injuriesDraft.trim() || null })
      .eq("id", id);
    setSavingInjuries(false);
    if (error) return toast.error("Salvataggio non riuscito.");
    setClient((c) => ({ ...c, injuries: injuriesDraft.trim() || null }));
    toast.success("Note infortuni salvate");
  }

  async function addNote() {
    if (!noteDraft.trim()) return;
    const { data, error } = await supabase
      .from("pt_notes")
      .insert({ pt_id: pt.id, client_id: id, content: noteDraft.trim() })
      .select()
      .single();
    if (error) return toast.error("Impossibile salvare la nota.");
    setNotes((n) => [data, ...n]);
    setNoteDraft("");
  }

  async function deleteNote(noteId) {
    const previous = notes;
    setNotes((n) => n.filter((note) => note.id !== noteId));
    const { error } = await supabase.from("pt_notes").delete().eq("id", noteId);
    if (error) {
      setNotes(previous);
      toast.error("Impossibile eliminare la nota.");
    }
  }

  async function sendMessage() {
    if (!messageDraft.trim()) return;
    const content = messageDraft.trim();
    setMessageDraft("");
    const { data, error } = await supabase
      .from("messages")
      .insert({ sender_id: pt.id, recipient_id: id, content })
      .select()
      .single();
    if (error) return toast.error("Messaggio non inviato.");
    setMessages((m) => [...m, data]);
  }

  async function saveMembership(e) {
    e.preventDefault();
    if (!membershipForm.startDate || !membershipForm.endDate) return;
    const { data, error } = await supabase
      .from("memberships")
      .insert({
        user_id: id,
        plan_label: membershipForm.planLabel.trim() || null,
        start_date: membershipForm.startDate,
        end_date: membershipForm.endDate,
        notes: membershipForm.notes.trim() || null,
      })
      .select()
      .single();
    if (error) return toast.error("Impossibile salvare l'abbonamento.");
    setMembership(data);
    setShowMembershipForm(false);
    setMembershipForm({ planLabel: "", startDate: "", endDate: "", notes: "" });
    toast.success("Abbonamento registrato");
  }

  async function removeClient() {
    setRemoving(true);
    // Unlink, don't delete: the allievo keeps their account and history, they just stop showing
    // up in this PT's roster. They can reconnect any time with an invite code.
    const { error } = await supabase.from("profiles").update({ pt_id: null }).eq("id", id);
    setRemoving(false);
    if (error) {
      toast.error("Impossibile rimuovere l'allievo.");
      return;
    }
    toast.success("Allievo rimosso dal tuo roster");
    router.push("/clients");
  }

  if (authLoading || loading) {
    return (
      <AppShell profile={pt} back={{ href: "/clients", label: "Torna agli allievi" }}>
        <div className="space-y-4">
          <SkeletonCard />
          <SkeletonCard />
        </div>
      </AppShell>
    );
  }

  if (!client) {
    return (
      <AppShell profile={pt} back={{ href: "/clients", label: "Torna agli allievi" }}>
        <p className="text-white/50">Allievo non trovato.</p>
      </AppShell>
    );
  }

  const status = membershipStatus(membership);
  const statusMeta = MEMBERSHIP_STATUS[status];

  return (
    <AppShell profile={pt} back={{ href: "/clients", label: "Torna agli allievi" }}>
      <div className="animate-fade-in">
        <header className="mb-6 flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <Avatar name={client.full_name} size={48} />
            <div>
              <h1 className="text-xl font-bold sm:text-2xl">{client.full_name}</h1>
              <p className="text-sm text-white/50">{client.email}</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <Link href={`/clients/${id}/program/new`} className="btn-secondary">
              <Layers size={16} /> Nuovo programma
            </Link>
            <Link href={`/clients/${id}/plan/new`} className="btn-primary w-auto px-5">
              <Plus size={16} /> Nuova scheda
            </Link>
            <button
              type="button"
              className="flex h-11 w-11 items-center justify-center rounded-2xl border border-white/15 bg-white/5 text-white/50 transition hover:border-red-500/30 hover:bg-red-500/10 hover:text-red-300"
              onClick={() => setConfirmRemove(true)}
              aria-label="Rimuovi allievo"
            >
              <UserMinus size={17} />
            </button>
          </div>
        </header>

        {confirmRemove && (
          <div className="glass-card mb-4 flex flex-wrap items-center justify-between gap-3 border-red-500/30 p-4">
            <p className="text-sm text-white/80">
              Rimuovere {client.full_name} dal tuo roster? L&apos;allievo mantiene account e storico, ma non lo vedrai più qui —
              potrà ricollegarsi con un codice invito.
            </p>
            <div className="flex shrink-0 gap-2">
              <button type="button" className="btn-secondary" onClick={() => setConfirmRemove(false)} disabled={removing}>
                Annulla
              </button>
              <button
                type="button"
                className="rounded-2xl bg-red-500/90 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-red-500 disabled:opacity-50"
                onClick={removeClient}
                disabled={removing}
              >
                {removing ? "Rimozione…" : "Sì, rimuovi"}
              </button>
            </div>
          </div>
        )}

        <div className="grid gap-4 md:grid-cols-2">
          <section className="glass-card p-5">
            <div className="mb-3 flex items-center justify-between">
              <h2 className="flex items-center gap-2 font-semibold">
                <CreditCard size={16} className="text-white/50" /> Abbonamento
              </h2>
              <span className={`flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium ${statusMeta.classes}`}>
                <span className={`h-1.5 w-1.5 rounded-full ${statusMeta.dot}`} /> {statusMeta.label}
              </span>
            </div>
            {membership ? (
              <div className="text-sm text-white/70">
                <p className="font-medium text-white">{membership.plan_label || "Piano generico"}</p>
                <p className="mt-1 text-white/50">
                  {membership.start_date} → {membership.end_date}
                </p>
                {membership.notes && <p className="mt-1 text-white/50">{membership.notes}</p>}
              </div>
            ) : (
              <p className="text-sm text-white/50">Nessun abbonamento registrato.</p>
            )}

            {showMembershipForm ? (
              <form onSubmit={saveMembership} className="mt-4 space-y-2 border-t border-white/10 pt-4">
                <input
                  className="input-field"
                  placeholder="Piano (es. Mensile, Trimestrale)"
                  value={membershipForm.planLabel}
                  onChange={(e) => setMembershipForm((f) => ({ ...f, planLabel: e.target.value }))}
                />
                <div className="flex gap-2">
                  <input
                    type="date"
                    className="input-field"
                    value={membershipForm.startDate}
                    onChange={(e) => setMembershipForm((f) => ({ ...f, startDate: e.target.value }))}
                    required
                  />
                  <input
                    type="date"
                    className="input-field"
                    value={membershipForm.endDate}
                    onChange={(e) => setMembershipForm((f) => ({ ...f, endDate: e.target.value }))}
                    required
                  />
                </div>
                <div className="flex gap-2">
                  <button type="submit" className="btn-primary">
                    Salva
                  </button>
                  <button type="button" className="btn-secondary" onClick={() => setShowMembershipForm(false)}>
                    Annulla
                  </button>
                </div>
              </form>
            ) : (
              <button className="btn-secondary mt-4" onClick={() => setShowMembershipForm(true)}>
                {membership ? "Rinnova" : "Registra abbonamento"}
              </button>
            )}
          </section>

          <section className="glass-card p-5">
            <h2 className="mb-3 flex items-center gap-2 font-semibold">
              <ShieldAlert size={16} className="text-white/50" /> Infortuni / limitazioni
            </h2>
            <textarea
              className="input-field min-h-[80px] resize-none"
              placeholder="Nessuna nota"
              value={injuriesDraft}
              onChange={(e) => setInjuriesDraft(e.target.value)}
            />
            <button
              className="btn-secondary mt-3"
              onClick={saveInjuries}
              disabled={savingInjuries || injuriesDraft === (client.injuries || "")}
            >
              {savingInjuries ? "Salvataggio…" : "Salva"}
            </button>
          </section>
        </div>

        {programs.length > 0 && (
          <section className="glass-card mt-4 p-5">
            <h2 className="mb-3 flex items-center gap-2 font-semibold">
              <Layers size={16} className="text-white/50" /> Programmi
            </h2>
            <div className="space-y-3">
              {programs.map((program) => {
                const weeksBuilt = plans.filter((p) => p.program_id === program.id);
                const builtIndexes = new Set(weeksBuilt.map((p) => p.week_index));
                const nextWeek = Array.from({ length: program.total_weeks }, (_, i) => i + 1).find(
                  (w) => !builtIndexes.has(w)
                );
                return (
                  <div key={program.id} className="rounded-xl border border-white/10 p-4">
                    <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
                      <div>
                        <p className="font-medium">{program.name}</p>
                        <p className="text-xs text-white/50">
                          {weeksBuilt.length}/{program.total_weeks} settimane
                          {program.weekly_increment_percent > 0 ? ` · +${program.weekly_increment_percent}%/sett.` : ""}
                        </p>
                      </div>
                      {nextWeek && (
                        <Link
                          href={`/clients/${id}/plan/new?programId=${program.id}&week=${nextWeek}`}
                          className="btn-secondary px-3 py-1.5 text-xs"
                        >
                          <Plus size={13} /> Settimana {nextWeek}
                        </Link>
                      )}
                    </div>
                    {weeksBuilt.length > 0 && (
                      <div className="flex flex-wrap gap-1.5">
                        {weeksBuilt
                          .sort((a, b) => a.week_index - b.week_index)
                          .map((p) => (
                            <Link
                              key={p.id}
                              href={`/clients/${id}/plan/${p.id}/edit`}
                              className="rounded-lg bg-white/5 px-2.5 py-1 text-xs text-white/70 transition hover:bg-white/10"
                            >
                              Sett. {p.week_index}: {p.name}
                            </Link>
                          ))}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </section>
        )}

        <section className="glass-card mt-4 p-5">
          <h2 className="mb-3 flex items-center gap-2 font-semibold">
            <Dumbbell size={16} className="text-white/50" /> Schede assegnate ({plans.length})
          </h2>
          {plans.length === 0 ? (
            <p className="text-sm text-white/50">Nessuna scheda assegnata ancora.</p>
          ) : (
            <div className="space-y-2">
              {plans.map((p) => (
                <div
                  key={p.id}
                  className="flex items-center justify-between gap-3 rounded-xl border border-white/10 px-4 py-3 transition hover:border-white/20"
                >
                  <div className="min-w-0">
                    <p className="truncate font-medium">{p.name}</p>
                    <p className="text-xs text-white/50">
                      {p.category || "Scheda"} {p.estimated_minutes ? `· ~${p.estimated_minutes} min` : ""}
                    </p>
                  </div>
                  <div className="flex shrink-0 items-center gap-3">
                    <span className="text-xs text-white/30">{new Date(p.created_at).toLocaleDateString("it-IT")}</span>
                    <Link
                      href={`/clients/${id}/plan/${p.id}/edit`}
                      className="flex h-8 w-8 items-center justify-center rounded-lg text-white/40 transition hover:bg-white/5 hover:text-white"
                      aria-label="Modifica scheda"
                    >
                      <Pencil size={14} />
                    </Link>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>

        <section className="glass-card mt-4 p-5">
          <h2 className="mb-3 flex items-center gap-2 font-semibold">
            <Activity size={16} className="text-white/50" /> Andamento
          </h2>
          <div className="mb-4 flex gap-4 text-sm">
            <div>
              <p className="text-lg font-bold">{sessionStats.count}</p>
              <p className="text-xs text-white/50">Allenamenti completati</p>
            </div>
            {sessionStats.lastAt && (
              <div>
                <p className="text-lg font-bold">{new Date(sessionStats.lastAt).toLocaleDateString("it-IT")}</p>
                <p className="text-xs text-white/50">Ultimo allenamento</p>
              </div>
            )}
          </div>
          {bodyMetrics.length === 0 ? (
            <p className="text-sm text-white/50">Nessuna misurazione registrata dall&apos;allievo.</p>
          ) : (
            <div className="space-y-2">
              {bodyMetrics.map((m) => (
                <div key={m.id} className="flex items-center justify-between rounded-xl border border-white/10 px-4 py-2.5 text-sm">
                  <span className="flex items-center gap-2 text-white/70">
                    <Scale size={14} className="text-white/40" />
                    {new Date(m.date).toLocaleDateString("it-IT")}
                  </span>
                  <span className="font-medium">
                    {m.weight_kg ? `${m.weight_kg} kg` : "—"}
                    {m.body_fat_percent ? ` · ${m.body_fat_percent}% BF` : ""}
                  </span>
                </div>
              ))}
            </div>
          )}
        </section>

        <div className="mt-4 grid gap-4 md:grid-cols-2">
          <section className="glass-card p-5">
            <h2 className="mb-3 flex items-center gap-2 font-semibold">
              <StickyNote size={16} className="text-white/50" /> Note private
            </h2>
            <div className="mb-3 flex gap-2">
              <input
                className="input-field"
                placeholder="Aggiungi una nota…"
                value={noteDraft}
                onChange={(e) => setNoteDraft(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && addNote()}
              />
              <button className="btn-secondary px-3" onClick={addNote} aria-label="Aggiungi nota">
                <Plus size={16} />
              </button>
            </div>
            <div className="thin-scroll max-h-52 space-y-2 overflow-y-auto">
              {notes.length === 0 && <p className="text-sm text-white/40">Nessuna nota.</p>}
              {notes.map((n) => (
                <div key={n.id} className="group flex items-start justify-between gap-2 rounded-xl bg-white/5 px-3 py-2 text-sm">
                  <div>
                    <p>{n.content}</p>
                    <p className="mt-1 text-xs text-white/30">{new Date(n.created_at).toLocaleString("it-IT")}</p>
                  </div>
                  <button
                    onClick={() => deleteNote(n.id)}
                    className="shrink-0 text-white/20 opacity-0 transition hover:text-red-400 group-hover:opacity-100"
                    aria-label="Elimina nota"
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              ))}
            </div>
          </section>

          <section className="glass-card flex flex-col p-5">
            <h2 className="mb-3 flex items-center gap-2 font-semibold">
              <Send size={16} className="text-white/50" /> Chat
            </h2>
            <div className="thin-scroll mb-3 flex-1 space-y-2 overflow-y-auto" style={{ maxHeight: "13rem" }}>
              {messages.length === 0 && <p className="text-sm text-white/40">Nessun messaggio.</p>}
              {messages.map((m) => (
                <div
                  key={m.id}
                  className={`max-w-[85%] rounded-2xl px-3 py-2 text-sm ${
                    m.sender_id === pt.id ? "ml-auto bg-brand-orange/80 text-white" : "bg-white/10"
                  }`}
                >
                  {m.content}
                </div>
              ))}
            </div>
            <div className="flex gap-2">
              <input
                className="input-field"
                placeholder="Scrivi un messaggio…"
                value={messageDraft}
                onChange={(e) => setMessageDraft(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && sendMessage()}
              />
              <button className="btn-secondary px-3" onClick={sendMessage} aria-label="Invia messaggio">
                <Send size={16} />
              </button>
            </div>
          </section>
        </div>
      </div>
    </AppShell>
  );
}
