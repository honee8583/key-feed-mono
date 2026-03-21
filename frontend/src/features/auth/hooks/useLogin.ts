import { useMutation } from '@tanstack/react-query';
import { login } from '../api/auth';
import { useAuthStore } from '@/stores/authStore';
import type { LoginRequest, LoginResponse } from '../types/auth.types';
import type { ApiResponse } from '@/types/api.types';

export function useLogin() {
    const setAuth = useAuthStore((state) => state.setAuth);

    return useMutation<ApiResponse<LoginResponse>, Error, LoginRequest>({
        mutationFn: (data: LoginRequest) => login(data),
        onSuccess: (response) => {
            console.log('📌 [임시 확인용] 로그인 서버 응답 데이터 (성공):', response);
            const { accessToken, ...user } = response.data;
            setAuth(user, accessToken);
        },
        onError: (error) => {
            const apiError = error as { response?: { data?: unknown } };
            console.log('📌 [임시 확인용] 로그인 서버 응답 데이터 (에러):', apiError?.response?.data || error);
        }
    });
}
