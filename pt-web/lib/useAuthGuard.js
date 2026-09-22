"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { supabase } from "./supabaseClient";

/**
 * Client-side auth + role guard shared by every protected page: waits for the Supabase session
 * to resolve, redirects to /login if there isn't one, loads the profile row and redirects to
 * /login (with an explanatory message) if it isn't a PT - an allievo account has no reason to be
 * here, and this app never had a self-serve "become a PT" path. Returns null profile/session
 * while `loading` is true so pages can render a spinner instead of a flash of protected content.
 */
export function useAuthGuard() {
  const router = useRouter();
  const [session, setSession] = useState(null);
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;

    async function load() {
      const {
        data: { session: current },
      } = await supabase.auth.getSession();

      if (!active) return;

      if (!current) {
        router.replace("/login");
        return;
      }

      const { data: profileRow, error } = await supabase
        .from("profiles")
        .select("id, full_name, email, role, invite_code")
        .eq("id", current.user.id)
        .single();

      if (!active) return;

      if (error || !profileRow || profileRow.role !== "PT") {
        await supabase.auth.signOut();
        router.replace("/login?error=not-pt");
        return;
      }

      setSession(current);
      setProfile(profileRow);
      setLoading(false);
    }

    load();

    const { data: listener } = supabase.auth.onAuthStateChange((_event, next) => {
      if (!next) router.replace("/login");
    });

    return () => {
      active = false;
      listener?.subscription?.unsubscribe();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return { session, profile, loading, setProfile };
}
