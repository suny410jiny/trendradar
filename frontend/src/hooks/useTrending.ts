import { useQuery } from '@tanstack/react-query';
import { fetchTrending, fetchSnapshots } from '@/api/trending';

export function useTrending(country: string, category?: number | null, tag?: string | null, limit: number = 10) {
  return useQuery({
    queryKey: ['trending', country, category, tag, limit],
    queryFn: () => fetchTrending(country, category ?? undefined, tag ?? undefined, limit),
    staleTime: 1000 * 60 * 5,
  });
}

export function useSnapshots(videoId: string) {
  return useQuery({
    queryKey: ['snapshots', videoId],
    queryFn: () => fetchSnapshots(videoId),
    enabled: !!videoId,
  });
}
