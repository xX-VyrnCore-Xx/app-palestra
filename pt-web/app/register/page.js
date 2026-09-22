"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { User, Mail, Lock, Eye, EyeOff, AlertCircle, Loader2 } from "lucide-react";
import { supabase } from "../../lib/supabaseClient";

export default function RegisterPage() {
  const router = useRouter();
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const passwordStrength = scorePassword(password);

  async function handleSubmit(e) {
    e.preventDefault();
    setError(null);

    if (!fullName.trim()) return setError("Come ti chiami?");
    if (!email.trim()) return setError("Inserisci la tua email.");
    if (password.length < 8) return setError("La password deve avere almeno 8 caratteri.");

    setLoading(true);
    const { data, error: signUpError } = await supabase.auth.signUp({
      email: email.trim(),
      password,
    });

    if (signUpError) {
      setLoading(false);
      setError(
        signUpError.message.includes("already registered") || signUpError.message.includes("already exists")
          ? "Esiste già un account con questa email."
          : signUpError.message
      );
      return;
    }

    if (!data.session || !data.user) {
      // Email confirmation is enabled on this project - no session yet, so we can't create the
      // profile row client-side (RLS requires auth.uid() = id). Send them to confirm and log in.
      setLoading(false);
      setError(null);
      router.push("/login?error=confirm-email");
      return;
    }

    const { error: profileError } = await supabase.from("profiles").upsert({
      id: data.user.id,
      email: email.trim(),
      full_name: fullName.trim(),
      role: "PT",
    });

    setLoading(false);
    if (profileError) {
      setError("Account creato ma profilo non salvato: " + profileError.message);
      return;
    }

    router.push("/");
  }

  return (
    <div className="flex min-h-screen items-center justify-center px-4 py-12">
      <div className="w-full max-w-sm animate-fade-in">
        <div className="mb-8 text-center">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-brand-orange to-brand-orangeDeep text-2xl font-bold shadow-lg shadow-brand-orange/30">
            V
          </div>
          <h1 className="text-2xl font-bold">Crea il tuo account PT</h1>
          <p className="mt-1 text-sm text-white/50">Gestionale Personal Trainer</p>
        </div>

        <form onSubmit={handleSubmit} className="glass-card space-y-4 p-6">
          <div>
            <label className="label-text" htmlFor="fullName">
              Nome completo
            </label>
            <div className="relative">
              <User size={17} className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-white/35" />
              <input
                id="fullName"
                type="text"
                autoComplete="name"
                className="input-field pl-10"
                placeholder="Mario Rossi"
                value={fullName}
                onChange={(e) => {
                  setFullName(e.target.value);
                  setError(null);
                }}
              />
            </div>
          </div>

          <div>
            <label className="label-text" htmlFor="email">
              Email
            </label>
            <div className="relative">
              <Mail size={17} className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-white/35" />
              <input
                id="email"
                type="email"
                autoComplete="email"
                autoCapitalize="none"
                autoCorrect="off"
                className="input-field pl-10"
                placeholder="tu@esempio.it"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value);
                  setError(null);
                }}
              />
            </div>
          </div>

          <div>
            <label className="label-text" htmlFor="password">
              Password (min. 8 caratteri)
            </label>
            <div className="relative">
              <Lock size={17} className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-white/35" />
              <input
                id="password"
                type={showPassword ? "text" : "password"}
                autoComplete="new-password"
                className="input-field pl-10 pr-11"
                placeholder="••••••••"
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value);
                  setError(null);
                }}
              />
              <button
                type="button"
                onClick={() => setShowPassword((v) => !v)}
                className="absolute right-3.5 top-1/2 -translate-y-1/2 text-white/40 hover:text-white/80"
                aria-label={showPassword ? "Nascondi password" : "Mostra password"}
              >
                {showPassword ? <EyeOff size={17} /> : <Eye size={17} />}
              </button>
            </div>
            {password.length > 0 && (
              <div className="mt-2">
                <div className="flex h-1.5 gap-1">
                  {[0, 1, 2, 3].map((i) => (
                    <div
                      key={i}
                      className={`flex-1 rounded-full transition-colors ${
                        i < passwordStrength.score ? passwordStrength.color : "bg-white/10"
                      }`}
                    />
                  ))}
                </div>
                <p className="mt-1 text-xs text-white/40">{passwordStrength.label}</p>
              </div>
            )}
          </div>

          {error && (
            <p className="flex items-start gap-2 rounded-xl border border-red-500/30 bg-red-500/10 px-3 py-2.5 text-sm text-red-300">
              <AlertCircle size={16} className="mt-0.5 shrink-0" /> {error}
            </p>
          )}

          <button type="submit" className="btn-primary" disabled={loading}>
            {loading && <Loader2 size={16} className="animate-spin" />}
            {loading ? "Creazione account…" : "Registrati"}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-white/50">
          Hai già un account?{" "}
          <Link href="/login" className="font-medium text-brand-orange hover:text-brand-orangeDeep">
            Accedi
          </Link>
        </p>
      </div>
    </div>
  );
}

function scorePassword(password) {
  if (!password) return { score: 0, label: "", color: "" };
  let score = 0;
  if (password.length >= 8) score++;
  if (password.length >= 12) score++;
  if (/[A-Z]/.test(password) && /[a-z]/.test(password)) score++;
  if (/[0-9]/.test(password) && /[^A-Za-z0-9]/.test(password)) score++;

  const levels = [
    { label: "Debole", color: "bg-red-500" },
    { label: "Debole", color: "bg-red-500" },
    { label: "Discreta", color: "bg-amber-400" },
    { label: "Buona", color: "bg-lime-400" },
    { label: "Ottima", color: "bg-emerald-400" },
  ];
  return { score, ...levels[score] };
}
