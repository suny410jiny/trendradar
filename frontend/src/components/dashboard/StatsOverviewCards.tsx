import type { StatsOverview } from '@/types';

interface Props {
  stats: StatsOverview | undefined;
  isLoading: boolean;
}

export default function StatsOverviewCards({ stats, isLoading }: Props) {
  const Skeleton = () => (
    <div className="rounded-xl border border-border bg-card p-5 animate-pulse">
      <div className="flex items-center gap-2 mb-3">
        <div className="w-5 h-5 bg-muted-foreground rounded" />
        <div className="h-3 w-24 bg-muted-foreground rounded" />
      </div>
      <div className="h-8 w-32 bg-muted-foreground rounded mb-2" />
      <div className="h-2 w-40 bg-muted-foreground rounded" />
    </div>
  );

  if (isLoading) {
    return (
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4" data-cy="stats-cards-loading">
        {[1, 2, 3, 4].map((i) => (
          <Skeleton key={i} />
        ))}
      </div>
    );
  }

  if (!stats) {
    return null;
  }

  const formatNumber = (num: number): string => {
    if (num >= 1_000_000) {
      return (num / 1_000_000).toFixed(1) + 'M';
    }
    if (num >= 1_000) {
      return (num / 1_000).toFixed(1) + 'K';
    }
    return num.toString();
  };

  const formatPercent = (num: number): string => {
    // 백엔드에서 이미 퍼센트 값으로 반환 (예: 4.2 = 4.2%)
    return num.toFixed(1) + '%';
  };

  const cards = [
    {
      title: '총 영상',
      value: formatNumber(stats.totalVideos),
      icon: '📊',
      subtitle: `24시간 내 ${stats.uniqueVideos24h}개 고유 영상`,
      borderColor: 'border-l-4 border-l-blue-500',
      dataTestId: 'stats-total-videos',
    },
    {
      title: '신규진입',
      value: formatNumber(stats.newEntryCount),
      icon: '🆕',
      subtitle: stats.newEntryCount > 0 ? '+' + stats.newEntryCount : '0',
      borderColor: 'border-l-4 border-l-green-500',
      dataTestId: 'stats-new-entry',
    },
    {
      title: '급상승',
      value: formatNumber(stats.surgeCount),
      icon: '🔥',
      subtitle: stats.surgeCount > 0 ? '+' + stats.surgeCount : '0',
      borderColor: 'border-l-4 border-l-red-500',
      dataTestId: 'stats-surge',
    },
    {
      title: '참여율',
      value: formatPercent(stats.avgEngagementRate),
      icon: '❤️',
      subtitle: '평균 참여율',
      borderColor: 'border-l-4 border-l-pink-500',
      dataTestId: 'stats-engagement',
    },
  ];

  return (
    <div className="grid grid-cols-2 md:grid-cols-4 gap-4" data-cy="stats-cards">
      {cards.map((card) => (
        <div
          key={card.dataTestId}
          className={`rounded-xl border border-border bg-card p-5 ${card.borderColor}`}
          data-cy={card.dataTestId}
        >
          <div className="flex items-center gap-2 mb-2">
            <span className="text-xl">{card.icon}</span>
            <span className="text-xs font-medium text-muted-foreground">{card.title}</span>
          </div>
          <p className="text-2xl font-bold text-foreground">{card.value}</p>
          {card.subtitle && (
            <p className="text-xs text-muted-foreground mt-1">{card.subtitle}</p>
          )}
        </div>
      ))}
    </div>
  );
}
