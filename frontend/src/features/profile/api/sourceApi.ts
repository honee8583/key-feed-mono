import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/axios';

export interface UserSource {
    sourceId: number;
    userSourceId: number;
    userDefinedName: string;
    url: string;
    logoUrl: string | null;
    lastCrawledAt: string | null;
    receiveFeed: boolean;
}

export const sourceKeys = {
    all: ['sources', 'my'] as const,
    list: () => [...sourceKeys.all, 'list'] as const,
    search: (keyword: string) => [...sourceKeys.all, 'search', keyword] as const,
};

// 1. 내 소스 목록 조회
export async function getMySources(): Promise<UserSource[]> {
    const { data } = await apiClient.get<{ data: UserSource[] }>('/api/sources/my');
    return data.data;
}

export function useMySources() {
    return useQuery({
        queryKey: sourceKeys.list(),
        queryFn: getMySources,
    });
}

// 2. 내 소스 검색
export async function searchMySources(keyword: string): Promise<UserSource[]> {
    const { data } = await apiClient.get<{ data: UserSource[] }>('/api/sources/my/search', {
        params: { keyword }
    });
    return data.data;
}

export function useSearchMySources(keyword: string) {
    return useQuery({
        queryKey: sourceKeys.search(keyword),
        queryFn: () => searchMySources(keyword),
        enabled: !!keyword,
    });
}

// 3. 소스 추가 (구독)
export async function addSource(params: { name: string; url: string; receiveFeed?: boolean }): Promise<UserSource> {
    const { data } = await apiClient.post<{ data: UserSource }>('/api/sources', params);
    return data.data;
}

export function useAddSource() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: addSource,
        onSuccess: () => {
             queryClient.invalidateQueries({ queryKey: sourceKeys.all });
        }
    });
}

// 4. 소스 피드 수신 토글
export async function toggleFeedReceive(userSourceId: number): Promise<UserSource> {
    const { data } = await apiClient.patch<{ data: UserSource }>(`/api/sources/my/${userSourceId}/receive-feed`);
    return data.data;
}

export function useToggleFeedReceive() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: toggleFeedReceive,
        onSuccess: () => {
             queryClient.invalidateQueries({ queryKey: sourceKeys.all });
        }
    });
}

// 5. 소스 구독 해제
export async function deleteSource(userSourceId: number) {
    const { data } = await apiClient.delete(`/api/sources/my/${userSourceId}`);
    return data;
}

export function useDeleteSource() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: deleteSource,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: sourceKeys.all });
        }
    });
}
