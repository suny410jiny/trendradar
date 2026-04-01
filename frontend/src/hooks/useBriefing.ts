import { useQuery } from '@tanstack/react-query';
import { fetchBriefing } from '@/api/briefing';

export function useBriefing(country: string) {
  return useQuery({
    queryKey: ['briefing', country],
    queryFn: () => fetchBriefing(country),
    staleTime: 1000 * 60 * 10,
  });
}
