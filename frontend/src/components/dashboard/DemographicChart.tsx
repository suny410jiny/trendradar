import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from 'recharts';
import type { DemographicStat } from '@/types';

interface Props {
  data: DemographicStat[] | undefined;
  isLoading: boolean;
}

export default function DemographicChart({ data, isLoading }: Props) {
  const Skeleton = () => (
    <div className="rounded-xl border border-border bg-card p-5">
      <h3 className="text-foreground font-semibold mb-4">연령대별 트렌드 분포</h3>
      <div className="h-80 bg-muted-foreground/10 rounded animate-pulse mb-4" />
      <div className="flex gap-2">
        {[1, 2, 3, 4].map((i) => (
          <div key={i} className="h-8 flex-1 bg-muted-foreground/10 rounded animate-pulse" />
        ))}
      </div>
    </div>
  );

  if (isLoading) {
    return <Skeleton />;
  }

  if (!data || data.length === 0) {
    return (
      <div className="rounded-xl border border-border bg-card p-5" data-cy="demographic-chart">
        <h3 className="text-foreground font-semibold mb-4">연령대별 트렌드 분포</h3>
        <div className="h-80 flex items-center justify-center text-muted-foreground">
          데이터가 없습니다
        </div>
      </div>
    );
  }

  const ageGroupColors: Record<string, string> = {
    '10대': '#22d3ee', // cyan
    '20대': '#8b5cf6', // violet
    '30대': '#f59e0b', // amber
    '40대+': '#6b7280', // gray
  };

  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload[0]) {
      const data = payload[0].payload as DemographicStat;
      return (
        <div className="bg-card border border-border rounded p-2 text-xs shadow-lg">
          <p className="text-foreground font-medium">{data.ageGroup}</p>
          <p className="text-muted-foreground">영상: {data.videoCount}개</p>
          <p className="text-muted-foreground">비율: {data.percentage.toFixed(1)}%</p>
          {data.topCategories.length > 0 && (
            <p className="text-muted-foreground mt-1">주요: {data.topCategories.join(', ')}</p>
          )}
        </div>
      );
    }
    return null;
  };

  const getAgeGroupColor = (ageGroup: string): string => {
    return ageGroupColors[ageGroup] || '#6b7280';
  };

  return (
    <div className="rounded-xl border border-border bg-card p-5" data-cy="demographic-chart">
      <h3 className="text-foreground font-semibold mb-4">연령대별 트렌드 분포</h3>
      <ResponsiveContainer width="100%" height={300}>
        <BarChart
          data={data}
          margin={{ top: 10, right: 30, left: 30, bottom: 50 }}
        >
          <CartesianGrid strokeDasharray="3 3" stroke="oklch(0.50 0.02 260)" opacity={0.2} />
          <XAxis
            dataKey="ageGroup"
            tick={{ fontSize: 13, fill: 'oklch(0.50 0.02 260)' }}
          />
          <YAxis
            label={{ value: '영상 수', angle: -90, position: 'insideLeft', fill: 'oklch(0.50 0.02 260)' }}
            tick={{ fontSize: 12, fill: 'oklch(0.50 0.02 260)' }}
          />
          <Tooltip content={<CustomTooltip />} />
          <Bar dataKey="videoCount" fill="#8b5cf6" radius={[8, 8, 0, 0]}>
            {data.map((item, index) => (
              <Cell key={`cell-${index}`} fill={getAgeGroupColor(item.ageGroup)} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>

      {/* Age group pills with top categories */}
      <div className="mt-6 grid grid-cols-2 md:grid-cols-4 gap-3">
        {data.map((item) => (
          <div
            key={item.ageGroup}
            className="rounded-lg border border-border bg-muted/50 p-3"
            data-cy={`demographic-pill-${item.ageGroup}`}
          >
            <div className="flex items-center gap-2 mb-2">
              <div
                className="w-3 h-3 rounded-full"
                style={{ backgroundColor: getAgeGroupColor(item.ageGroup) }}
              />
              <span className="text-sm font-semibold text-foreground">{item.ageGroup}</span>
            </div>
            <div className="text-xs text-muted-foreground space-y-1">
              <p>{item.videoCount}개 영상 ({item.percentage.toFixed(0)}%)</p>
              {item.topCategories.length > 0 && (
                <p className="text-xs">주요: {item.topCategories.slice(0, 2).join(', ')}</p>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
