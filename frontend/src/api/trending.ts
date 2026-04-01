import apiClient from './client';
import type { ApiResponse, TrendingVideo, ViewSnapshot } from '@/types';

export async function fetchTrending(
  country: string,
  category?: number,
  tag?: string,
  limit: number = 10
): Promise<TrendingVideo[]> {
  const params: Record<string, string | number> = { country, limit };
  if (category) params.category = category;
  if (tag) params.tag = tag;

  const { data } = await apiClient.get<ApiResponse<TrendingVideo[]>>('/api/v1/trending', { params });
  return data.data;
}

export async function fetchSnapshots(videoId: string): Promise<ViewSnapshot[]> {
  const { data } = await apiClient.get<ApiResponse<ViewSnapshot[]>>(
    `/api/v1/trending/${videoId}/snapshots`
  );
  return data.data;
}
