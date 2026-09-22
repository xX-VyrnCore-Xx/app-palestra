"use client";

import { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { supabase } from "../../../lib/supabaseClient";
import { useAuthGuard } from "../../../lib/useAuthGuard";

const MEMBERSHIP_STATUS = {
  active: { label: "Attivo", classes: "bg-emerald-500/15 text-emerald-300" },
  expiring: { label: "In scadenza", classes: "bg-amber-500/15 text-amber-300" },
  expired: { label: "Scaduto", classes: "bg-red-500/15 text-red-300" },
  none: { label: "Nessun abbonamento", classes: "bg-white/10 text-white/50" },
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

  const loadAll = useCallback(async () => {
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
    setInjuriesDraft(clientRow?.injuries || "");
    setPlans(planRows || []);
    setMembership((membershipRows && membershipRows[0]) || null);
    setNotes(noteRows || []);
    setMessages(messageRows || []);
    setLoading(false);
  }, [pt, id]);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  // Lightweight polling instead of a realtime subscription - keeps this page simple and still
  // gets new messages/plan changes within a few seconds, which is enough for a management tool
  // (not a chat app you'd stare at all day).
  useEffect(() => {
    const interval = setInterval(loadAll, 6000);
    return () => clearInterval(interval);
  }, [loadAll]);

  async function saveInjuries() {
    setSavingInjuries(true);
    await supabase
      .from("profiles")
      .update({ injuries: injuriesDraft.trim() || null })
      .eq("id", id);
    setSavingInjuries(false);
    setClient((c) => ({ ...c, injuries: injuriesDraft.trim() || null }));
  }

  async function addNote() {
    if (!noteDraft.trim()) return;
    const { data } = await supabase
      .from("pt_notes")
      .insert({ pt_id: pt.id, client_id: id, content: noteDraft.trim() })
      .select()
      .single();
    if (data) setNotes((n) => [data, ...n]);
    setNoteDraft("");
  }

  async function sendMessage() {
    if (!messageDraft.trim()) return;
    const content = messageDraft.trim();
    setMessageDraft("");
    const { data } = await supabase
      .from("messages")
      .insert({ sender_id: pt.id, recipient_id: id, content })
      .select()
      .single();
    if (data) setMessages((m) => [...m, data]);
  }

  async function saveMembership(e) {
    e.preventDefault();
    if (!membershipForm.startDate || !membershipForm.endDate) return;
    const { data } = await supabase
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
    if (data) {
      setMembership(data);
      setShowMembershipForm(false);
      setMembershipForm({ planLabel: "", startDate: "", endDate: "", notes: "" });
    }
  }

  if (authLoading || loading) return <CenteredSpinner />;
  if (!client) return <CenteredSpinner label="Allievo non trovato" />;

  const status = membershipStatus(membership);
  const statusMeta = MEMBERSHIP_STATUS[status];

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <Link href="/" className="mb-4 inline-block text-sm text-white/50 hover:text-white/80">
        ‹ Torna agli allievi
      </Link>

      <header className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">{client.full_name}</h1>
          <p className="text-sm text-white/50">{client.email}</p>
        </div>
        <Link href={`/clients/${id}/plan/new`} className="btn-primary w-auto px-5">
          + Nuova scheda
        </Link>
      </header>

      <div className="grid gap-6 md:grid-cols-2">
        <section className="glass-card p-5">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="font-semibold">Abbonamento</h2>
            <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${statusMeta.classes}`}>
              {statusMeta.label}
            </span>
          </div>
          {membership ? (
            <div className="text-sm text-white/70">
              <p>{membership.plan_label || "Piano generico"}</p>
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
          <h2 className="mb-3 font-semibold">Infortuni / limitazioni</h2>
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

      <section className="glass-card mt-6 p-5">
        <h2 className="mb-3 font-semibold">Schede assegnate ({plans.length})</h2>
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

      <div className="mt-6 grid gap-6 md:grid-cols-2">
        <section className="glass-card p-5">
          <h2 className="mb-3 font-semibold">Note private</h2>
          <div className="mb-3 flex gap-2">
            <input
              className="input-field"
              placeholder="Aggiungi una nota…"
              value={noteDraft}
              onChange={(e) => setNoteDraft(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && addNote()}
            />
            <button className="btn-secondary" onClick={addNote}>
              +
            </button>
          </div>
          <div className="max-h-52 space-y-2 overflow-y-auto">
            {notes.length === 0 && <p className="text-sm text-white/40">Nessuna nota.</p>}
            {notes.map((n) => (
              <div key={n.id} className="rounded-xl bg-white/5 px-3 py-2 text-sm">
                <p>{n.content}</p>
                <p className="mt-1 text-xs text-white/30">{new Date(n.created_at).toLocaleString("it-IT")}</p>
              </div>
            ))}
          </div>
        </section>

        <section className="glass-card flex flex-col p-5">
          <h2 className="mb-3 font-semibold">Chat</h2>
          <div className="mb-3 flex-1 space-y-2 overflow-y-auto" style={{ maxHeight: "13rem" }}>
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
            <button className="btn-secondary" onClick={sendMessage}>
              Invia
            </button>
          </div>
        </section>
      </div>
    </div>
  );
}

function CenteredSpinner({ label }) {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-3">
      <div className="h-8 w-8 animate-spin rounded-full border-2 border-white/20 border-t-brand-orange" />
      {label && <p className="text-sm text-white/50">{label}</p>}
    </div>
  );
}
