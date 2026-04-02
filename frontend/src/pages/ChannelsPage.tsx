import { useFilterStore } from '../stores/filterStore';
import { useChannelRanking } from '../hooks/useChannels';
import { Link } from 'react-router-dom';
import { GRADE_CONFIG } from '../types';

export default function ChannelsPage() {
  const { country } = useFilterStore();
  const { data: channels, isLoading } = useChannelRanking(country, 100);

  return (
    <div className="space-y-4" data-cy="channels-page">
      <h1 className="text-xl font-bold">채널 급부상 랭킹 TOP 100</h1>

      {isLoading && <div className="text-gray-500">로딩 중...</div>}

      <div className="bg-gray-900 rounded-xl border border-gray-800 overflow-hidden">
        {/* Header */}
        <div className="grid grid-cols-12 gap-2 px-4 py-3 bg-gray-800/50 text-xs text-gray-500 font-medium">
          <div className="col-span-1">#</div>
          <div className="col-span-1">등급</div>
          <div className="col-span-4">채널</div>
          <div className="col-span-2 text-right">구독자</div>
          <div className="col-span-2 text-right">트렌딩 영상</div>
          <div className="col-span-2 text-right">스코어</div>
        </div>

        {/* Rows */}
        {channels?.map((ch, i) => {
          const gradeConf = GRADE_CONFIG[ch.grade] ?? GRADE_CONFIG['D'];
          return (
            <Link key={ch.channelId} to={`/channels/${ch.channelId}`}
              className="grid grid-cols-12 gap-2 px-4 py-3 items-center border-t border-gray-800/50 hover:bg-gray-800/30 transition-colors">
              <div className="col-span-1 text-sm font-bold text-gray-500">{i + 1}</div>
              <div className="col-span-1">
                <span className={`text-sm font-bold px-2 py-0.5 rounded ${gradeConf.bgColor} ${gradeConf.color}`}>
                  {ch.grade}
                </span>
              </div>
              <div className="col-span-4 flex items-center gap-2 min-w-0">
                {ch.thumbnailUrl && <img src={ch.thumbnailUrl} alt="" className="w-8 h-8 rounded-full" />}
                <div className="min-w-0">
                  <div className="text-sm font-medium truncate">{ch.title}</div>
                  {ch.darkhorse && <span className="text-xs text-yellow-400">다크호스</span>}
                </div>
              </div>
              <div className="col-span-2 text-right text-sm text-gray-400">{formatCount(ch.subscriberCount)}</div>
              <div className="col-span-2 text-right text-sm text-gray-400">{ch.trendingVideoCount}개</div>
              <div className="col-span-2 text-right">
                <span className="text-sm font-mono font-bold text-blue-400">{(ch.surgeScore * 100).toFixed(1)}</span>
              </div>
            </Link>
          );
        })}

        {!channels?.length && !isLoading && (
          <div className="px-4 py-8 text-center text-gray-600">데이터 수집 중... 채널 정보가 쌓이면 랭킹이 표시됩니다.</div>
        )}
      </div>
    </div>
  );
}

function formatCount(n: number): string {
  if (n >= 1_000_000_000) return `${(n / 1_000_000_000).toFixed(1)}B`;
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
  if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
  return String(n);
}
