import {
  RadarChart,
  Radar,
  PolarGrid,
  PolarAngleAxis,
  PolarRadiusAxis,
  ResponsiveContainer,
  Tooltip,
} from 'recharts';
import type { TagStat } from '@/types';

interface Props {
  data: TagStat[] | undefined;
  isLoading: boolean;
}

export default function TagRadarChart({ data, isLoading }: Props) {
  const Skeleton = () => (
    <div className="rounded-xl border border-border bg-card p-5">
      <h3 className="text-foreground font-semibold mb-4">알고리즘 태그 분포</h3>
      <div className="h-72 bg-muted-foreground/10 rounded animate-pulse" />
    </div>
  );

  if (isLoading) {
    return <Skeleton />;
  }

  if (!data || data.length === 0) {
    return (
      <div className="rounded-xl border border-border bg-card p-5" data-cy="tag-radar-chart">
        <h3 className="text-foreground font-semibold mb-4">알고리즘 태그 분포</h3>
        <div className="h-72 flex items-center justify-center text-muted-foreground">
          데이터가 없습니다
        </div>
      </div>
    );
  }

  const chartData = data.map((item) => ({
    ...item,
    value: item.videoCount,
  }));

  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload[0]) {
      const data = payload[0].payload as TagStat & { value: number };
      return (
        <div className="bg-card border border-border rounded p-2 text-xs shadow-lg">
          <p className="text-foreground font-medium">{data.tagLabel}</p>
          <p className="text-muted-foreground">영상: {data.videoCount}개</p>
          <p className="text-muted-foreground">비율: {data.percentage.toFixed(1)}%</p>
        </div>
      );
    }
    return null;
  };

  return (
    <div className="rounded-xl border border-border bg-card p-5" data-cy="tag-radar-chart">
      <h3 className="text-foreground font-semibold mb-4">알고리즘 태그 분포</h3>
      <ResponsiveContainer width="100%" height={280}>
        <RadarChart data={chartData} margin={{ top: 10, right: 30, bottom: 10, left: 30 }}>
          <defs>
            <linearGradient id="tagGradient" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="rgb(59, 130, 246)" stopOpacity={0.8} />
              <stop offset="100%" stopColor="rgb(59, 130, 246)" stopOpacity={0.3} />
            </linearGradient>
          </defs>
          <PolarGrid stroke="oklch(0.50 0.02 260)" opacity={0.3} />
          <PolarAngleAxis
            dataKey="tagLabel"
            tick={{ fontSize: 12, fill: 'oklch(0.50 0.02 260)' }}
          />
          <PolarRadiusAxis
            angle={90}
            domain={[0, 'auto']}
            tick={{ fontSize: 11, fill: 'oklch(0.50 0.02 260)' }}
          />
          <Radar
            name="영상 수"
            dataKey="value"
            stroke="rgb(59, 130, 246)"
            fill="url(#tagGradient)"
            strokeWidth={2}
          />
          <Tooltip content={<CustomTooltip />} />
        </RadarChart>
      </ResponsiveContainer>
    </div>
  );
}
