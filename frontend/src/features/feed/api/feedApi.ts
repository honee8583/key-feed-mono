import { useInfiniteQuery } from '@tanstack/react-query';
import { apiClient } from '@/lib/axios';
import type { FeedResponse } from '../types';

interface GetFeedParams {
    lastId?: number | null;
    size?: number;
    keyword?: string;
}

export async function getFeed({ lastId, size = 10, keyword }: GetFeedParams): Promise<FeedResponse> {
    const { data } = await apiClient.get<{ data: FeedResponse }>('/api/feed', {
        params: { lastId, size, keyword },
    });
    return data.data;
}

export const feedKeys = {
    all: ['feed'] as const,
    list: () => [...feedKeys.all, 'list'] as const,
    search: (keyword: string) => [...feedKeys.all, 'search', keyword] as const,
};

interface UseFeedOptions {
    keyword?: string;
    enabled?: boolean;
}

export function useFeed(options: UseFeedOptions = {}) {
    const { keyword, enabled = true } = options;
    return useInfiniteQuery({
        queryKey: keyword ? feedKeys.search(keyword) : feedKeys.list(),
        queryFn: ({ pageParam }) => getFeed({ lastId: pageParam as number | null, size: 10, keyword }),
        initialPageParam: null as number | null,
        getNextPageParam: (lastPage) => lastPage.hasNext ? lastPage.nextCursorId : null,
        enabled,
    });
}
