import apiClient from './client';
import type { ApiResponse, StatsOverview, TrendingKeyword } from '@/types';

export async function fetchStatsOverview(country: string): Promise<StatsOverview> {
  const { data } = await apiClient.get<ApiResponse<StatsOverview>>(
    '/api/v1/stats/overview',
    { params: { country } }
  );
  return data.data;
}

export async function fetchTrendingKeywords(country: string, limit: number = 30): Promise<TrendingKeyword[]> {
  const { data } = await apiClient.get<ApiResponse<TrendingKeyword[]>>(
    '/api/v1/stats/keywords',
    { params: { country, limit } }
  );
  return data.data;
}
