"use client";

import Link from "next/link";
import { ChevronLeft } from "lucide-react";
import Sidebar from "./Sidebar";

/**
 * Page chrome: a fixed left sidebar on desktop (see Sidebar.js) with the main content offset to
 * the right of it; on mobile the sidebar becomes a drawer behind a hamburger button and content
 * runs full-width underneath its own slim top bar (which renders `back` itself there). On
 * desktop, `back` renders as a small link above the page content instead, since the sidebar has
 * no room for it.
 */
export default function AppShell({ profile, back, children }) {
  return (
    <div className="min-h-screen">
      <Sidebar profile={profile} back={back} />
      <main className="lg:pl-64">
        <div className="mx-auto max-w-6xl px-4 py-6 sm:px-6 sm:py-8 lg:px-10">
          {back && (
            <Link
              href={back.href}
              className="mb-4 hidden items-center gap-1 text-sm text-white/50 transition hover:text-white/80 lg:flex"
            >
              <ChevronLeft size={16} /> {back.label}
            </Link>
          )}
          {children}
        </div>
      </main>
    </div>
  );
}
