import axios from 'axios';
import { env } from './env';
import { useAuthStore } from '@/stores/authStore';

export const apiClient = axios.create({
    baseURL: env.apiBaseUrl,
    timeout: 10_000,
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true, // For refresh token cookies
});

let isRefreshing = false;
let refreshSubscribers: ((token: string) => void)[] = [];

// 토큰 갱신 대기열에 담아뒀다가, 갱신 성공 시 순차 실행
function onRefreshed(token: string) {
    refreshSubscribers.forEach((callback) => callback(token));
    refreshSubscribers = [];
}

function addRefreshSubscriber(callback: (token: string) => void) {
    refreshSubscribers.push(callback);
}

// 1. 요청 인터셉터: Zustand 스토어에 토큰이 있다면 항상 주입
apiClient.interceptors.request.use(
    (config) => {
        const token = useAuthStore.getState().accessToken;
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

// 2. 응답 인터셉터: 401 에러 처리 및 토큰 재발급
apiClient.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;

        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;

            // 이미 토큰 갱신 중이라면 대기열(subscribers)에 넣고 대기
            if (isRefreshing) {
                return new Promise((resolve) => {
                    addRefreshSubscriber((token: string) => {
                        originalRequest.headers.Authorization = `Bearer ${token}`;
                        resolve(apiClient(originalRequest));
                    });
                });
            }

            // 갱신 중복 방지 플래그 ON
            isRefreshing = true;

            try {
                // 토큰 갱신 API 호출 (Secure 쿠키에 담긴 refreshToken 자동 전송됨)
                const { data } = await axios.post(`${env.apiBaseUrl}/api/auth/refresh`, {}, { withCredentials: true });

                // 새로 발급된 토큰 (응답 구조에 맞게 가져옴: { data: "new_jwt_..." })
                const newAccessToken = data.data;

                // 상태 업데이트 (유저는 유지하고 토큰만 교체)
                const currentUser = useAuthStore.getState().user;
                if (currentUser) {
                    useAuthStore.getState().setAuth(currentUser, newAccessToken);
                } else {
                    useAuthStore.getState().setAuth({ id: -1, email: '', name: '', role: 'USER' }, newAccessToken); // Fallback
                }

                // 갱신된 토큰으로 대기열에 쌓인 다른 요청들도 재시도 실행 처리
                onRefreshed(newAccessToken);

                // 현재 실패했던 요청에 갱신된 토큰 세팅 후 재요청
                originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
                return apiClient(originalRequest);

            } catch (refreshError) {
                // 리프레쉬 API 마저 실패 (예: Refresh Token 만료 등) -> 완전 로그아웃
                useAuthStore.getState().logout();
                return Promise.reject(refreshError);
            } finally {
                // 플래그 해제
                isRefreshing = false;
            }
        }

        return Promise.reject(error);
    }
);
