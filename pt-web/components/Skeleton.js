export function SkeletonLine({ className = "" }) {
  return <div className={`animate-pulse rounded-lg bg-white/[0.06] ${className}`} />;
}

export function SkeletonCard() {
  return (
    <div className="glass-card space-y-3 p-4">
      <div className="flex items-center gap-3">
        <SkeletonLine className="h-10 w-10 rounded-full" />
        <div className="flex-1 space-y-2">
          <SkeletonLine className="h-3.5 w-1/3" />
          <SkeletonLine className="h-3 w-1/2" />
        </div>
      </div>
    </div>
  );
}

export function SkeletonList({ count = 3 }) {
  return (
    <div className="space-y-2">
      {Array.from({ length: count }).map((_, i) => (
        <SkeletonCard key={i} />
      ))}
    </div>
  );
}
