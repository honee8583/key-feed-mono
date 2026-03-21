import { useInfiniteQuery } from '@tanstack/react-query';
import { apiClient } from '@/lib/axios';
import type { FeedResponse } from '../types';

interface GetFeedParams {
    lastId?: number | null;
    size?: number;
}

export async function getFeed({ lastId, size = 10 }: GetFeedParams): Promise<FeedResponse> {
    const { data } = await apiClient.get<{ data: FeedResponse }>('/api/feed', {
        params: { lastId, size },
    });
    return data.data;
}

export const feedKeys = {
    all: ['feed'] as const,
    list: () => [...feedKeys.all, 'list'] as const,
};

export function useFeed() {
    return useInfiniteQuery({
        queryKey: feedKeys.list(),
        queryFn: ({ pageParam }) => getFeed({ lastId: pageParam, size: 10 }),
        initialPageParam: null as number | null,
        getNextPageParam: (lastPage) => lastPage.hasNext ? lastPage.nextCursorId : null,
    });
}
