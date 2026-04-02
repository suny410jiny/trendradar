import apiClient from './client';
import type { ApiResponse, CrossBorderOpportunity, CrossBorderPropagation, CrossBorderGlobalLocal } from '@/types';

export async function fetchOpportunities(country: string): Promise<CrossBorderOpportunity[]> {
  const { data } = await apiClient.get<ApiResponse<CrossBorderOpportunity[]>>('/api/v2/crossborder/opportunities', {
    params: { country },
  });
  return data.data;
}

export async function fetchPropagation(keyword: string): Promise<CrossBorderPropagation> {
  const { data } = await apiClient.get<ApiResponse<CrossBorderPropagation>>('/api/v2/crossborder/propagation', {
    params: { keyword },
  });
  return data.data;
}

export async function fetchGlobalVsLocal(country: string): Promise<CrossBorderGlobalLocal> {
  const { data } = await apiClient.get<ApiResponse<CrossBorderGlobalLocal>>('/api/v2/crossborder/global-vs-local', {
    params: { country },
  });
  return data.data;
}
