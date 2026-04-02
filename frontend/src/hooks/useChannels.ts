import { useQuery } from '@tanstack/react-query';
import { fetchChannelRanking, fetchChannelDetail, fetchChannelSnapshots } from '@/api/channels';

export function useChannelRanking(country: string, limit: number = 100) {
  return useQuery({
    queryKey: ['channelRanking', country, limit],
    queryFn: () => fetchChannelRanking(country, limit),
    staleTime: 1000 * 60 * 5,
  });
}

export function useChannelDetail(channelId: string | undefined) {
  return useQuery({
    queryKey: ['channelDetail', channelId],
    queryFn: () => fetchChannelDetail(channelId!),
    enabled: !!channelId,
    staleTime: 1000 * 60 * 5,
  });
}

export function useChannelSnapshots(channelId: string | undefined) {
  return useQuery({
    queryKey: ['channelSnapshots', channelId],
    queryFn: () => fetchChannelSnapshots(channelId!),
    enabled: !!channelId,
    staleTime: 1000 * 60 * 5,
  });
}
