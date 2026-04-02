import { useParams, Link } from 'react-router-dom';
import { useChannelDetail, useChannelSnapshots } from '../hooks/useChannels';
import { GRADE_CONFIG } from '../types';
import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';

export default function ChannelDetailPage() {
  const { channelId } = useParams();
  const { data: channel, isLoading } = useChannelDetail(channelId);
  const { data: snapshots } = useChannelSnapshots(channelId);

  if (isLoading) return <div className="text-gray-500">로딩 중...</div>;
  if (!channel) return <div className="text-gray-500">채널을 찾을 수 없습니다.</div>;

  const gradeConfig = GRADE_CONFIG[channel.grade] ?? GRADE_CONFIG['D'];

  // Chart data from snapshots
  const chartData = snapshots?.map(s => ({
    date: new Date(s.snapshotAt).toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' }),
    subscribers: s.subscriberCount,
    views: s.totalViewCount,
  })).reverse() ?? [];

  return (
    <div className="space-y-6" data-cy="channel-detail-page">
      <Link to="/channels" className="text-sm text-gray-500 hover:text-gray-300">← 채널 랭킹</Link>

      {/* Channel Header */}
      <div className="bg-gray-900 rounded-xl border border-gray-800 p-6">
        <div className="flex items-start gap-4">
          {channel.thumbnailUrl && <img src={channel.thumbnailUrl} alt="" className="w-16 h-16 rounded-full" />}
          <div className="flex-1">
            <div className="flex items-center gap-3 mb-2 flex-wrap">
              <h1 className="text-xl font-bold">{channel.title}</h1>
              <span className={`text-lg font-bold px-3 py-1 rounded ${gradeConfig.bgColor} ${gradeConfig.color}`}>
                {channel.grade}
              </span>
              {channel.darkhorse && <span className="text-sm bg-yellow-500/20 text-yellow-400 px-3 py-1 rounded">다크호스</span>}
            </div>
            <p className="text-sm text-gray-400">{channel.gradeLabel}</p>
          </div>
        </div>

        {/* Metrics */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mt-6">
          <MetricBox label="구독자" value={formatCount(channel.subscriberCount)} />
          <MetricBox label="총 영상" value={formatCount(channel.videoCount)} />
          <MetricBox label="총 조회수" value={formatCount(channel.totalViewCount)} />
          <MetricBox label="급부상 스코어" value={(channel.surgeScore * 100).toFixed(1)} color="text-blue-400" />
        </div>
      </div>

      {/* Growth Chart */}
      {chartData.length > 0 && (
        <div className="bg-gray-900 rounded-xl border border-gray-800 p-6">
          <h2 className="text-sm font-semibold text-gray-400 mb-4">구독자 추이</h2>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={chartData}>
                <XAxis dataKey="date" tick={{ fontSize: 12, fill: '#6b7280' }} />
                <YAxis tick={{ fontSize: 12, fill: '#6b7280' }} />
                <Tooltip contentStyle={{ backgroundColor: '#1f2937', border: 'none', borderRadius: '8px' }}
                         labelStyle={{ color: '#9ca3af' }} />
                <Line type="monotone" dataKey="subscribers" stroke="#3b82f6" strokeWidth={2} dot={false} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}

      {/* AI Analysis */}
      {channel.aiAnalysis && (
        <div className="bg-gray-900 rounded-xl border border-gray-800 p-6">
          <div className="flex items-center gap-2 mb-3">
            <span>🤖</span>
            <h2 className="text-sm font-semibold text-gray-400">AI 채널 분석</h2>
            {channel.aiAnalysis.fromCache && <span className="text-xs text-gray-600">(캐시)</span>}
          </div>
          <p className="text-gray-200 leading-relaxed whitespace-pre-line">{channel.aiAnalysis.content}</p>
        </div>
      )}
    </div>
  );
}

function MetricBox({ label, value, color = 'text-white' }: { label: string; value: string; color?: string }) {
  return (
    <div className="bg-gray-800/50 rounded-lg p-3">
      <div className="text-xs text-gray-500">{label}</div>
      <div className={`text-lg font-bold ${color}`}>{value}</div>
    </div>
  );
}

function formatCount(n: number): string {
  if (n >= 1_000_000_000) return `${(n / 1_000_000_000).toFixed(1)}B`;
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
  if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
  return String(n);
}
