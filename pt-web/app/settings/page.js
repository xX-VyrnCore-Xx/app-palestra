"use client";

import { useState } from "react";
import { User, Copy, Check, Share2, LogOut, Loader2 } from "lucide-react";
import { useRouter } from "next/navigation";
import { supabase } from "../../lib/supabaseClient";
import { useAuthGuard } from "../../lib/useAuthGuard";
import AppShell from "../../components/AppShell";
import Avatar from "../../components/Avatar";
import { useToast } from "../../components/Toast";
import { SkeletonCard } from "../../components/Skeleton";

export default function SettingsPage() {
  const { profile, loading, setProfile } = useAuthGuard();
  const toast = useToast();
  const router = useRouter();
  const [nameDraft, setNameDraft] = useState("");
  const [savingName, setSavingName] = useState(false);
  const [signingOut, setSigningOut] = useState(false);
  const [copied, setCopied] = useState(false);

  // Seed the draft once the profile finishes loading (useAuthGuard starts it as null).
  if (profile && nameDraft === "" && profile.full_name) {
    // Safe to set state during render here: it only fires once, the first render after profile
    // arrives, and reads back the value it just set - same pattern React docs use for
    // "adjusting state when a prop changes" without an extra effect + flash of stale content.
    setNameDraft(profile.full_name);
  }

  async function saveName() {
    if (!profile || !nameDraft.trim() || nameDraft.trim() === profile.full_name) return;
    setSavingName(true);
    const { error } = await supabase.from("profiles").update({ full_name: nameDraft.trim() }).eq("id", profile.id);
    setSavingName(false);
    if (error) return toast.error("Impossibile salvare il nome.");
    setProfile((p) => ({ ...p, full_name: nameDraft.trim() }));
    toast.success("Nome aggiornato");
  }

  function copyInviteCode() {
    if (!profile?.invite_code) return;
    navigator.clipboard.writeText(profile.invite_code).then(() => {
      setCopied(true);
      toast.success("Codice copiato negli appunti");
      setTimeout(() => setCopied(false), 1500);
    });
  }

  function shareInviteCode() {
    if (!profile?.invite_code) return;
    const text = `Collegati a me su Vibe Fitness con il codice: ${profile.invite_code}`;
    if (navigator.share) {
      navigator.share({ text }).catch(() => {});
    } else {
      navigator.clipboard.writeText(text);
      toast.success("Messaggio di invito copiato");
    }
  }

  async function handleSignOut() {
    setSigningOut(true);
    await supabase.auth.signOut();
    router.replace("/login");
  }

  if (loading) {
    return (
      <AppShell profile={null}>
        <SkeletonCard />
      </AppShell>
    );
  }

  return (
    <AppShell profile={profile}>
      <div className="mx-auto max-w-xl animate-fade-in">
        <h1 className="text-2xl font-bold sm:text-3xl">Impostazioni</h1>
        <p className="mt-1 text-sm text-white/50">Il tuo profilo Personal Trainer.</p>

        <section className="glass-card mt-6 p-5">
          <div className="mb-4 flex items-center gap-3">
            <Avatar name={profile.full_name} size={56} />
            <div className="min-w-0">
              <p className="truncate font-semibold">{profile.full_name}</p>
              <p className="truncate text-sm text-white/50">{profile.email}</p>
            </div>
          </div>

          <label className="label-text" htmlFor="fullName">
            Nome completo
          </label>
          <div className="relative">
            <User size={17} className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-white/35" />
            <input
              id="fullName"
              className="input-field pl-10"
              value={nameDraft}
              onChange={(e) => setNameDraft(e.target.value)}
            />
          </div>
          <button
            className="btn-secondary mt-3"
            onClick={saveName}
            disabled={savingName || !nameDraft.trim() || nameDraft.trim() === profile.full_name}
          >
            {savingName && <Loader2 size={15} className="animate-spin" />}
            {savingName ? "Salvataggio…" : "Salva nome"}
          </button>
        </section>

        <section className="glass-card mt-4 p-5">
          <p className="label-text">Il tuo codice invito</p>
          <div className="flex items-center justify-between gap-3">
            <span className="text-2xl font-bold tracking-widest text-brand-orange">{profile.invite_code || "…"}</span>
            <div className="flex shrink-0 gap-1.5">
              <button
                onClick={copyInviteCode}
                className="flex h-9 w-9 items-center justify-center rounded-xl border border-white/10 bg-white/5 transition hover:bg-white/10"
                disabled={!profile.invite_code}
                aria-label="Copia codice"
              >
                {copied ? <Check size={15} className="text-emerald-400" /> : <Copy size={15} />}
              </button>
              <button
                onClick={shareInviteCode}
                className="flex h-9 w-9 items-center justify-center rounded-xl border border-white/10 bg-white/5 transition hover:bg-white/10"
                disabled={!profile.invite_code}
                aria-label="Condividi codice"
              >
                <Share2 size={15} />
              </button>
            </div>
          </div>
          <p className="mt-2 text-sm text-white/50">
            Dallo ai tuoi allievi per collegarli al volo, in registrazione o dal loro profilo.
          </p>
        </section>

        <button
          onClick={handleSignOut}
          disabled={signingOut}
          className="mt-4 flex w-full items-center justify-center gap-2 rounded-2xl border border-red-500/20 bg-red-500/5 px-4 py-3 text-sm font-medium text-red-300 transition hover:bg-red-500/10"
        >
          <LogOut size={16} /> {signingOut ? "Uscita…" : "Esci dall'account"}
        </button>
      </div>
    </AppShell>
  );
}
