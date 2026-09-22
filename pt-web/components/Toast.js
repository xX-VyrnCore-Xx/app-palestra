"use client";

import { createContext, useCallback, useContext, useState } from "react";

const ToastContext = createContext(null);

/**
 * Minimal toast system: every mutation in this app (save injuries, add note, send message,
 * renew membership...) used to succeed or fail silently, with only inline error text and no
 * positive confirmation - a PT saving a note had no way to know it actually went through short
 * of refreshing. `success`/`error` push a small pill in the corner that self-dismisses.
 */
export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const dismiss = useCallback((id) => {
    setToasts((t) => t.filter((toast) => toast.id !== id));
  }, []);

  const push = useCallback(
    (message, variant = "success") => {
      const id = Math.random().toString(36).slice(2);
      setToasts((t) => [...t, { id, message, variant }]);
      setTimeout(() => dismiss(id), 3200);
    },
    [dismiss]
  );

  const api = {
    success: (message) => push(message, "success"),
    error: (message) => push(message, "error"),
  };

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="pointer-events-none fixed inset-x-0 bottom-4 z-50 flex flex-col items-center gap-2 px-4">
        {toasts.map((toast) => (
          <div
            key={toast.id}
            role="status"
            className={`pointer-events-auto flex items-center gap-2 rounded-2xl border px-4 py-2.5 text-sm font-medium shadow-2xl shadow-black/40 backdrop-blur-xl animate-toast-in ${
              toast.variant === "error"
                ? "border-red-500/30 bg-red-950/80 text-red-200"
                : "border-emerald-500/30 bg-emerald-950/80 text-emerald-200"
            }`}
          >
            {toast.variant === "error" ? "⚠" : "✓"} {toast.message}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error("useToast must be used within ToastProvider");
  return ctx;
}
