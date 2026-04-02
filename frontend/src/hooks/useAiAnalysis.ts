import { useQuery } from '@tanstack/react-query';
import { fetchVideoAnalysis, fetchChannelAnalysis, fetchPrediction } from '@/api/ai';

export function useVideoAnalysis(videoId: string | undefined) {
  return useQuery({
    queryKey: ['videoAnalysis', videoId],
    queryFn: () => fetchVideoAnalysis(videoId!),
    enabled: !!videoId,
    staleTime: 1000 * 60 * 60,  // 1 hour (AI analysis cached)
  });
}

export function useChannelAnalysis(channelId: string | undefined) {
  return useQuery({
    queryKey: ['channelAnalysis', channelId],
    queryFn: () => fetchChannelAnalysis(channelId!),
    enabled: !!channelId,
    staleTime: 1000 * 60 * 30,  // 30 min
  });
}

export function usePrediction(country: string) {
  return useQuery({
    queryKey: ['prediction', country],
    queryFn: () => fetchPrediction(country),
    staleTime: 1000 * 60 * 60,  // 1 hour
  });
}
