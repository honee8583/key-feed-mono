import { create } from 'zustand';
import type { Notification } from '@/types';
import { MOCK_NOTIFICATIONS } from '@/lib/mock';

interface NotificationState {
    notifications: Notification[];
    clearAll: () => void;
}

export const useNotificationStore = create<NotificationState>((set) => ({
    notifications: MOCK_NOTIFICATIONS,
    clearAll: () => set({ notifications: [] })
}));
