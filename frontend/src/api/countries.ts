import apiClient from './client';
import type { ApiResponse, Country } from '@/types';

export async function fetchCountries(): Promise<Country[]> {
  const { data } = await apiClient.get<ApiResponse<Country[]>>('/api/v1/countries');
  return data.data;
}
