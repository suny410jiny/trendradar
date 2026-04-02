import { useFilterStore } from '../stores/filterStore';
import { useStatsOverview } from '../hooks/useStats';
import { useBriefing } from '../hooks/useBriefing';
import { useChannelRanking } from '../hooks/useChannels';
import { useKeywordTrending } from '../hooks/useKeywords';
import { Link } from 'react-router-dom';
import { GRADE_CONFIG, COUNTRY_FLAGS } from '../types';
import { formatCount } from '../utils/format';

export default function DashboardPage() {
  const { country } = useFilterStore();
  const { data: stats, isLoading: statsLoading } = useStatsOverview(country);
  const { data: briefing } = useBriefing(country);
  const { data: channels } = useChannelRanking(country, 5);
  const { data: keywords } = useKeywordTrending(country, 'HOUR', 10);

  return (
    <div className="space-y-9" data-cy="dashboard-page">
      {/* Hero: AI Briefing */}
      <section className="relative rounded-[20px] overflow-hidden min-h-[320px] flex bg-gradient-to-br from-primary via-primary-light to-primary">
        <div className="absolute right-[-60px] top-[-60px] w-[420px] h-[420px] rounded-full bg-white/[0.06]" />
        <div className="absolute left-[40%] bottom-[-80px] w-[300px] h-[300px] rounded-full bg-accent/[0.08]" />
        <div className="relative z-10 p-12 flex-1 flex flex-col justify-center">
          <span className="inline-flex items-center gap-1.5 bg-white/12 backdrop-blur-sm border border-white/15 text-white text-[11px] font-bold uppercase tracking-[1.5px] px-4 py-1.5 rounded-3xl mb-5 w-fit">
            ✨ AI 트렌드 브리핑
          </span>
          {briefing?.summary ? (
            <>
              <h1
                className="text-[36px] font-normal italic text-white leading-[1.25] tracking-[-0.5px] mb-4 max-w-[640px]"
                style={{ fontFamily: 'var(--font-serif)' }}
              >
                {briefing.summary.split('.')[0]}.
              </h1>
              <p className="text-sm text-white/75 leading-[1.75] max-w-[560px]">
                {briefing.summary.split('.').slice(1).join('.').trim()}
              </p>
            </>
          ) : (
            <h1
              className="text-[36px] font-normal italic text-white/60 leading-[1.25]"
              style={{ fontFamily: 'var(--font-serif)' }}
            >
              AI 브리핑을 준비하고 있습니다...
            </h1>
          )}
          <div className="mt-6 flex items-center gap-4 flex-wrap">
            <span className="text-[11px] text-white/50 font-semibold flex items-center gap-1">🕐 1시간 전</span>
            <span className="text-[11px] text-white/50 font-semibold flex items-center gap-1">{COUNTRY_FLAGS[country]} {country}</span>
            <span className="text-[11px] text-white/50 font-semibold flex items-center gap-1">✨ Claude AI</span>
          </div>
        </div>
      </section>

      {/* KPI Strip */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <KpiCard label="트렌딩 영상" value={stats?.totalVideos ?? '-'} loading={statsLoading} />
        <KpiCard label="신규 진입" value={stats?.newEntryCount ?? '-'} color="text-success" loading={statsLoading} />
        <KpiCard label="급상승" value={stats?.surgeCount ?? '-'} color="text-destructive" loading={statsLoading} />
        <KpiCard label="평균 참여율" value={stats ? `${stats.avgEngagementRate}%` : '-'} color="text-primary" loading={statsLoading} />
      </div>

      {/* Bento: Channels + Keywords */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* TOP 5 Channels */}
        <div className="lg:col-span-7 bg-card rounded-[20px] shadow-[0_1px_3px_rgba(0,0,0,0.03),0_8px_24px_rgba(0,0,0,0.03)] overflow-hidden">
          <div className="flex items-center justify-between px-7 pt-6">
            <span className="text-xs font-extrabold uppercase tracking-[1.2px] text-foreground">🔥 급부상 채널 TOP 5</span>
            <Link to="/channels" className="text-xs font-bold text-primary-light">전체 랭킹 →</Link>
          </div>
          <div className="px-7 pb-6 pt-4">
            {channels?.map((ch, i) => {
              const gc = GRADE_CONFIG[ch.grade] ?? GRADE_CONFIG['D'];
              return (
                <Link key={ch.channelId} to={`/channels/${ch.channelId}`}
                  className="flex items-center gap-3.5 py-3 border-b border-border/40 last:border-b-0 hover:bg-secondary/50 -mx-3 px-3 rounded-xl transition-colors">
                  <span className={`text-lg font-black w-7 text-right ${i < 3 ? 'text-accent' : 'text-border'}`}
                    style={{ fontFamily: 'var(--font-heading)' }}>
                    {String(i + 1).padStart(2, '0')}
                  </span>
                  {ch.thumbnailUrl ? (
                    <img src={ch.thumbnailUrl} alt="" className="w-10 h-10 rounded-xl object-cover" />
                  ) : (
                    <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary-light to-primary-subtle" />
                  )}
                  <div className="flex-1 min-w-0">
                    <div className="text-sm font-bold truncate">{ch.title}</div>
                    <div className="text-[11px] text-muted-foreground">
                      구독 {formatCount(ch.subscriberCount)} · 트렌딩 {ch.trendingVideoCount}개
                      {ch.darkhorse && <span className="ml-1 text-accent font-bold">다크호스</span>}
                    </div>
                  </div>
                  <span className={`text-[11px] font-extrabold px-2.5 py-1 rounded-lg ${gc.bgColor} ${gc.color}`}>
                    {ch.grade}
                  </span>
                </Link>
              );
            })}
            {!channels?.length && <p className="text-sm text-muted-foreground py-4">데이터 수집 중...</p>}
          </div>
        </div>

        {/* Keywords + Growth */}
        <div className="lg:col-span-5 flex flex-col gap-5">
          {/* Keyword Cloud */}
          <div className="bg-card rounded-[20px] shadow-[0_1px_3px_rgba(0,0,0,0.03),0_8px_24px_rgba(0,0,0,0.03)] p-7">
            <div className="flex items-center justify-between mb-1">
              <span className="text-xs font-extrabold uppercase tracking-[1.2px]">🔑 핫 키워드</span>
              <Link to="/keywords" className="text-xs font-bold text-primary-light">전체 보기 →</Link>
            </div>
            <div className="flex flex-wrap gap-2 mt-4">
              {keywords?.slice(0, 8).map((kw, i) => {
                const colors = [
                  'bg-[#ccfbf1] text-[#134e4a]',
                  'bg-[#ffedd5] text-[#9a3412]',
                  'bg-[#d1fae5] text-[#065f46]',
                  'bg-[#ffdad6] text-[#93000a]',
                  'bg-[#f3e8ff] text-[#6b21a8]',
                  'bg-[#fef9c3] text-[#854d0e]',
                ];
                return (
                  <span key={kw.keyword} className={`inline-flex items-center gap-1.5 px-4 py-2 rounded-xl text-[13px] font-semibold ${colors[i % colors.length]} hover:-translate-y-0.5 hover:shadow-md transition-all cursor-pointer`}>
                    #{kw.keyword}
                    <span className="text-[10px] font-bold opacity-60">{kw.videoCount}</span>
                  </span>
                );
              })}
              {!keywords?.length && <p className="text-sm text-muted-foreground">데이터 수집 중...</p>}
            </div>
          </div>

          {/* Growth Callout */}
          {stats && (
            <div className="bg-card rounded-[20px] shadow-[0_1px_3px_rgba(0,0,0,0.03),0_8px_24px_rgba(0,0,0,0.03)] p-7">
              <div className="text-[10px] text-accent font-extrabold uppercase tracking-[2px] mb-3">Weekly Highlight</div>
              <div className="text-[44px] font-black text-primary tracking-[-2px] leading-none" style={{ fontFamily: 'var(--font-heading)' }}>
                {stats.uniqueVideos24h}
              </div>
              <div className="text-[13px] text-muted-foreground font-medium mt-2">24시간 내 수집된 고유 영상 수</div>
              <div className="h-1.5 rounded-full bg-border mt-4 overflow-hidden">
                <div className="h-full rounded-full bg-gradient-to-r from-primary to-primary-light" style={{ width: `${Math.min((stats.uniqueVideos24h / 500) * 100, 100)}%` }} />
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Pull Quote */}
      {briefing?.summary && (
        <div className="relative bg-secondary rounded-[20px] p-9 overflow-hidden">
          <div className="absolute left-0 top-0 bottom-0 w-1 rounded-full bg-gradient-to-b from-primary-light to-primary-subtle" />
          <p className="text-[22px] italic leading-[1.5] text-primary max-w-[800px]"
            style={{ fontFamily: 'var(--font-serif)' }}>
            "{briefing.summary.split('.').slice(0, 2).join('.').trim()}."
          </p>
          <div className="text-xs text-muted-foreground font-semibold uppercase tracking-[1px] mt-4">
            — TrendRadar AI 인사이트 · {new Date().toLocaleDateString('ko-KR')}
          </div>
        </div>
      )}
    </div>
  );
}

function KpiCard({ label, value, color = 'text-foreground', loading = false }: {
  label: string;
  value: string | number;
  color?: string;
  loading?: boolean;
}) {
  return (
    <div className="bg-card rounded-2xl p-5 shadow-[0_1px_3px_rgba(0,0,0,0.03),0_8px_24px_rgba(0,0,0,0.03)] hover:-translate-y-0.5 hover:shadow-[0_2px_6px_rgba(0,0,0,0.04),0_12px_32px_rgba(0,0,0,0.06)] transition-all">
      <div className="text-[11px] text-muted-foreground font-semibold uppercase tracking-[0.5px] mb-2">{label}</div>
      {loading ? (
        <div className="h-9 w-20 bg-border/50 rounded-lg animate-pulse" />
      ) : (
        <div className={`text-[30px] font-extrabold tracking-[-1.5px] ${color}`} style={{ fontFamily: 'var(--font-heading)' }}>{value}</div>
      )}
    </div>
  );
}
