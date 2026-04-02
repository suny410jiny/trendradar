import { useFilterStore } from '../stores/filterStore';
import { useChannelRanking } from '../hooks/useChannels';
import { Link } from 'react-router-dom';
import { GRADE_CONFIG } from '../types';
import { formatCount } from '../utils/format';

export default function ChannelsPage() {
  const { country } = useFilterStore();
  const { data: channels, isLoading } = useChannelRanking(country, 100);

  return (
    <div className="space-y-6" data-cy="channels-page">
      <h1 className="text-xl font-extrabold tracking-tight" style={{ fontFamily: 'var(--font-heading)' }}>
        채널 급부상 랭킹 TOP 100
      </h1>

      {isLoading && <div className="text-muted-foreground text-sm">로딩 중...</div>}

      <div className="bg-card rounded-[20px] shadow-[0_1px_3px_rgba(0,0,0,0.03),0_8px_24px_rgba(0,0,0,0.03)] overflow-hidden">
        <div className="grid grid-cols-12 gap-2 px-7 py-3.5 bg-secondary text-xs text-muted-foreground font-semibold uppercase tracking-[0.5px]">
          <div className="col-span-1">#</div>
          <div className="col-span-1">등급</div>
          <div className="col-span-4">채널</div>
          <div className="col-span-2 text-right">구독자</div>
          <div className="col-span-2 text-right">트렌딩</div>
          <div className="col-span-2 text-right">스코어</div>
        </div>

        {channels?.map((ch, i) => {
          const gc = GRADE_CONFIG[ch.grade] ?? GRADE_CONFIG['D'];
          return (
            <Link key={ch.channelId} to={`/channels/${ch.channelId}`}
              className="grid grid-cols-12 gap-2 px-7 py-3.5 items-center border-t border-border/40 hover:bg-secondary/50 transition-colors">
              <div className={`col-span-1 text-sm font-black ${i < 3 ? 'text-accent' : 'text-border'}`}
                style={{ fontFamily: 'var(--font-heading)' }}>
                {String(i + 1).padStart(2, '0')}
              </div>
              <div className="col-span-1">
                <span className={`text-[11px] font-extrabold px-2.5 py-1 rounded-lg ${gc.bgColor} ${gc.color}`}>
                  {ch.grade}
                </span>
              </div>
              <div className="col-span-4 flex items-center gap-3 min-w-0">
                {ch.thumbnailUrl ? (
                  <img src={ch.thumbnailUrl} alt="" className="w-9 h-9 rounded-xl object-cover shrink-0" />
                ) : (
                  <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-primary-light to-primary-subtle shrink-0" />
                )}
                <div className="min-w-0">
                  <div className="text-sm font-bold truncate">{ch.title}</div>
                  {ch.darkhorse && <span className="text-[10px] text-accent font-bold">다크호스</span>}
                </div>
              </div>
              <div className="col-span-2 text-right text-sm text-muted-foreground">{formatCount(ch.subscriberCount)}</div>
              <div className="col-span-2 text-right text-sm text-muted-foreground">{ch.trendingVideoCount}개</div>
              <div className="col-span-2 text-right">
                <span className="text-sm font-mono font-extrabold text-primary">{(ch.surgeScore * 100).toFixed(1)}</span>
              </div>
            </Link>
          );
        })}

        {!channels?.length && !isLoading && (
          <div className="px-7 py-12 text-center text-muted-foreground text-sm">
            데이터 수집 중... 채널 정보가 쌓이면 랭킹이 표시됩니다.
          </div>
        )}
      </div>
    </div>
  );
}
