"use client";

import { createClient } from "@supabase/supabase-js";

// Same project, same anon key + RLS policies the Android app's Supabase client uses - a PT
// signed in here operates under the exact same "own clients only" row-level security as the
// app, so there is no elevated/service-role access anywhere in this browser bundle.
const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL;
const supabaseAnonKey = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY;

if (!supabaseUrl || !supabaseAnonKey) {
  // Fails loudly at build/runtime rather than silently hitting an undefined URL - easier to
  // diagnose a missing Vercel env var than a cryptic fetch failure deep in a query.
  console.error(
    "Missing NEXT_PUBLIC_SUPABASE_URL / NEXT_PUBLIC_SUPABASE_ANON_KEY - set them in the Vercel project's Environment Variables."
  );
}

export const supabase = createClient(supabaseUrl, supabaseAnonKey, {
  auth: {
    persistSession: true,
    autoRefreshToken: true,
    detectSessionInUrl: true,
  },
});
