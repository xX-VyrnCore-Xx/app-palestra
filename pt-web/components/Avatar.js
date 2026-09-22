"use client";

// Same idea as the Android app's avatar fallback: initials on a deterministic gradient (hashed
// from the name) so every person gets a stable, distinguishable color instead of every avatar
// looking identical.
const GRADIENTS = [
  "from-orange-400 to-rose-500",
  "from-violet-400 to-indigo-500",
  "from-emerald-400 to-teal-500",
  "from-sky-400 to-blue-500",
  "from-fuchsia-400 to-pink-500",
  "from-amber-400 to-orange-500",
];

function hashName(name) {
  let hash = 0;
  for (let i = 0; i < name.length; i++) hash = (hash * 31 + name.charCodeAt(i)) >>> 0;
  return hash;
}

function initials(name) {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "?";
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

export default function Avatar({ name = "", size = 40, className = "" }) {
  const gradient = GRADIENTS[hashName(name) % GRADIENTS.length];
  return (
    <div
      className={`flex shrink-0 items-center justify-center rounded-full bg-gradient-to-br font-semibold text-white ${gradient} ${className}`}
      style={{ width: size, height: size, fontSize: size * 0.4 }}
    >
      {initials(name)}
    </div>
  );
}
