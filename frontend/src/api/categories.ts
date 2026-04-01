import apiClient from './client';
import type { ApiResponse, Category } from '@/types';

export async function fetchCategories(): Promise<Category[]> {
  const { data } = await apiClient.get<ApiResponse<Category[]>>('/api/v1/categories');
  return data.data;
}
