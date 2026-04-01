import apiClient from './client';
import type { ApiResponse, BriefingResponse } from '@/types';

export async function fetchBriefing(country: string): Promise<BriefingResponse> {
  const { data } = await apiClient.get<ApiResponse<BriefingResponse>>(
    '/api/v1/briefing', { params: { country } }
  );
  return data.data;
}
