import { useState } from 'react';
import { useFilterStore } from '../stores/filterStore';
import { useKeywordTrending } from '../hooks/useKeywords';
import { formatCount } from '../utils/format';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';

const PERIODS = [
  { value: 'HOUR', label: '시간별' },
  { value: 'DAY', label: '일별' },
  { value: 'WEEK', label: '주별' },
  { value: 'MONTH', label: '월별' },
] as const;

export default function KeywordsPage() {
  const { country } = useFilterStore();
  const [period, setPeriod] = useState('HOUR');
  const { data: keywords, isLoading } = useKeywordTrending(country, period, 30);

  const chartData = keywords?.slice(0, 15).map(kw => ({
    keyword: kw.keyword.length > 10 ? kw.keyword.slice(0, 10) + '...' : kw.keyword,
    count: kw.videoCount,
    views: kw.totalViews,
  })) ?? [];

  return (
    <div className="space-y-6" data-cy="keywords-page">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-extrabold tracking-tight" style={{ fontFamily: 'var(--font-heading)' }}>키워드 트렌드</h1>
        <div className="flex gap-0.5 bg-secondary rounded-2xl p-1">
          {PERIODS.map(p => (
            <button key={p.value} onClick={() => setPeriod(p.value)}
              className={`px-4 py-2 text-xs font-semibold rounded-xl transition-colors ${
                period === p.value ? 'bg-primary text-primary-foreground' : 'text-muted-foreground hover:text-foreground'
              }`}>
              {p.label}
            </button>
          ))}
        </div>
      </div>

      {chartData.length > 0 && (
        <div className="bg-card rounded-[20px] shadow-[0_1px_3px_rgba(0,0,0,0.03),0_8px_24px_rgba(0,0,0,0.03)] p-8">
          <h2 className="text-xs font-extrabold uppercase tracking-[1.2px] text-muted-foreground mb-5">키워드별 영상 수</h2>
          <div className="h-80">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData} layout="vertical">
                <XAxis type="number" tick={{ fontSize: 12, fill: '#737782' }} />
                <YAxis type="category" dataKey="keyword" width={100} tick={{ fontSize: 12, fill: '#134e4a' }} />
                <Tooltip contentStyle={{ backgroundColor: '#ffffff', border: '1px solid #e4e2dd', borderRadius: '12px', boxShadow: '0 4px 12px rgba(0,0,0,0.08)' }}
                         labelStyle={{ color: '#737782' }} />
                <Bar dataKey="count" fill="url(#tealGradient)" radius={[0, 8, 8, 0]} />
                <defs>
                  <linearGradient id="tealGradient" x1="0" y1="0" x2="1" y2="0">
                    <stop offset="0%" stopColor="#134e4a" />
                    <stop offset="100%" stopColor="#0f766e" />
                  </linearGradient>
                </defs>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}

      <div className="bg-card rounded-[20px] shadow-[0_1px_3px_rgba(0,0,0,0.03),0_8px_24px_rgba(0,0,0,0.03)] overflow-hidden">
        <div className="grid grid-cols-12 gap-2 px-7 py-3.5 bg-secondary text-xs text-muted-foreground font-semibold uppercase tracking-[0.5px]">
          <div className="col-span-1">#</div>
          <div className="col-span-5">키워드</div>
          <div className="col-span-2 text-right">영상 수</div>
          <div className="col-span-2 text-right">총 조회수</div>
          <div className="col-span-2 text-right">참여율</div>
        </div>
        {keywords?.map((kw, i) => (
          <div key={kw.keyword} className="grid grid-cols-12 gap-2 px-7 py-3.5 items-center border-t border-border/40 hover:bg-secondary/50 transition-colors">
            <div className={`col-span-1 text-sm font-black ${i < 3 ? 'text-accent' : 'text-border'}`}
              style={{ fontFamily: 'var(--font-heading)' }}>
              {String(i + 1).padStart(2, '0')}
            </div>
            <div className="col-span-5 text-sm font-semibold">{kw.keyword}</div>
            <div className="col-span-2 text-right text-sm text-muted-foreground">{kw.videoCount}</div>
            <div className="col-span-2 text-right text-sm text-muted-foreground">{formatCount(kw.totalViews)}</div>
            <div className="col-span-2 text-right text-sm text-muted-foreground">{(kw.avgEngagement * 100).toFixed(1)}%</div>
          </div>
        ))}
        {isLoading && <div className="px-7 py-12 text-center text-muted-foreground text-sm">로딩 중...</div>}
        {!keywords?.length && !isLoading && <div className="px-7 py-12 text-center text-muted-foreground text-sm">데이터 수집 중...</div>}
      </div>
    </div>
  );
}
