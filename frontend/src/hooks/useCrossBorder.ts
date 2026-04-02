import { useQuery } from '@tanstack/react-query';
import { fetchOpportunities, fetchPropagation, fetchGlobalVsLocal } from '@/api/crossborder';

export function useOpportunities(country: string) {
  return useQuery({
    queryKey: ['opportunities', country],
    queryFn: () => fetchOpportunities(country),
    staleTime: 1000 * 60 * 5,
  });
}

export function usePropagation(keyword: string | undefined) {
  return useQuery({
    queryKey: ['propagation', keyword],
    queryFn: () => fetchPropagation(keyword!),
    enabled: !!keyword,
    staleTime: 1000 * 60 * 5,
  });
}

export function useGlobalVsLocal(country: string) {
  return useQuery({
    queryKey: ['globalVsLocal', country],
    queryFn: () => fetchGlobalVsLocal(country),
    staleTime: 1000 * 60 * 5,
  });
}
