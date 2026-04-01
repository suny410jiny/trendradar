import CountrySelector from '@/components/common/CountrySelector';
import CategoryFilter from '@/components/common/CategoryFilter';
import BriefingCard from '@/components/common/BriefingCard';
import TrendingList from '@/components/trending/TrendingList';

export default function HomePage() {
  return (
    <div className="min-h-screen bg-background" data-cy="home-page">
      {/* Header */}
      <header className="sticky top-0 z-10 border-b border-border bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="mx-auto max-w-3xl px-4 py-4">
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
        </div>
      </header>

      {/* Content */}
      <main className="mx-auto max-w-3xl px-4 py-6 space-y-6">
        {/* AI 브리핑 */}
        <BriefingCard />

        {/* 트렌딩 리스트 */}
        <TrendingList />
      </main>
    </div>
  );
}
