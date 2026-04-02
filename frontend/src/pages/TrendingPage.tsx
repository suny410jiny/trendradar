import CategoryFilter from '../components/common/CategoryFilter';
import AlgorithmTagFilter from '../components/common/AlgorithmTagFilter';
import TrendingList from '../components/trending/TrendingList';

export default function TrendingPage() {
  return (
    <div className="space-y-4" data-cy="trending-page">
      <h1 className="text-xl font-bold">트렌딩 영상 TOP 50</h1>
      <div className="flex flex-wrap gap-4">
        <CategoryFilter />
        <AlgorithmTagFilter />
      </div>
      <TrendingList />
    </div>
  );
}
