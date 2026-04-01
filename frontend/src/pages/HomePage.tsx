import CountrySelector from '@/components/common/CountrySelector';
import CategoryFilter from '@/components/common/CategoryFilter';
import AlgorithmTagFilter from '@/components/common/AlgorithmTagFilter';
import BriefingCard from '@/components/common/BriefingCard';
import TrendingList from '@/components/trending/TrendingList';
import StatsOverviewCards from '@/components/dashboard/StatsOverviewCards';
import CategoryDistributionChart from '@/components/dashboard/CategoryDistributionChart';
import TagRadarChart from '@/components/dashboard/TagRadarChart';
import DemographicChart from '@/components/dashboard/DemographicChart';
import TrendingKeywords from '@/components/dashboard/TrendingKeywords';
import { useStatsOverview, useTrendingKeywords } from '@/hooks/useStats';
import { useFilterStore } from '@/stores/filterStore';

export default function HomePage() {
  const { country } = useFilterStore();
  const { data: stats, isLoading: statsLoading } = useStatsOverview(country);
  const { data: keywords, isLoading: keywordsLoading } = useTrendingKeywords(country, 30);

  return (
    <div className="min-h-screen bg-background" data-cy="home-page">
      {/* Header */}
      <header className="sticky top-0 z-10 border-b border-border bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="mx-auto max-w-5xl px-4 py-4">
          <div className="flex items-center gap-3 mb-4">
            <span className="text-2xl">📡</span>
            <div>
              <h1 className="text-xl font-bold text-foreground">TrendRadar</h1>
              <p className="text-xs text-muted-foreground">세상의 트렌드를 레이더처럼 감지하다</p>
            </div>
          </div>

          {/* 국가 선택 */}
          <CountrySelector />

          {/* 카테고리 필터 */}
          <div className="mt-3">
            <CategoryFilter />
          </div>

          {/* 알고리즘 태그 필터 */}
          <div className="mt-2">
            <AlgorithmTagFilter />
          </div>
        </div>
      </header>

      {/* Content */}
      <main className="mx-auto max-w-5xl px-4 py-6 space-y-8">
        {/* 대시보드 섹션 */}
        <section className="space-y-6">
          {/* KPI 카드 */}
          <StatsOverviewCards stats={stats} isLoading={statsLoading} />

          {/* AI 브리핑 */}
          <BriefingCard />

          {/* 트렌딩 키워드 클라우드 */}
          <TrendingKeywords data={keywords} isLoading={keywordsLoading} />

          {/* 2단 그리드: 카테고리 분포 + 태그 레이더 */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <CategoryDistributionChart
              data={stats?.categoryDistribution}
              isLoading={statsLoading}
            />
            <TagRadarChart
              data={stats?.tagDistribution}
              isLoading={statsLoading}
            />
          </div>

          {/* 연령대별 트렌드 분포 (전체 너비) */}
          <DemographicChart
            data={stats?.demographicDistribution}
            isLoading={statsLoading}
          />
        </section>

        {/* 구분선 */}
        <div className="border-t border-border" />

        {/* 트렌딩 리스트 섹션 */}
        <section className="space-y-4">
          <div className="flex items-center gap-3">
            <h2 className="text-xl font-bold text-foreground">실시간 트렌딩 TOP 50</h2>
            {stats && (
              <span className="text-sm px-2 py-1 rounded-full bg-primary/10 text-primary font-medium">
                {stats.totalVideos}개
              </span>
            )}
          </div>
          <TrendingList />
        </section>
      </main>
    </div>
  );
}
