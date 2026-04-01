export default function LoadingSpinner() {
  return (
    <div className="space-y-4" data-cy="loading-spinner">
      {[1, 2, 3].map((i) => (
        <div key={i} className="flex gap-4 rounded-xl border border-border p-4 animate-pulse">
          <div className="h-24 w-40 rounded-lg bg-muted" />
          <div className="flex-1 space-y-3">
            <div className="h-4 w-3/4 rounded bg-muted" />
            <div className="h-3 w-1/2 rounded bg-muted" />
            <div className="flex gap-2">
              <div className="h-3 w-16 rounded bg-muted" />
              <div className="h-3 w-16 rounded bg-muted" />
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
