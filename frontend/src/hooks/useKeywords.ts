import { useQuery } from '@tanstack/react-query';
import { fetchKeywordTrending, fetchKeywordTimeline } from '@/api/keywords';

export function useKeywordTrending(country: string, period: string = 'DAY', limit: number = 50) {
  return useQuery({
    queryKey: ['keywordTrending', country, period, limit],
    queryFn: () => fetchKeywordTrending(country, period, limit),
    staleTime: 1000 * 60 * 5,
  });
}

export function useKeywordTimeline(keyword: string | undefined, period: string = 'WEEK') {
  return useQuery({
    queryKey: ['keywordTimeline', keyword, period],
    queryFn: () => fetchKeywordTimeline(keyword!, period),
    enabled: !!keyword,
    staleTime: 1000 * 60 * 5,
  });
}
