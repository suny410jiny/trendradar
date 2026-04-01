import { useTrending } from '@/hooks/useTrending';
import { useFilterStore } from '@/stores/filterStore';
import TrendingCard from './TrendingCard';
import LoadingSpinner from '@/components/common/LoadingSpinner';

export default function TrendingList() {
  const { country, category, tag } = useFilterStore();
  const { data: videos, isLoading, error } = useTrending(country, category, tag, 50);

  if (isLoading) return <LoadingSpinner />;

  if (error) {
    return (
      <div className="rounded-xl border border-destructive/30 bg-destructive/5 p-8 text-center" data-cy="trending-error">
        <p className="text-sm text-destructive">데이터를 불러오는 중 오류가 발생했습니다.</p>
        <p className="mt-1 text-xs text-muted-foreground">잠시 후 다시 시도해주세요.</p>
      </div>
    );
  }

  if (!videos || videos.length === 0) {
    return (
      <div className="rounded-xl border border-border p-8 text-center" data-cy="trending-empty">
        <p className="text-sm text-muted-foreground">트렌딩 데이터가 없습니다.</p>
        <p className="mt-1 text-xs text-muted-foreground">수집이 아직 진행되지 않았을 수 있습니다.</p>
      </div>
    );
  }

  return (
    <div className="space-y-3" data-cy="trending-list">
      {videos.map((video) => (
        <TrendingCard key={`${video.videoId}-${video.rankPosition}`} video={video} />
      ))}
    </div>
  );
}
