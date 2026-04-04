import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/axios';
import type { PaymentMethod } from '../types';

export const paymentKeys = {
    all: ['payment-methods'] as const,
    list: () => [...paymentKeys.all, 'list'] as const,
};

// 1. 고객 키 발급
export async function getCustomerKey(): Promise<string> {
    const { data } = await apiClient.get<{ data: string }>('/api/payment-methods/customer-key');
    return data.data;
}

// 2. 결제 수단 등록 (authKey 전달)
export async function registerPaymentMethod(authKey: string): Promise<PaymentMethod> {
    const { data } = await apiClient.post<{ data: PaymentMethod }>('/api/payment-methods/register', { authKey });
    return data.data;
}

// 3. 결제 수단 목록 조회
export async function getPaymentMethods(): Promise<PaymentMethod[]> {
    const { data } = await apiClient.get<{ data: PaymentMethod[] }>('/api/payment-methods');
    return data.data;
}

export function usePaymentMethods() {
    return useQuery({
        queryKey: paymentKeys.list(),
        queryFn: getPaymentMethods,
    });
}

// 4. 결제 수단 삭제
export async function deletePaymentMethod(methodId: number) {
    await apiClient.delete(`/api/payment-methods/${methodId}`);
}

export function useDeletePaymentMethod() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: deletePaymentMethod,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: paymentKeys.all });
        }
    });
}

// 5. 기본 결제 수단 변경
export async function setDefaultPaymentMethod(methodId: number) {
    await apiClient.patch(`/api/payment-methods/${methodId}/default`);
}

export function useSetDefaultPaymentMethod() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: setDefaultPaymentMethod,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: paymentKeys.all });
        }
    });
}
