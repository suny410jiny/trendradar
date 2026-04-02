import { useState } from 'react';
import { useFilterStore } from '../stores/filterStore';
import { useKeywordTrending } from '../hooks/useKeywords';
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
        <h1 className="text-xl font-bold">키워드 트렌드</h1>
        <div className="flex gap-1 bg-gray-800 rounded-lg p-1">
          {PERIODS.map(p => (
            <button key={p.value} onClick={() => setPeriod(p.value)}
              className={`px-3 py-1.5 text-xs rounded-md transition-colors ${
                period === p.value ? 'bg-blue-500 text-white' : 'text-gray-400 hover:text-white'
              }`}>
              {p.label}
            </button>
          ))}
        </div>
      </div>

      {/* Bar Chart */}
      {chartData.length > 0 && (
        <div className="bg-gray-900 rounded-xl border border-gray-800 p-6">
          <h2 className="text-sm text-gray-400 mb-4">키워드별 영상 수</h2>
          <div className="h-80">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData} layout="vertical">
                <XAxis type="number" tick={{ fontSize: 12, fill: '#6b7280' }} />
                <YAxis type="category" dataKey="keyword" width={100} tick={{ fontSize: 12, fill: '#9ca3af' }} />
                <Tooltip contentStyle={{ backgroundColor: '#1f2937', border: 'none', borderRadius: '8px' }}
                         labelStyle={{ color: '#9ca3af' }} />
                <Bar dataKey="count" fill="#3b82f6" radius={[0, 4, 4, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}

      {/* Keyword List */}
      <div className="bg-gray-900 rounded-xl border border-gray-800 overflow-hidden">
        <div className="grid grid-cols-12 gap-2 px-4 py-3 bg-gray-800/50 text-xs text-gray-500 font-medium">
          <div className="col-span-1">#</div>
          <div className="col-span-5">키워드</div>
          <div className="col-span-2 text-right">영상 수</div>
          <div className="col-span-2 text-right">총 조회수</div>
          <div className="col-span-2 text-right">참여율</div>
        </div>
        {keywords?.map((kw, i) => (
          <div key={kw.keyword} className="grid grid-cols-12 gap-2 px-4 py-3 items-center border-t border-gray-800/50">
            <div className="col-span-1 text-sm font-bold text-gray-500">{i + 1}</div>
            <div className="col-span-5 text-sm">{kw.keyword}</div>
            <div className="col-span-2 text-right text-sm text-gray-400">{kw.videoCount}</div>
            <div className="col-span-2 text-right text-sm text-gray-400">{formatCount(kw.totalViews)}</div>
            <div className="col-span-2 text-right text-sm text-gray-400">{(kw.avgEngagement * 100).toFixed(1)}%</div>
          </div>
        ))}
        {isLoading && <div className="px-4 py-8 text-center text-gray-600">로딩 중...</div>}
        {!keywords?.length && !isLoading && <div className="px-4 py-8 text-center text-gray-600">데이터 수집 중...</div>}
      </div>
    </div>
  );
}

function formatCount(n: number): string {
  if (n >= 1_000_000_000) return `${(n / 1_000_000_000).toFixed(1)}B`;
  if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
  if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
  return String(n);
}
