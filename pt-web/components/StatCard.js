export default function StatCard({ icon: Icon, label, value, accent = "text-white" }) {
  return (
    <div className="glass-card flex items-center gap-3 p-4">
      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-white/[0.06]">
        <Icon size={18} className={accent} />
      </div>
      <div className="min-w-0">
        <p className={`text-xl font-bold leading-none ${accent}`}>{value}</p>
        <p className="mt-1 truncate text-xs text-white/50">{label}</p>
      </div>
    </div>
  );
}
