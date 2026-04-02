import TrendingList from '../components/trending/TrendingList';

export default function TrendingPage() {
  return (
    <div data-cy="trending-page">
      <h1 className="text-2xl font-bold mb-6">트렌딩 영상 TOP 50</h1>
      <TrendingList />
    </div>
  );
}
