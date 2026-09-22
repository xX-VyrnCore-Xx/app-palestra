"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { LogOut, ChevronLeft } from "lucide-react";
import { supabase } from "../lib/supabaseClient";
import Avatar from "./Avatar";

/**
 * Persistent chrome for every protected page: brand mark, the signed-in PT's identity, sign out.
 * `back` renders a "‹ Torna a…" link in the header instead of the page having to build its own
 * (every page used to hand-roll this, inconsistently placed and styled).
 */
export default function AppShell({ profile, back, children }) {
  const router = useRouter();
  const [menuOpen, setMenuOpen] = useState(false);
  const [signingOut, setSigningOut] = useState(false);

  async function handleSignOut() {
    setSigningOut(true);
    await supabase.auth.signOut();
    router.replace("/login");
  }

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-30 border-b border-white/10 bg-[#0f0b17]/80 backdrop-blur-xl">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
          <Link href="/" className="flex items-center gap-2.5">
            <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-gradient-to-br from-brand-orange to-brand-orangeDeep text-sm font-bold shadow-lg shadow-brand-orange/20">
              V
            </div>
            <div className="leading-tight">
              <p className="text-sm font-semibold">Vibe Fitness</p>
              <p className="text-[11px] text-white/40">Gestionale PT</p>
            </div>
          </Link>

          {back && (
            <Link
              href={back.href}
              className="hidden items-center gap-1 text-sm text-white/50 transition hover:text-white/80 sm:flex"
            >
              <ChevronLeft size={16} /> {back.label}
            </Link>
          )}

          {profile && (
            <div className="relative">
              <button
                onClick={() => setMenuOpen((v) => !v)}
                className="flex items-center gap-2 rounded-full border border-white/10 bg-white/5 py-1 pl-1 pr-3 transition hover:bg-white/10"
              >
                <Avatar name={profile.full_name} size={28} />
                <span className="hidden text-sm font-medium sm:inline">
                  {profile.full_name?.split(" ")[0]}
                </span>
              </button>
              {menuOpen && (
                <>
                  <div className="fixed inset-0 z-10" onClick={() => setMenuOpen(false)} />
                  <div className="glass-card absolute right-0 top-12 z-20 w-52 overflow-hidden p-1.5">
                    <div className="px-3 py-2">
                      <p className="truncate text-sm font-medium">{profile.full_name}</p>
                      <p className="truncate text-xs text-white/40">{profile.email}</p>
                    </div>
                    <div className="my-1 h-px bg-white/10" />
                    <button
                      onClick={handleSignOut}
                      disabled={signingOut}
                      className="flex w-full items-center gap-2 rounded-xl px-3 py-2 text-left text-sm text-red-300 transition hover:bg-red-500/10"
                    >
                      <LogOut size={15} /> {signingOut ? "Uscita…" : "Esci"}
                    </button>
                  </div>
                </>
              )}
            </div>
          )}
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-6 sm:py-8">{children}</main>
    </div>
  );
}
