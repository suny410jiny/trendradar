import { useParams, useNavigate } from 'react-router-dom';
import { useSnapshots } from '@/hooks/useTrending';
import { useTrending } from '@/hooks/useTrending';
import { useFilterStore } from '@/stores/filterStore';
import AlgorithmTagBadge from '@/components/common/AlgorithmTagBadge';
import ViewTrendChart from '@/components/chart/ViewTrendChart';
import MetricCard from '@/components/chart/MetricCard';
import type { TrendingVideo } from '@/types';

function formatCount(n: number): string {
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
  if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
  return n.toLocaleString();
}

export default function DetailPage() {
  const { videoId } = useParams<{ videoId: string }>();
  const navigate = useNavigate();
  const { country } = useFilterStore();
  const { data: videos } = useTrending(country, null, 50);
  const { data: snapshots, isLoading: snapshotsLoading } = useSnapshots(videoId || '');

  const video: TrendingVideo | undefined = videos?.find((v) => v.videoId === videoId);

  if (!video) {
    return (
      <div className="min-h-screen bg-background p-6" data-cy="detail-page">
        <button onClick={() => navigate(-1)} className="text-sm text-muted-foreground hover:text-foreground mb-4">
          ← 뒤로가기
        </button>
        <p className="text-muted-foreground">영상 정보를 불러오는 중...</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background" data-cy="detail-page">
      {/* Header */}
      <div className="border-b border-border">
        <div className="mx-auto max-w-3xl px-4 py-4">
          <button
            onClick={() => navigate(-1)}
            className="text-sm text-muted-foreground hover:text-foreground transition-colors"
          >
            ← 뒤로가기
          </button>
        </div>
      </div>

      <main className="mx-auto max-w-3xl px-4 py-6 space-y-6">
        {/* 영상 정보 */}
        <div className="flex gap-4">
          <img
            src={video.thumbnailUrl}
            alt={video.title}
            className="h-32 w-56 rounded-lg object-cover bg-muted shrink-0"
          />
          <div className="flex flex-col justify-between">
            <div>
              <p className="text-xs text-muted-foreground mb-1">
                {video.rankPosition}위 · {video.categoryName}
              </p>
              <h1 className="text-lg font-bold text-foreground leading-tight">{video.title}</h1>
              <p className="text-sm text-muted-foreground mt-1">{video.channelTitle}</p>
            </div>
            <div className="flex gap-1.5 flex-wrap mt-2">
              {video.tags.map((tag) => (
                <AlgorithmTagBadge key={tag} tag={tag} />
              ))}
            </div>
          </div>
        </div>

        {/* 메트릭 카드 */}
        <div className="grid grid-cols-3 gap-3">
          <MetricCard title="조회수" value={formatCount(video.viewCount)} icon="👁" />
          <MetricCard
            title="좋아요율"
            value={video.viewCount ? `${((video.likeCount / video.viewCount) * 100).toFixed(1)}%` : '0%'}
            icon="❤️"
            subtitle={`${formatCount(video.likeCount)} likes`}
          />
          <MetricCard title="댓글수" value={formatCount(video.commentCount)} icon="💬" />
        </div>

        {/* 조회수 추이 차트 */}
        {snapshotsLoading ? (
          <div className="rounded-xl border border-border p-6 animate-pulse">
            <div className="h-4 w-40 rounded bg-muted mb-4" />
            <div className="h-64 rounded bg-muted" />
          </div>
        ) : snapshots && snapshots.length > 0 ? (
          <ViewTrendChart snapshots={snapshots} />
        ) : (
          <div className="rounded-xl border border-border p-6 text-center">
            <p className="text-sm text-muted-foreground">조회수 추이 데이터가 아직 없습니다.</p>
          </div>
        )}

        {/* YouTube 링크 */}
        <a
          href={`https://www.youtube.com/watch?v=${video.videoId}`}
          target="_blank"
          rel="noopener noreferrer"
          className="flex items-center justify-center gap-2 w-full rounded-xl bg-red-600 py-3 text-sm font-semibold text-white hover:bg-red-700 transition-colors"
          data-cy="youtube-link"
        >
          ▶ YouTube에서 보기
        </a>
      </main>
    </div>
  );
}
