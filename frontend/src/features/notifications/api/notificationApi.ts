import { useInfiniteQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { apiClient } from '@/lib/axios';
import { env } from '@/lib/env';
import { useAuthStore } from '@/stores/authStore';
import type { Notification } from '@/types';

interface NotificationsResponse {
    status: number;
    message: string;
    data: {
        content: Notification[];
        nextCursorId: number | null;
        hasNext: boolean;
    };
}

export const notificationKeys = {
    all: ['notifications'] as const,
    list: () => [...notificationKeys.all, 'list'] as const,
};

export function useNotifications() {
    return useInfiniteQuery({
        queryKey: notificationKeys.list(),
        queryFn: async ({ pageParam }) => {
            const params: Record<string, any> = { size: 20 };
            if (pageParam !== null) {
                params.lastId = pageParam;
            }
            const { data } = await apiClient.get<NotificationsResponse>('/api/notifications', { params });
            return data.data;
        },
        initialPageParam: null as number | null,
        getNextPageParam: (lastPage) => lastPage.hasNext ? lastPage.nextCursorId : null,
    });
}

export function useNotificationSubscription() {
    const queryClient = useQueryClient();
    const token = useAuthStore(state => state.accessToken);
    
    useEffect(() => {
        if (!token) return;

        let reader: ReadableStreamDefaultReader<Uint8Array> | undefined;
        let isActive = true;

        const connect = async () => {
            try {
                // native EventSource doesn't support custom headers, using fetch for SSE
                const response = await fetch(`${env.apiBaseUrl}/api/notifications/subscribe`, {
                    headers: {
                        Authorization: `Bearer ${token}`,
                        Accept: 'text/event-stream'
                    }
                });

                if (!response.ok || !response.body) return;

                console.log('✅ 실시간 알림(SSE) 서버 연동 성공');
                reader = response.body.getReader();
                const decoder = new TextDecoder();
                let buffer = '';

                while (isActive) {
                    const { value, done } = await reader.read();
                    if (done) break;

                    buffer += decoder.decode(value, { stream: true });
                    const lines = buffer.split('\n');
                    buffer = lines.pop() || ''; // keep the last incomplete line in buffer

                    let eventType = '';
                    let eventData = '';

                    for (const line of lines) {
                        if (line.startsWith('event:')) {
                            eventType = line.replace('event:', '').trim();
                        } else if (line.startsWith('data:')) {
                            eventData = line.replace('data:', '').trim();
                        } else if (line === '') {
                            // Empty line means end of an event
                            if (eventType === 'notification' && eventData && eventData !== 'connected') {
                                try {
                                    const newNotification = JSON.parse(eventData);
                                    
                                    // Prepend the new notification to the first page of the cache
                                    queryClient.setQueryData(notificationKeys.list(), (oldData: any) => {
                                        if (!oldData) return oldData;
                                        
                                        const newPages = [...oldData.pages];
                                        if (newPages.length > 0) {
                                            newPages[0] = {
                                                ...newPages[0],
                                                content: [newNotification, ...newPages[0].content]
                                            };
                                        }
                                        return { ...oldData, pages: newPages };
                                    });
                                } catch (e) {
                                    console.error('Failed to parse SSE notification', e);
                                }
                            }
                            // Reset for next event
                            eventType = '';
                            eventData = '';
                        }
                    }
                }
            } catch (err) {
                console.error("SSE Connection Error:", err);
                // Simple reconnection logic could be added here
            }
        };

        connect();

        return () => {
            isActive = false;
            // Best effort to cancel the reader
            if (reader) reader.cancel();
        };
    }, [token, queryClient]);
}
