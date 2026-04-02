import apiClient from './client';
import type { ApiResponse, KeywordTrend, KeywordTimeline } from '@/types';

export async function fetchKeywordTrending(country: string, period: string = 'DAY', limit: number = 50): Promise<KeywordTrend[]> {
  const { data } = await apiClient.get<ApiResponse<KeywordTrend[]>>('/api/v2/keywords/trending', {
    params: { country, period, limit },
  });
  return data.data;
}

export async function fetchKeywordTimeline(keyword: string, period: string = 'WEEK'): Promise<KeywordTimeline[]> {
  const { data } = await apiClient.get<ApiResponse<KeywordTimeline[]>>(`/api/v2/keywords/${encodeURIComponent(keyword)}/timeline`, {
    params: { period },
  });
  return data.data;
}
