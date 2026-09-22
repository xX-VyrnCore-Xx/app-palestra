"use client";

import { useState, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { Mail, Lock, Eye, EyeOff, AlertCircle, Loader2 } from "lucide-react";
import { supabase } from "../../lib/supabaseClient";

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const errorParam = searchParams.get("error");
  const [error, setError] = useState(
    errorParam === "not-pt"
      ? "Questo account non è un Personal Trainer. Il gestionale è riservato ai PT."
      : errorParam === "confirm-email"
      ? "Controlla la tua email per confermare l'account, poi accedi qui."
      : null
  );

  async function handleSubmit(e) {
    e.preventDefault();
    setError(null);

    if (!email.trim() || !password) {
      setError("Inserisci email e password.");
      return;
    }

    setLoading(true);
    const { data, error: signInError } = await supabase.auth.signInWithPassword({
      email: email.trim(),
      password,
    });
    if (signInError) {
      setLoading(false);
      setError(
        signInError.message.includes("Invalid login credentials")
          ? "Email o password non corrette."
          : signInError.message.includes("Failed to fetch") || signInError.message.includes("NetworkError")
          ? "Impossibile contattare il server. Controlla la connessione e riprova."
          : signInError.message
      );
      return;
    }

    const { data: profile, error: profileError } = await supabase
      .from("profiles")
      .select("role")
      .eq("id", data.user.id)
      .single();

    if (profileError || !profile || profile.role !== "PT") {
      await supabase.auth.signOut();
      setLoading(false);
      setError("Questo account non è un Personal Trainer. Il gestionale è riservato ai PT.");
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
          <h1 className="text-2xl font-bold">Vibe Fitness</h1>
          <p className="mt-1 text-sm text-white/50">Gestionale Personal Trainer</p>
        </div>

        <form onSubmit={handleSubmit} className="glass-card space-y-4 p-6">
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
              Password
            </label>
            <div className="relative">
              <Lock size={17} className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-white/35" />
              <input
                id="password"
                type={showPassword ? "text" : "password"}
                autoComplete="current-password"
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
          </div>

          {error && (
            <p className="flex items-start gap-2 rounded-xl border border-red-500/30 bg-red-500/10 px-3 py-2.5 text-sm text-red-300">
              <AlertCircle size={16} className="mt-0.5 shrink-0" /> {error}
            </p>
          )}

          <button type="submit" className="btn-primary" disabled={loading}>
            {loading && <Loader2 size={16} className="animate-spin" />}
            {loading ? "Accesso in corso…" : "Accedi"}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-white/50">
          Non hai un account?{" "}
          <Link href="/register" className="font-medium text-brand-orange hover:text-brand-orangeDeep">
            Registrati come PT
          </Link>
        </p>
      </div>
    </div>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={null}>
      <LoginForm />
    </Suspense>
  );
}
