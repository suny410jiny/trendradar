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
import type { CategoryStat } from '@/types';

interface Props {
  data: CategoryStat[] | undefined;
  isLoading: boolean;
}

export default function CategoryDistributionChart({ data, isLoading }: Props) {
  const Skeleton = () => (
    <div className="rounded-xl border border-border bg-card p-5">
      <h3 className="text-foreground font-semibold mb-4">카테고리 분포</h3>
      <div className="h-72 bg-muted-foreground/10 rounded animate-pulse" />
    </div>
  );

  if (isLoading) {
    return <Skeleton />;
  }

  if (!data || data.length === 0) {
    return (
      <div className="rounded-xl border border-border bg-card p-5" data-cy="category-chart">
        <h3 className="text-foreground font-semibold mb-4">카테고리 분포</h3>
        <div className="h-72 flex items-center justify-center text-muted-foreground">
          데이터가 없습니다
        </div>
      </div>
    );
  }

  // Sort by videoCount descending
  const sortedData = [...data].sort((a, b) => b.videoCount - a.videoCount);

  // Gradient colors from blue to purple
  const colors = sortedData.map((_, i) => {
    const ratio = i / Math.max(sortedData.length - 1, 1);
    // Interpolate from blue (#3b82f6) to purple (#a855f7)
    const h = 240 + ratio * 60; // 240 (blue) to 300 (purple)
    const s = 100 - ratio * 10; // 100% to 90%
    const l = 50 + ratio * 5; // 50% to 55%
    return `hsl(${h}, ${s}%, ${l}%)`;
  });

  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload[0]) {
      const data = payload[0].payload as CategoryStat;
      return (
        <div className="bg-card border border-border rounded p-2 text-xs shadow-lg">
          <p className="text-foreground font-medium">{data.categoryName}</p>
          <p className="text-muted-foreground">영상: {data.videoCount}개</p>
          <p className="text-muted-foreground">비율: {data.percentage.toFixed(1)}%</p>
          <p className="text-muted-foreground">평균: {Math.round(data.avgViews).toLocaleString()}회</p>
        </div>
      );
    }
    return null;
  };

  return (
    <div className="rounded-xl border border-border bg-card p-5" data-cy="category-chart">
      <h3 className="text-foreground font-semibold mb-4">카테고리 분포</h3>
      <ResponsiveContainer width="100%" height={280}>
        <BarChart
          layout="vertical"
          data={sortedData}
          margin={{ top: 5, right: 30, left: 150, bottom: 5 }}
        >
          <defs>
            {colors.map((color, i) => (
              <linearGradient key={`grad-${i}`} id={`gradient-${i}`} x1="0" x2="1" y1="0" y2="0">
                <stop offset="0%" stopColor={color} stopOpacity={0.6} />
                <stop offset="100%" stopColor={color} stopOpacity={1} />
              </linearGradient>
            ))}
          </defs>
          <CartesianGrid strokeDasharray="3 3" stroke="oklch(0.50 0.02 260)" opacity={0.2} />
          <XAxis type="number" stroke="oklch(0.50 0.02 260)" tick={{ fontSize: 11, fill: 'oklch(0.50 0.02 260)' }} />
          <YAxis
            dataKey="categoryName"
            type="category"
            width={140}
            tick={{ fontSize: 12, fill: 'oklch(0.50 0.02 260)' }}
          />
          <Tooltip content={<CustomTooltip />} />
          <Bar dataKey="videoCount" radius={[0, 8, 8, 0]} label={{ position: 'right', fontSize: 11, fill: 'oklch(0.50 0.02 260)', formatter: (v) => v ?? '' }}>
            {sortedData.map((_, i) => (
              <Cell key={`cell-${i}`} fill={`url(#gradient-${i})`} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
