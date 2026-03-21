import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import type { User } from '@/features/auth/types/auth.types';

interface AuthStore {
    user: User | null;
    accessToken: string | null;
    isAuthenticated: boolean;
    pendingEmail: string | null;
    setAuth: (user: User, accessToken: string) => void;
    setPendingEmail: (email: string | null) => void;
    logout: () => void;
}

export const useAuthStore = create<AuthStore>()(
    persist(
        (set) => ({
            user: null,
            accessToken: null,
            isAuthenticated: false,
            pendingEmail: null,
            setAuth: (user, accessToken) => set({ user, accessToken, isAuthenticated: true }),
            setPendingEmail: (email) => set({ pendingEmail: email }),
            logout: () => {
                // Clear common auth cookies just in case (refreshToken usually HTTP-only, 
                // but if it's accessible or if there are other client-side cookies, clear them)
                document.cookie = 'refreshToken=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
                document.cookie = 'accessToken=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';

                set({ user: null, accessToken: null, isAuthenticated: false, pendingEmail: null });

                // 리다이렉트 (Silent Refresh 실패 시 등)
                if (typeof window !== 'undefined' && !window.location.pathname.startsWith('/auth')) {
                    window.location.href = '/auth/login';
                }
            },
        }),
        {
            name: 'auth-storage', // 로컬 스토리지에 저장될 키 이름
            storage: createJSONStorage(() => localStorage),
            partialize: (state) => ({
                user: state.user,
                accessToken: state.accessToken,
                isAuthenticated: state.isAuthenticated
            }), // pendingEmail은 유지하지 않음
        }
    )
);
