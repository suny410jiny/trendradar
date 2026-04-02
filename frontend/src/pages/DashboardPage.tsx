import { useFilterStore } from '../stores/filterStore';
import { useStatsOverview } from '../hooks/useStats';
import { useBriefing } from '../hooks/useBriefing';
import { useChannelRanking } from '../hooks/useChannels';
import { useKeywordTrending } from '../hooks/useKeywords';
import { Link } from 'react-router-dom';
import { GRADE_CONFIG, COUNTRY_FLAGS } from '../types';

export default function DashboardPage() {
  const { country } = useFilterStore();
  const { data: stats, isLoading: statsLoading } = useStatsOverview(country);
  const { data: briefing } = useBriefing(country);
  const { data: channels } = useChannelRanking(country, 5);
  const { data: keywords } = useKeywordTrending(country, 'HOUR', 10);

  return (
    <div className="space-y-6" data-cy="dashboard-page">
      {/* KPI Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <KpiCard label="트렌딩 영상" value={stats?.totalVideos ?? '-'} loading={statsLoading} />
        <KpiCard label="신규 진입" value={stats?.newEntryCount ?? '-'} color="text-green-400" loading={statsLoading} />
        <KpiCard label="급상승" value={stats?.surgeCount ?? '-'} color="text-red-400" loading={statsLoading} />
        <KpiCard label="평균 참여율" value={stats ? `${stats.avgEngagementRate}%` : '-'} color="text-blue-400" loading={statsLoading} />
      </div>

      {/* AI Briefing */}
      {briefing?.summary && (
        <div className="bg-gray-900 rounded-xl border border-gray-800 p-6">
          <div className="flex items-center gap-2 mb-3">
            <span className="text-lg">🤖</span>
            <h2 className="text-sm font-semibold text-gray-400">AI 트렌드 브리핑</h2>
          </div>
          <p className="text-gray-200 leading-relaxed whitespace-pre-line">{briefing.summary}</p>
        </div>
      )}

      {/* 2-column: Channels + Keywords */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* TOP 5 Channels */}
        <div className="bg-gray-900 rounded-xl border border-gray-800 p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-semibold text-gray-400">TOP 5 급부상 채널</h2>
            <Link to="/channels" className="text-xs text-blue-400 hover:text-blue-300">전체 보기 →</Link>
          </div>
          <div className="space-y-3">
            {channels?.map((ch, i) => {
              const gradeConf = GRADE_CONFIG[ch.grade] ?? GRADE_CONFIG['D'];
              return (
                <Link key={ch.channelId} to={`/channels/${ch.channelId}`}
                  className="flex items-center gap-3 p-2 rounded-lg hover:bg-gray-800 transition-colors">
                  <span className="text-lg font-bold text-gray-500 w-6">{i + 1}</span>
                  <span className={`text-sm font-bold px-2 py-0.5 rounded ${gradeConf.bgColor} ${gradeConf.color}`}>
                    {ch.grade}
                  </span>
                  <div className="flex-1 min-w-0">
                    <div className="text-sm font-medium truncate">{ch.title}</div>
                    <div className="text-xs text-gray-500">구독 {formatCount(ch.subscriberCount)}</div>
                  </div>
                  {ch.darkhorse && <span className="text-xs bg-yellow-500/20 text-yellow-400 px-2 py-0.5 rounded">다크호스</span>}
                </Link>
              );
            })}
            {!channels?.length && <p className="text-sm text-gray-600">데이터 수집 중...</p>}
          </div>
        </div>

        {/* TOP 10 Keywords */}
        <div className="bg-gray-900 rounded-xl border border-gray-800 p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-semibold text-gray-400">TOP 10 키워드</h2>
            <Link to="/keywords" className="text-xs text-blue-400 hover:text-blue-300">전체 보기 →</Link>
          </div>
          <div className="space-y-2">
            {keywords?.map((kw, i) => (
              <div key={kw.keyword} className="flex items-center gap-3 p-2">
                <span className="text-sm font-bold text-gray-500 w-6">{i + 1}</span>
                <div className="flex-1">
                  <span className="text-sm">{kw.keyword}</span>
                </div>
                <span className="text-xs text-gray-500">{kw.videoCount}개 영상</span>
                <span className="text-xs text-gray-400">{formatCount(kw.totalViews)} 조회</span>
              </div>
            ))}
            {!keywords?.length && <p className="text-sm text-gray-600">데이터 수집 중...</p>}
          </div>
        </div>
      </div>
    </div>
  );
}

function KpiCard({ label, value, color = 'text-white', loading = false }: {
  label: string;
  value: string | number;
  color?: string;
  loading?: boolean;
}) {
  return (
    <div className="bg-gray-900 rounded-xl border border-gray-800 p-4">
      <div className="text-xs text-gray-500 mb-1">{label}</div>
      {loading ? (
        <div className="h-8 w-16 bg-gray-800 rounded animate-pulse" />
      ) : (
        <div className={`text-2xl font-bold ${color}`}>{value}</div>
      )}
    </div>
  );
}

function formatCount(n: number): string {
  if (n >= 1_000_000_000) return `${(n / 1_000_000_000).toFixed(1)}B`;
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
  if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
  return String(n);
}
