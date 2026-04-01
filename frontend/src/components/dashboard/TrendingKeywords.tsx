import type { TrendingKeyword } from '@/types';

interface Props {
  data: TrendingKeyword[] | undefined;
  isLoading: boolean;
}

export default function TrendingKeywords({ data, isLoading }: Props) {
  if (isLoading) {
    return (
      <div className="rounded-xl border border-border bg-card p-5" data-cy="keywords-loading">
        <h3 className="text-foreground font-semibold mb-4">트렌딩 키워드</h3>
        <div className="flex flex-wrap gap-2">
          {Array.from({ length: 15 }).map((_, i) => (
            <div
              key={i}
              className="h-7 rounded-full bg-muted animate-pulse"
              style={{ width: `${60 + Math.random() * 80}px` }}
            />
          ))}
        </div>
      </div>
    );
  }

  if (!data || data.length === 0) {
    return (
      <div className="rounded-xl border border-border bg-card p-5" data-cy="keywords-empty">
        <h3 className="text-foreground font-semibold mb-4">트렌딩 키워드</h3>
        <p className="text-sm text-muted-foreground">키워드 데이터가 없습니다</p>
      </div>
    );
  }

  const maxCount = data[0]?.count || 1;

  // 키워드 크기/색상 계산
  const getKeywordStyle = (keyword: TrendingKeyword) => {
    const ratio = keyword.count / maxCount;

    // 크기: 12px ~ 20px
    const fontSize = 12 + ratio * 8;

    // 색상: 높을수록 진한 파란색
    const opacity = 0.4 + ratio * 0.6;

    return { fontSize: `${fontSize}px`, opacity };
  };

  return (
    <div className="rounded-xl border border-border bg-card p-5" data-cy="trending-keywords">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-foreground font-semibold">트렌딩 키워드</h3>
        <span className="text-xs text-muted-foreground">YouTube 태그 기반</span>
      </div>

      <div className="flex flex-wrap gap-2">
        {data.map((keyword) => {
          const style = getKeywordStyle(keyword);
          return (
            <span
              key={keyword.keyword}
              className="inline-flex items-center rounded-full border border-primary/20 bg-primary/5 px-3 py-1 text-primary font-medium transition-all hover:bg-primary/10 hover:border-primary/40 cursor-default"
              style={{ fontSize: style.fontSize, opacity: style.opacity }}
              title={`${keyword.count}개 영상 (${keyword.percentage}%)`}
            >
              #{keyword.keyword}
            </span>
          );
        })}
      </div>
    </div>
  );
}
