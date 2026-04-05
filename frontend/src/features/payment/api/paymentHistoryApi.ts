import { useInfiniteQuery } from '@tanstack/react-query';
import { apiClient } from '@/lib/axios';
import type { PaymentHistoryPage } from '../types';

export const paymentHistoryKeys = {
    all: ['paymentHistory'] as const,
    list: (status?: string) => [...paymentHistoryKeys.all, 'list', status] as const,
};

export async function getPaymentHistory(params: {
    cursorId?: number;
    size?: number;
    status?: string;
}): Promise<PaymentHistoryPage> {
    const { data } = await apiClient.get<{ data: PaymentHistoryPage }>('/api/payment-history', {
        params: {
            ...(params.cursorId != null && { cursorId: params.cursorId }),
            size: params.size ?? 10,
            ...(params.status && { status: params.status }),
        },
    });
    return data.data;
}

export function usePaymentHistory(status?: string) {
    return useInfiniteQuery({
        queryKey: paymentHistoryKeys.list(status),
        queryFn: ({ pageParam }) =>
            getPaymentHistory({ cursorId: pageParam as number | undefined, status }),
        initialPageParam: undefined as number | undefined,
        getNextPageParam: (lastPage) =>
            lastPage.hasNext ? (lastPage.nextCursorId ?? undefined) : undefined,
    });
}
