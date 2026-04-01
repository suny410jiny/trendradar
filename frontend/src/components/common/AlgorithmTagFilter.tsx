import { useFilterStore } from '@/stores/filterStore';

const ALGORITHM_TAGS = [
  { value: 'SURGE', label: '급상승', icon: '🔥' },
  { value: 'GLOBAL', label: '글로벌', icon: '🌍' },
  { value: 'NEW_ENTRY', label: '신규진입', icon: '🆕' },
  { value: 'LONG_RUN', label: '롱런', icon: '📺' },
  { value: 'HOT_COMMENT', label: '화제성', icon: '💬' },
  { value: 'HIGH_ENGAGE', label: '고참여율', icon: '❤️' },
  { value: 'COMEBACK', label: '역주행', icon: '🔄' },
] as const;

export default function AlgorithmTagFilter() {
  const { tag, setTag } = useFilterStore();

  return (
    <div className="flex gap-2 flex-wrap" data-cy="tag-filter">
      <button
        onClick={() => setTag(null)}
        className={`rounded-full px-3 py-1 text-sm font-medium transition-all ${
          tag === null
            ? 'bg-primary text-primary-foreground'
            : 'bg-muted text-muted-foreground hover:bg-accent'
        }`}
      >
        전체
      </button>
      {ALGORITHM_TAGS.map((t) => (
        <button
          key={t.value}
          data-cy={`tag-${t.value}`}
          onClick={() => setTag(t.value)}
          className={`rounded-full px-3 py-1 text-sm font-medium transition-all ${
            tag === t.value
              ? 'bg-primary text-primary-foreground'
              : 'bg-muted text-muted-foreground hover:bg-accent'
          }`}
        >
          {t.icon}{t.label}
        </button>
      ))}
    </div>
  );
}
