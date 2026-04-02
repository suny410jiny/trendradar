import { useState } from 'react';
import { useFilterStore } from '../stores/filterStore';
import { useOpportunities, useGlobalVsLocal } from '../hooks/useCrossBorder';
import { COUNTRY_FLAGS } from '../types';

type TabType = 'opportunities' | 'propagation' | 'globalLocal';

export default function CrossBorderPage() {
  const { country } = useFilterStore();
  const [tab, setTab] = useState<TabType>('opportunities');

  return (
    <div className="space-y-6" data-cy="crossborder-page">
      <h1 className="text-xl font-bold">크로스보더 트렌드 분석</h1>

      {/* Tab Bar */}
      <div className="flex gap-1 bg-gray-800 rounded-lg p-1 w-fit">
        <TabButton active={tab === 'opportunities'} onClick={() => setTab('opportunities')}>선점 기회</TabButton>
        <TabButton active={tab === 'propagation'} onClick={() => setTab('propagation')}>전파 경로</TabButton>
        <TabButton active={tab === 'globalLocal'} onClick={() => setTab('globalLocal')}>글로벌 vs 로컬</TabButton>
      </div>

      {tab === 'opportunities' && <OpportunitiesView country={country} />}
      {tab === 'globalLocal' && <GlobalLocalView country={country} />}
      {tab === 'propagation' && (
        <div className="bg-gray-900 rounded-xl border border-gray-800 p-6 text-gray-400 text-sm">
          키워드를 선택하면 전파 경로가 표시됩니다. (선점 기회 탭에서 키워드 클릭)
        </div>
      )}
    </div>
  );
}

function TabButton({ active, onClick, children }: { active: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button onClick={onClick} className={`px-4 py-2 text-sm rounded-md transition-colors ${
      active ? 'bg-blue-500 text-white' : 'text-gray-400 hover:text-white'
    }`}>{children}</button>
  );
}

function OpportunitiesView({ country }: { country: string }) {
  const { data: opportunities, isLoading } = useOpportunities(country);

  return (
    <div className="bg-gray-900 rounded-xl border border-gray-800 overflow-hidden">
      <div className="px-4 py-3 bg-gray-800/50">
        <p className="text-xs text-gray-500">다른 나라에서는 뜨고 있지만 {COUNTRY_FLAGS[country] ?? ''} {country}에서는 아직 안 뜬 키워드</p>
      </div>
      {opportunities?.map((opp, i) => (
        <div key={opp.keyword} className="flex items-center gap-4 px-4 py-3 border-t border-gray-800/50">
          <span className="text-sm font-bold text-gray-500 w-6">{i + 1}</span>
          <div className="flex-1">
            <div className="text-sm font-medium">{opp.keyword}</div>
            <div className="text-xs text-gray-500 mt-0.5">
              {opp.trendingCountries.map(c => `${COUNTRY_FLAGS[c] ?? ''} ${c}`).join(', ')}
            </div>
          </div>
          <span className="text-xs text-gray-400">{opp.totalVideoCount}개 영상</span>
          <span className="text-xs bg-green-500/20 text-green-400 px-2 py-0.5 rounded">선점 기회</span>
        </div>
      ))}
      {isLoading && <div className="px-4 py-8 text-center text-gray-600">로딩 중...</div>}
      {!opportunities?.length && !isLoading && (
        <div className="px-4 py-8 text-center text-gray-600">현재 선점 기회가 없거나 데이터 수집 중입니다.</div>
      )}
    </div>
  );
}

function GlobalLocalView({ country }: { country: string }) {
  const { data, isLoading } = useGlobalVsLocal(country);

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
      {/* Global */}
      <div className="bg-gray-900 rounded-xl border border-gray-800 p-6">
        <h3 className="text-sm font-semibold text-green-400 mb-4">글로벌 키워드 (3개국+)</h3>
        <div className="space-y-2">
          {data?.globalKeywords?.map(kw => (
            <div key={kw.keyword} className="flex items-center justify-between py-2">
              <div>
                <span className="text-sm">{kw.keyword}</span>
                <div className="text-xs text-gray-500 mt-0.5">{kw.countries.map(c => COUNTRY_FLAGS[c] ?? c).join(' ')}</div>
              </div>
              <span className="text-xs text-gray-400">{kw.videoCount}개</span>
            </div>
          ))}
          {!data?.globalKeywords?.length && !isLoading && <p className="text-sm text-gray-600">데이터 수집 중...</p>}
        </div>
      </div>

      {/* Local */}
      <div className="bg-gray-900 rounded-xl border border-gray-800 p-6">
        <h3 className="text-sm font-semibold text-yellow-400 mb-4">로컬 키워드 ({COUNTRY_FLAGS[country] ?? ''} {country} 전용)</h3>
        <div className="space-y-2">
          {data?.localKeywords?.map(kw => (
            <div key={kw.keyword} className="flex items-center justify-between py-2">
              <span className="text-sm">{kw.keyword}</span>
              <span className="text-xs text-gray-400">{kw.videoCount}개</span>
            </div>
          ))}
          {!data?.localKeywords?.length && !isLoading && <p className="text-sm text-gray-600">데이터 수집 중...</p>}
        </div>
      </div>
    </div>
  );
}
