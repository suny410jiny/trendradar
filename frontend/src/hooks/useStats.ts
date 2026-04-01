import { useQuery } from '@tanstack/react-query';
import { fetchStatsOverview, fetchTrendingKeywords } from '@/api/stats';

export function useStatsOverview(country: string) {
  return useQuery({
    queryKey: ['stats-overview', country],
    queryFn: () => fetchStatsOverview(country),
    staleTime: 1000 * 60 * 5,
  });
}

export function useTrendingKeywords(country: string, limit: number = 30) {
  return useQuery({
    queryKey: ['trending-keywords', country, limit],
    queryFn: () => fetchTrendingKeywords(country, limit),
    staleTime: 1000 * 60 * 5,
  });
}
