import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@/lib/axios';
import type { TrendingKeywordData, RecommendedSourceData } from '../types';

export async function getTrendingKeywords(size: number = 10): Promise<TrendingKeywordData[]> {
    const { data } = await apiClient.get<{ data: TrendingKeywordData[] }>('/api/keywords/trending', {
        params: { size },
    });
    return data.data;
}

export const exploreKeys = {
    all: ['explore'] as const,
    trending: () => [...exploreKeys.all, 'trending'] as const,
    recommendedSources: (page: number, size: number) => [...exploreKeys.all, 'recommendedSources', page, size] as const,
};

export function useTrendingKeywords(size: number = 10) {
    return useQuery({
        queryKey: exploreKeys.trending(),
        queryFn: () => getTrendingKeywords(size),
    });
}

export async function getRecommendedSources(page: number = 0, size: number = 10): Promise<RecommendedSourceData[]> {
    const { data } = await apiClient.get<{ data: RecommendedSourceData[] }>('/api/sources/recommended', {
        params: { page, size },
    });
    return data.data;
}

export function useRecommendedSources(page: number = 0, size: number = 10) {
    return useQuery({
        queryKey: exploreKeys.recommendedSources(page, size),
        queryFn: () => getRecommendedSources(page, size),
    });
}
