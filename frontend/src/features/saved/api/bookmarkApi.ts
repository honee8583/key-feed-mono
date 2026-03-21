import { useInfiniteQuery, useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/axios';
import type { BookmarkResponse, BookmarkFolder } from '../types';

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

export async function getBookmarkFolders(): Promise<BookmarkFolder[]> {
    const { data } = await apiClient.get<{ data: BookmarkFolder[] }>('/api/bookmarks/folders');
    return data.data;
}

export const folderKeys = {
    all: ['folders'] as const,
};

export function useBookmarkFolders() {
    return useQuery({
        queryKey: folderKeys.all,
        queryFn: getBookmarkFolders,
    });
}

export async function createBookmarkFolder(folder: { name: string; icon?: string; color?: string }): Promise<number> {
    const { data } = await apiClient.post<{ data: number }>('/api/bookmarks/folders', folder);
    return data.data;
}

export async function updateBookmarkFolder(params: { folderId: number; name: string; icon?: string; color?: string }) {
    const { folderId, ...body } = params;
    const { data } = await apiClient.patch(`/api/bookmarks/folders/${folderId}`, body);
    return data;
}

export async function deleteBookmarkFolder(folderId: number) {
    const { data } = await apiClient.delete(`/api/bookmarks/folders/${folderId}`);
    return data;
}

export function useCreateBookmarkFolder() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: createBookmarkFolder,
        onSuccess: () => {
             queryClient.invalidateQueries({ queryKey: folderKeys.all });
        }
    });
}

export function useUpdateBookmarkFolder() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: updateBookmarkFolder,
        onSuccess: () => {
             queryClient.invalidateQueries({ queryKey: folderKeys.all });
        }
    });
}

export function useDeleteBookmarkFolder() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: deleteBookmarkFolder,
        onSuccess: () => {
             queryClient.invalidateQueries({ queryKey: folderKeys.all });
        }
    });
}

export function useCreateBookmark() {
    return useMutation({
        mutationFn: async (contentId: string) => {
            const { data } = await apiClient.post<{ data: number }>('/api/bookmarks', { contentId });
            return data.data; // bookmarkId
        }
    });
}

export function useDeleteBookmark() {
    return useMutation({
        mutationFn: async (bookmarkId: number) => {
            const { data } = await apiClient.delete(`/api/bookmarks/${bookmarkId}`);
            return data;
        }
    });
}

export async function moveBookmarkFolder(params: { bookmarkId: number; folderId: number }) {
    const { data } = await apiClient.patch(`/api/bookmarks/${params.bookmarkId}/folder`, { folderId: params.folderId });
    return data;
}

export function useMoveBookmarkFolder() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: moveBookmarkFolder,
        onSuccess: () => {
             queryClient.invalidateQueries({ queryKey: bookmarkKeys.all });
        }
    });
}

export async function removeBookmarkFromFolder(bookmarkId: number) {
    const { data } = await apiClient.delete(`/api/bookmarks/${bookmarkId}/folder`);
    return data;
}

export function useRemoveBookmarkFromFolder() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: removeBookmarkFromFolder,
        onSuccess: () => {
             queryClient.invalidateQueries({ queryKey: bookmarkKeys.all });
        }
    });
}
