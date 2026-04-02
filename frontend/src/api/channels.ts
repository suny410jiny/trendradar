import apiClient from './client';
import type { ApiResponse, ChannelRanking, ChannelDetail, ChannelSnapshot } from '@/types';

export async function fetchChannelRanking(country: string, limit: number = 100): Promise<ChannelRanking[]> {
  const { data } = await apiClient.get<ApiResponse<ChannelRanking[]>>('/api/v2/channels/ranking', {
    params: { country, limit },
  });
  return data.data;
}

export async function fetchChannelDetail(channelId: string): Promise<ChannelDetail> {
  const { data } = await apiClient.get<ApiResponse<ChannelDetail>>(`/api/v2/channels/${channelId}`);
  return data.data;
}

export async function fetchChannelSnapshots(channelId: string): Promise<ChannelSnapshot[]> {
  const { data } = await apiClient.get<ApiResponse<ChannelSnapshot[]>>(`/api/v2/channels/${channelId}/snapshots`);
  return data.data;
}
