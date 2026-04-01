import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import type { ViewSnapshot } from '@/types';

interface Props {
  snapshots: ViewSnapshot[];
}

function formatDate(dateStr: string): string {
  const d = new Date(dateStr);
  return `${d.getMonth() + 1}/${d.getDate()}`;
}

function formatViewCount(n: number): string {
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
  if (n >= 1_000) return `${(n / 1_000).toFixed(0)}K`;
  return n.toString();
}

export default function ViewTrendChart({ snapshots }: Props) {
  const data = snapshots.map((s) => ({
    date: formatDate(s.snapshotAt),
    viewCount: s.viewCount,
  }));

  return (
    <div className="rounded-xl border border-border bg-card p-6" data-cy="view-trend-chart">
      <h3 className="text-sm font-semibold text-foreground mb-4">조회수 추이 (7일)</h3>
      <ResponsiveContainer width="100%" height={280}>
        <LineChart data={data}>
          <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" />
          <XAxis dataKey="date" tick={{ fontSize: 12 }} stroke="hsl(var(--muted-foreground))" />
          <YAxis tickFormatter={formatViewCount} tick={{ fontSize: 12 }} stroke="hsl(var(--muted-foreground))" />
          <Tooltip
            formatter={(value) => [formatViewCount(Number(value)), '조회수']}
            contentStyle={{ borderRadius: '8px', border: '1px solid hsl(var(--border))' }}
          />
          <Line
            type="monotone"
            dataKey="viewCount"
            stroke="hsl(var(--primary))"
            strokeWidth={2}
            dot={{ r: 4 }}
            activeDot={{ r: 6 }}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
