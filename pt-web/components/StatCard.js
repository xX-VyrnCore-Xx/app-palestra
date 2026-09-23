export default function StatCard({ icon: Icon, label, value, accent = "text-white" }) {
  return (
    <div className="glass-card flex items-center gap-3 p-4">
      <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-white/[0.09] to-white/[0.02] ring-1 ring-white/10">
        <Icon size={19} className={accent} />
      </div>
      <div className="min-w-0">
        <p className={`text-xl font-bold leading-none ${accent}`}>{value}</p>
        <p className="mt-1.5 truncate text-xs text-white/50">{label}</p>
      </div>
    </div>
  );
}
