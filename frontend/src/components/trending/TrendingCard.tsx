import { Link } from 'react-router-dom';
import AlgorithmTagBadge from '@/components/common/AlgorithmTagBadge';
import type { TrendingVideo } from '@/types';

interface Props {
  video: TrendingVideo;
}

function formatCount(n: number): string {
  if (n >= 1_000_000_000) return `${(n / 1_000_000_000).toFixed(1)}B`;
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
  if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
  return n.toString();
}

function engageRate(likes: number, views: number): string {
  if (!views) return '0%';
  return `${((likes / views) * 100).toFixed(1)}%`;
}

function timeAgo(dateStr: string): string {
  const now = new Date();
  const date = new Date(dateStr);
  const diffMs = now.getTime() - date.getTime();
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
  const diffDays = Math.floor(diffHours / 24);
  if (diffDays > 0) return `${diffDays}일 전`;
  if (diffHours > 0) return `${diffHours}시간 전`;
  return '방금 전';
}

export default function TrendingCard({ video }: Props) {
  return (
    <Link
      to={`/video/${video.videoId}`}
      data-cy="trending-card"
      className="flex gap-4 rounded-xl border border-border bg-card p-4 transition-all hover:shadow-md hover:border-primary/30"
    >
      {/* 랭킹 */}
      <div className="flex flex-col items-center justify-center w-12 shrink-0 gap-0.5">
        <span className={`text-lg font-bold ${
          video.rankPosition <= 3 ? 'text-primary' : 'text-muted-foreground'
        }`}>
          {video.rankPosition}
        </span>
        {/* 랭킹 변화 표시 */}
        {video.rankChangeType === 'UP' && (
          <span className="text-xs font-semibold text-green-500">▲{video.rankChange}</span>
        )}
        {video.rankChangeType === 'DOWN' && (
          <span className="text-xs font-semibold text-red-500">▼{video.rankChange}</span>
        )}
        {video.rankChangeType === 'SAME' && (
          <span className="text-xs text-muted-foreground">—</span>
        )}
        {video.rankChangeType === 'NEW' && (
          <span className="text-[10px] font-bold px-1.5 py-0.5 rounded bg-blue-500/20 text-blue-600">NEW</span>
        )}
      </div>

      {/* 썸네일 */}
      <div className="relative shrink-0">
        <img
          src={video.thumbnailUrl}
          alt={video.title}
          className="h-28 w-48 rounded-lg object-cover bg-muted"
          loading="lazy"
        />
        {video.duration && (
          <span className="absolute bottom-1 right-1 rounded bg-black/80 px-1.5 py-0.5 text-[10px] text-white">
            {video.duration.replace('PT', '').replace('H', ':').replace('M', ':').replace('S', '')}
          </span>
        )}
      </div>

      {/* 정보 */}
      <div className="flex flex-1 flex-col justify-between min-w-0">
        <div>
          <h3 className="text-base font-bold text-foreground line-clamp-2 leading-tight">
            {video.title}
          </h3>
          <div className="flex items-center gap-2 mt-1">
            <p className="text-xs text-muted-foreground truncate">
              {video.channelTitle}
              {video.categoryName && (
                <span className="ml-2 text-muted-foreground/60">· {video.categoryName}</span>
              )}
            </p>
            {video.publishedAt && (
              <span className="text-xs text-muted-foreground/70 whitespace-nowrap">
                {timeAgo(video.publishedAt)}
              </span>
            )}
            {video.targetDemographic && (
              <span className="text-xs px-2 py-0.5 rounded-full bg-primary/10 text-primary font-medium whitespace-nowrap">
                {video.targetDemographic}
              </span>
            )}
          </div>
        </div>

        {/* 태그 */}
        {video.tags.length > 0 && (
          <div className="flex gap-1 mt-1.5 flex-wrap">
            {video.tags.map((tag) => (
              <AlgorithmTagBadge key={tag} tag={tag} />
            ))}
          </div>
        )}

        {/* 통계 */}
        <div className="flex gap-3 mt-1.5 text-xs text-muted-foreground">
          <span>👁 {formatCount(video.viewCount)}</span>
          <span>❤️ {engageRate(video.likeCount, video.viewCount)}</span>
          <span>💬 {formatCount(video.commentCount)}</span>
        </div>
      </div>
    </Link>
  );
}
