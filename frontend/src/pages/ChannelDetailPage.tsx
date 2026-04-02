import { useParams, Link } from 'react-router-dom';
import { useChannelDetail, useChannelSnapshots } from '../hooks/useChannels';
import { GRADE_CONFIG } from '../types';
import { formatCount } from '../utils/format';
import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';

export default function ChannelDetailPage() {
  const { channelId } = useParams();
  const { data: channel, isLoading } = useChannelDetail(channelId);
  const { data: snapshots } = useChannelSnapshots(channelId);

  if (isLoading) return <div className="text-muted-foreground text-sm">로딩 중...</div>;
  if (!channel) return <div className="text-muted-foreground text-sm">채널을 찾을 수 없습니다.</div>;

  const gradeConfig = GRADE_CONFIG[channel.grade] ?? GRADE_CONFIG['D'];

  const chartData = snapshots?.map(s => ({
    date: new Date(s.snapshotAt).toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' }),
    subscribers: s.subscriberCount,
    views: s.totalViewCount,
  })).reverse() ?? [];

  return (
    <div className="space-y-6" data-cy="channel-detail-page">
      <Link to="/channels" className="text-sm text-muted-foreground hover:text-foreground transition-colors">← 채널 랭킹</Link>

      <div className="bg-card rounded-[20px] shadow-[0_1px_3px_rgba(0,0,0,0.03),0_8px_24px_rgba(0,0,0,0.03)] p-8">
        <div className="flex items-start gap-5">
          {channel.thumbnailUrl ? (
            <img src={channel.thumbnailUrl} alt="" className="w-16 h-16 rounded-2xl object-cover" />
          ) : (
            <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-primary-light to-primary-subtle" />
          )}
          <div className="flex-1">
            <div className="flex items-center gap-3 mb-2 flex-wrap">
              <h1 className="text-xl font-extrabold tracking-tight" style={{ fontFamily: 'var(--font-heading)' }}>{channel.title}</h1>
              <span className={`text-base font-extrabold px-3 py-1 rounded-lg ${gradeConfig.bgColor} ${gradeConfig.color}`}>
                {channel.grade}
              </span>
              {channel.darkhorse && <span className="text-sm bg-accent/15 text-accent font-bold px-3 py-1 rounded-lg">다크호스</span>}
            </div>
            <p className="text-sm text-muted-foreground">{channel.gradeLabel}</p>
          </div>
        </div>

        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mt-7">
          <MetricBox label="구독자" value={formatCount(channel.subscriberCount)} />
          <MetricBox label="총 영상" value={formatCount(channel.videoCount)} />
          <MetricBox label="총 조회수" value={formatCount(channel.totalViewCount)} />
          <MetricBox label="급부상 스코어" value={(channel.surgeScore * 100).toFixed(1)} color="text-primary" />
        </div>
      </div>

      {chartData.length > 0 && (
        <div className="bg-card rounded-[20px] shadow-[0_1px_3px_rgba(0,0,0,0.03),0_8px_24px_rgba(0,0,0,0.03)] p-8">
          <h2 className="text-xs font-extrabold uppercase tracking-[1.2px] text-muted-foreground mb-5">구독자 추이</h2>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={chartData}>
                <XAxis dataKey="date" tick={{ fontSize: 12, fill: '#737782' }} />
                <YAxis tick={{ fontSize: 12, fill: '#737782' }} />
                <Tooltip contentStyle={{ backgroundColor: '#ffffff', border: '1px solid #e4e2dd', borderRadius: '12px', boxShadow: '0 4px 12px rgba(0,0,0,0.08)' }}
                         labelStyle={{ color: '#737782' }} />
                <Line type="monotone" dataKey="subscribers" stroke="#0f766e" strokeWidth={2.5} dot={false} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}

      {channel.aiAnalysis && (
        <div className="bg-card rounded-[20px] shadow-[0_1px_3px_rgba(0,0,0,0.03),0_8px_24px_rgba(0,0,0,0.03)] p-8">
          <div className="flex items-center gap-2 mb-5">
            <span>🤖</span>
            <h2 className="text-xs font-extrabold uppercase tracking-[1.2px] text-muted-foreground">AI 채널 분석</h2>
            {channel.aiAnalysis.fromCache && <span className="text-[10px] text-muted-foreground/50">(캐시)</span>}
          </div>
          <div
            className="text-[17px] leading-[1.8] text-foreground/85"
            style={{ fontFamily: 'var(--font-serif)' }}
          >
            <span className="float-left text-[4.2rem] leading-none pr-3 font-light text-primary" style={{ fontFamily: 'var(--font-serif)' }}>
              {channel.aiAnalysis.content.charAt(0)}
            </span>
            {channel.aiAnalysis.content.slice(1)}
          </div>
        </div>
      )}
    </div>
  );
}

function MetricBox({ label, value, color = 'text-foreground' }: { label: string; value: string; color?: string }) {
  return (
    <div className="bg-secondary/60 rounded-2xl p-4">
      <div className="text-[11px] text-muted-foreground font-semibold uppercase tracking-[0.5px]">{label}</div>
      <div className={`text-lg font-extrabold ${color}`} style={{ fontFamily: 'var(--font-heading)' }}>{value}</div>
    </div>
  );
}
