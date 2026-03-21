import { useInfiniteQuery } from '@tanstack/react-query';
import { apiClient } from '@/lib/axios';
import type { BookmarkResponse } from '../types';

interface GetBookmarksParams {
    folderId?: number;
    lastId?: number;
    size?: number;
}

export async function getBookmarks(params: GetBookmarksParams): Promise<BookmarkResponse> {
    const { data } = await apiClient.get<{ data: BookmarkResponse }>('/api/bookmarks', {
        params,
    });
    return data.data;
}

export const bookmarkKeys = {
    all: ['bookmarks'] as const,
    list: (folderId?: number) => [...bookmarkKeys.all, 'list', folderId] as const,
};

export function useBookmarks(folderId?: number, size: number = 20) {
    return useInfiniteQuery({
        queryKey: bookmarkKeys.list(folderId),
        queryFn: ({ pageParam }) => getBookmarks({ folderId, lastId: pageParam, size }),
        initialPageParam: undefined as number | undefined,
        getNextPageParam: (lastPage) => lastPage.hasNext ? lastPage.nextCursorId : undefined,
    });
}
