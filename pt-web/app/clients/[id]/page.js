"use client";

import { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import {
  Plus,
  Send,
  CreditCard,
  ShieldAlert,
  StickyNote,
  Dumbbell,
  Trash2,
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
  const { profile: pt, loading: authLoading } = useAuthGuard();
  const toast = useToast();

  const [client, setClient] = useState(null);
  const [plans, setPlans] = useState([]);
  const [membership, setMembership] = useState(null);
  const [notes, setNotes] = useState([]);
  const [messages, setMessages] = useState([]);
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
      const [{ data: clientRow }, { data: planRows }, { data: membershipRows }, { data: noteRows }, { data: messageRows }] =
        await Promise.all([
          supabase.from("profiles").select("*").eq("id", id).single(),
          supabase
            .from("workout_plans")
            .select("id, name, category, estimated_minutes, created_at, program_id")
            .eq("assigned_to_user_id", id)
            .order("created_at", { ascending: false }),
          supabase.from("memberships").select("*").eq("user_id", id).order("end_date", { ascending: false }).limit(1),
          supabase.from("pt_notes").select("*").eq("pt_id", pt.id).eq("client_id", id).order("created_at", { ascending: false }),
          supabase
            .from("messages")
            .select("*")
            .or(`and(sender_id.eq.${pt.id},recipient_id.eq.${id}),and(sender_id.eq.${id},recipient_id.eq.${pt.id})`)
            .order("created_at", { ascending: true })
            .limit(100),
        ]);

      setClient(clientRow || null);
      if (!silent) setInjuriesDraft(clientRow?.injuries || "");
      setPlans(planRows || []);
      setMembership((membershipRows && membershipRows[0]) || null);
      setNotes(noteRows || []);
      setMessages(messageRows || []);
      setLoading(false);
    },
    [pt, id]
  );

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  // Lightweight polling instead of a realtime subscription - keeps this page simple and still
  // gets new messages/plan changes within a few seconds. `silent` avoids clobbering an
  // in-progress injuries edit with the just-fetched value on every tick.
  useEffect(() => {
    const interval = setInterval(() => loadAll({ silent: true }), 6000);
    return () => clearInterval(interval);
  }, [loadAll]);

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

  if (authLoading || loading) {
    return (
      <AppShell profile={pt} back={{ href: "/", label: "Torna agli allievi" }}>
        <div className="space-y-4">
          <SkeletonCard />
          <SkeletonCard />
        </div>
      </AppShell>
    );
  }

  if (!client) {
    return (
      <AppShell profile={pt} back={{ href: "/", label: "Torna agli allievi" }}>
        <p className="text-white/50">Allievo non trovato.</p>
      </AppShell>
    );
  }

  const status = membershipStatus(membership);
  const statusMeta = MEMBERSHIP_STATUS[status];

  return (
    <AppShell profile={pt} back={{ href: "/", label: "Torna agli allievi" }}>
      <div className="animate-fade-in">
        <header className="mb-6 flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <Avatar name={client.full_name} size={48} />
            <div>
              <h1 className="text-xl font-bold sm:text-2xl">{client.full_name}</h1>
              <p className="text-sm text-white/50">{client.email}</p>
            </div>
          </div>
          <Link href={`/clients/${id}/plan/new`} className="btn-primary w-auto px-5">
            <Plus size={16} /> Nuova scheda
          </Link>
        </header>

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

        <section className="glass-card mt-4 p-5">
          <h2 className="mb-3 flex items-center gap-2 font-semibold">
            <Dumbbell size={16} className="text-white/50" /> Schede assegnate ({plans.length})
          </h2>
          {plans.length === 0 ? (
            <p className="text-sm text-white/50">Nessuna scheda assegnata ancora.</p>
          ) : (
            <div className="space-y-2">
              {plans.map((p) => (
                <div key={p.id} className="flex items-center justify-between rounded-xl border border-white/10 px-4 py-3">
                  <div>
                    <p className="font-medium">{p.name}</p>
                    <p className="text-xs text-white/50">
                      {p.category || "Scheda"} {p.estimated_minutes ? `· ~${p.estimated_minutes} min` : ""}
                    </p>
                  </div>
                  <span className="text-xs text-white/30">{new Date(p.created_at).toLocaleDateString("it-IT")}</span>
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
