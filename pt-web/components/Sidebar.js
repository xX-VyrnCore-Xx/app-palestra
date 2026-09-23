"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useState } from "react";
import { LayoutDashboard, Users, MessageCircle, Settings, LogOut, Menu, X, ChevronLeft } from "lucide-react";
import { supabase } from "../lib/supabaseClient";
import { useConversations } from "../lib/useConversations";
import Avatar from "./Avatar";

const NAV_ITEMS = [
  { href: "/", label: "Dashboard", icon: LayoutDashboard, exact: true },
  { href: "/clients", label: "Allievi", icon: Users, exact: false },
  { href: "/messages", label: "Messaggi", icon: MessageCircle, exact: false, badge: "unread" },
  { href: "/settings", label: "Impostazioni", icon: Settings, exact: true },
];

function isActive(pathname, item) {
  if (item.exact) return pathname === item.href;
  return pathname === item.href || pathname.startsWith(item.href + "/");
}

/**
 * Fixed left-hand navigation, replacing the old single-page-with-a-topbar layout - three real
 * destinations now exist (Dashboard, Allievi, Impostazioni) instead of everything living on one
 * screen. Collapses into a slide-over drawer under the `lg` breakpoint, opened by a small top
 * bar's hamburger button, closed by tapping the scrim or a nav item.
 */
export default function Sidebar({ profile, back }) {
  const pathname = usePathname();
  const router = useRouter();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [signingOut, setSigningOut] = useState(false);
  const { totalUnread } = useConversations(profile?.id);

  async function handleSignOut() {
    setSigningOut(true);
    await supabase.auth.signOut();
    router.replace("/login");
  }

  const content = (
    <div className="flex h-full flex-col">
      <div className="flex items-center gap-2.5 px-5 pb-6 pt-6">
        <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-brand-orange to-brand-orangeDeep text-sm font-bold shadow-lg shadow-brand-orange/20">
          V
        </div>
        <div className="leading-tight">
          <p className="text-sm font-semibold">Vibe Fitness</p>
          <p className="text-[11px] text-white/40">Gestionale PT</p>
        </div>
      </div>

      <nav className="flex-1 space-y-1 px-3">
        {NAV_ITEMS.map((item) => {
          const active = isActive(pathname, item);
          const Icon = item.icon;
          return (
            <Link
              key={item.href}
              href={item.href}
              onClick={() => setMobileOpen(false)}
              className={`flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition ${
                active
                  ? "bg-brand-orange/15 text-brand-orange"
                  : "text-white/60 hover:bg-white/5 hover:text-white"
              }`}
            >
              <Icon size={18} strokeWidth={active ? 2.4 : 2} />
              <span className="flex-1">{item.label}</span>
              {item.badge === "unread" && totalUnread > 0 && (
                <span className="flex h-5 min-w-5 items-center justify-center rounded-full bg-brand-orange px-1.5 text-[11px] font-bold text-white">
                  {totalUnread > 9 ? "9+" : totalUnread}
                </span>
              )}
            </Link>
          );
        })}
      </nav>

      {profile && (
        <div className="border-t border-white/10 p-3">
          <div className="flex items-center gap-2.5 rounded-xl px-2 py-2">
            <Avatar name={profile.full_name} size={34} />
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium">{profile.full_name}</p>
              <p className="truncate text-xs text-white/40">{profile.email}</p>
            </div>
          </div>
          <button
            onClick={handleSignOut}
            disabled={signingOut}
            className="mt-1 flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium text-red-300 transition hover:bg-red-500/10"
          >
            <LogOut size={18} /> {signingOut ? "Uscita…" : "Esci"}
          </button>
        </div>
      )}
    </div>
  );

  return (
    <>
      {/* Desktop: fixed column, always visible */}
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 border-r border-white/10 bg-[#120d1b] lg:block">
        {content}
      </aside>

      {/* Mobile: slim top bar with a hamburger + optional "back" link, drawer overlays on open */}
      <div className="sticky top-0 z-20 flex items-center justify-between border-b border-white/10 bg-[#0f0b17]/90 px-4 py-3 backdrop-blur-xl lg:hidden">
        <button
          onClick={() => setMobileOpen(true)}
          className="flex h-9 w-9 items-center justify-center rounded-xl border border-white/10 bg-white/5"
          aria-label="Apri il menu"
        >
          <Menu size={18} />
        </button>
        {back ? (
          <Link href={back.href} className="flex items-center gap-1 text-sm text-white/60">
            <ChevronLeft size={16} /> {back.label}
          </Link>
        ) : (
          <span className="text-sm font-semibold">Vibe Fitness</span>
        )}
        {profile ? <Avatar name={profile.full_name} size={30} /> : <div className="w-9" />}
      </div>

      {mobileOpen && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div className="absolute inset-0 bg-black/60" onClick={() => setMobileOpen(false)} />
          <aside className="absolute inset-y-0 left-0 w-72 bg-[#120d1b] shadow-2xl">
            <button
              onClick={() => setMobileOpen(false)}
              className="absolute right-3 top-3 flex h-8 w-8 items-center justify-center rounded-lg text-white/50 hover:bg-white/5"
              aria-label="Chiudi il menu"
            >
              <X size={18} />
            </button>
            {content}
          </aside>
        </div>
      )}
    </>
  );
}
