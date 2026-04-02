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
      <h1 className="text-xl font-extrabold tracking-tight" style={{ fontFamily: 'var(--font-heading)' }}>
        크로스보더 트렌드 분석
      </h1>

      <div className="flex gap-0.5 bg-secondary rounded-2xl p-1 w-fit">
        <TabButton active={tab === 'opportunities'} onClick={() => setTab('opportunities')}>선점 기회</TabButton>
        <TabButton active={tab === 'propagation'} onClick={() => setTab('propagation')}>전파 경로</TabButton>
        <TabButton active={tab === 'globalLocal'} onClick={() => setTab('globalLocal')}>글로벌 vs 로컬</TabButton>
      </div>

      {tab === 'opportunities' && <OpportunitiesView country={country} />}
      {tab === 'globalLocal' && <GlobalLocalView country={country} />}
      {tab === 'propagation' && (
        <div className="bg-card rounded-[20px] shadow-[0_1px_3px_rgba(0,0,0,0.03),0_8px_24px_rgba(0,0,0,0.03)] p-8 text-muted-foreground text-sm">
          키워드를 선택하면 전파 경로가 표시됩니다. (선점 기회 탭에서 키워드 클릭)
        </div>
      )}
    </div>
  );
}

function TabButton({ active, onClick, children }: { active: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button onClick={onClick} className={`px-5 py-2 text-sm font-semibold rounded-xl transition-colors ${
      active ? 'bg-primary text-primary-foreground' : 'text-muted-foreground hover:text-foreground'
    }`}>{children}</button>
  );
}

function OpportunitiesView({ country }: { country: string }) {
  const { data: opportunities, isLoading } = useOpportunities(country);

  return (
    <div className="bg-card rounded-[20px] shadow-[0_1px_3px_rgba(0,0,0,0.03),0_8px_24px_rgba(0,0,0,0.03)] overflow-hidden">
      <div className="px-7 py-4 bg-secondary">
        <p className="text-xs text-muted-foreground font-medium">다른 나라에서는 뜨고 있지만 {COUNTRY_FLAGS[country] ?? ''} {country}에서는 아직 안 뜬 키워드</p>
      </div>
      {opportunities?.map((opp, i) => (
        <div key={opp.keyword} className="flex items-center gap-4 px-7 py-4 border-t border-border/40 hover:bg-secondary/50 transition-colors">
          <span className={`text-sm font-black w-6 text-right ${i < 3 ? 'text-accent' : 'text-border'}`}
            style={{ fontFamily: 'var(--font-heading)' }}>
            {String(i + 1).padStart(2, '0')}
          </span>
          <div className="flex-1">
            <div className="text-sm font-bold">{opp.keyword}</div>
            <div className="text-[11px] text-muted-foreground mt-0.5">
              {opp.trendingCountries.map(c => `${COUNTRY_FLAGS[c] ?? ''} ${c}`).join(', ')}
            </div>
          </div>
          <span className="text-xs text-muted-foreground">{opp.totalVideoCount}개 영상</span>
          <span className="text-[11px] bg-success/10 text-success font-bold px-3 py-1 rounded-lg">선점 기회</span>
        </div>
      ))}
      {isLoading && <div className="px-7 py-12 text-center text-muted-foreground text-sm">로딩 중...</div>}
      {!opportunities?.length && !isLoading && (
        <div className="px-7 py-12 text-center text-muted-foreground text-sm">현재 선점 기회가 없거나 데이터 수집 중입니다.</div>
      )}
    </div>
  );
}

function GlobalLocalView({ country }: { country: string }) {
  const { data, isLoading } = useGlobalVsLocal(country);

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <div className="bg-card rounded-[20px] shadow-[0_1px_3px_rgba(0,0,0,0.03),0_8px_24px_rgba(0,0,0,0.03)] p-7">
        <h3 className="text-sm font-extrabold text-primary-light mb-5">🌍 글로벌 키워드 (3개국+)</h3>
        <div className="space-y-2">
          {data?.globalKeywords?.map(kw => (
            <div key={kw.keyword} className="flex items-center justify-between py-2.5 border-b border-border/30 last:border-b-0">
              <div>
                <span className="text-sm font-semibold">{kw.keyword}</span>
                <div className="text-[11px] text-muted-foreground mt-0.5">{kw.countries.map(c => COUNTRY_FLAGS[c] ?? c).join(' ')}</div>
              </div>
              <span className="text-xs text-muted-foreground font-medium">{kw.videoCount}개</span>
            </div>
          ))}
          {!data?.globalKeywords?.length && !isLoading && <p className="text-sm text-muted-foreground">데이터 수집 중...</p>}
        </div>
      </div>

      <div className="bg-card rounded-[20px] shadow-[0_1px_3px_rgba(0,0,0,0.03),0_8px_24px_rgba(0,0,0,0.03)] p-7">
        <h3 className="text-sm font-extrabold text-accent mb-5">📍 로컬 키워드 ({COUNTRY_FLAGS[country] ?? ''} {country} 전용)</h3>
        <div className="space-y-2">
          {data?.localKeywords?.map(kw => (
            <div key={kw.keyword} className="flex items-center justify-between py-2.5 border-b border-border/30 last:border-b-0">
              <span className="text-sm font-semibold">{kw.keyword}</span>
              <span className="text-xs text-muted-foreground font-medium">{kw.videoCount}개</span>
            </div>
          ))}
          {!data?.localKeywords?.length && !isLoading && <p className="text-sm text-muted-foreground">데이터 수집 중...</p>}
        </div>
      </div>
    </div>
  );
}
