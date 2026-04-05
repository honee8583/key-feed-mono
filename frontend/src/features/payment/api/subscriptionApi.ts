import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '@/lib/axios';
import type { Subscription } from '../types';

export const subscriptionKeys = {
    all: ['subscription'] as const,
    me: () => [...subscriptionKeys.all, 'me'] as const,
};

export async function getMySubscription(): Promise<Subscription> {
    const { data } = await apiClient.get<{ data: Subscription }>('/api/subscriptions/me');
    return data.data;
}

export async function startSubscription(methodId: number): Promise<Subscription> {
    const { data } = await apiClient.post<{ data: Subscription }>('/api/subscriptions/start', { methodId });
    return data.data;
}

export async function cancelSubscription(): Promise<{ status: string; expiredAt: string; canceledAt: string }> {
    const { data } = await apiClient.post('/api/subscriptions/cancel');
    return data.data;
}

export async function resumeSubscription(methodId: number): Promise<Subscription> {
    const { data } = await apiClient.post<{ data: Subscription }>('/api/subscriptions/resume', { methodId });
    return data.data;
}

export async function refundSubscription(): Promise<{ status: string; canceledAt: string }> {
    const { data } = await apiClient.post('/api/subscriptions/refund');
    return data.data;
}

export function useMySubscription() {
    return useQuery({
        queryKey: subscriptionKeys.me(),
        queryFn: getMySubscription,
    });
}

export function useStartSubscription() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: startSubscription,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: subscriptionKeys.all });
        },
    });
}

export function useCancelSubscription() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: cancelSubscription,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: subscriptionKeys.all });
        },
    });
}

export function useResumeSubscription() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: resumeSubscription,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: subscriptionKeys.all });
        },
    });
}

export function useRefundSubscription() {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: refundSubscription,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: subscriptionKeys.all });
        },
    });
}
