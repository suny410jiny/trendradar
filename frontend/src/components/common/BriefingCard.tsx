import { useBriefing } from '@/hooks/useBriefing';
import { useFilterStore } from '@/stores/filterStore';

export default function BriefingCard() {
  const { country } = useFilterStore();
  const { data, isLoading } = useBriefing(country);

  if (isLoading) {
    return (
      <div className="rounded-xl border border-border bg-card p-6 animate-pulse" data-cy="briefing-card">
        <div className="h-4 w-48 rounded bg-muted mb-3" />
        <div className="space-y-2">
          <div className="h-3 w-full rounded bg-muted" />
          <div className="h-3 w-5/6 rounded bg-muted" />
          <div className="h-3 w-4/6 rounded bg-muted" />
        </div>
      </div>
    );
  }

  if (!data) return null;

  return (
    <div className="rounded-xl border border-border bg-card p-6" data-cy="briefing-card">
      <h2 className="text-lg font-semibold text-foreground mb-3 flex items-center gap-2">
        <span>💡</span>
        <span>오늘의 {data.countryName} 트렌드 브리핑</span>
      </h2>
      <div className="text-sm text-muted-foreground leading-relaxed whitespace-pre-line">
        {data.summary}
      </div>
    </div>
  );
}
