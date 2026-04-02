import apiClient from './client';
import type { ApiResponse, AiAnalysisResult } from '@/types';

export async function fetchVideoAnalysis(videoId: string): Promise<AiAnalysisResult> {
  const { data } = await apiClient.get<ApiResponse<AiAnalysisResult>>(`/api/v2/ai/video/${videoId}`);
  return data.data;
}

export async function fetchChannelAnalysis(channelId: string): Promise<AiAnalysisResult> {
  const { data } = await apiClient.get<ApiResponse<AiAnalysisResult>>(`/api/v2/ai/channel/${channelId}`);
  return data.data;
}

export async function fetchPrediction(country: string): Promise<AiAnalysisResult> {
  const { data } = await apiClient.get<ApiResponse<AiAnalysisResult>>(`/api/v2/ai/prediction/${country}`);
  return data.data;
}
