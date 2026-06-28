import { useMutation } from '@tanstack/react-query';
import { apiClient } from '@/lib/axios';
import type { ApiResponse } from '@/types/api.types';

export interface ChangePasswordRequest {
    currentPassword: string;
    newPassword: string;
    confirmPassword: string;
}

// 비밀번호 변경 — PATCH /api/users/password
export async function changePassword(body: ChangePasswordRequest): Promise<ApiResponse<null>> {
    const { data } = await apiClient.patch<ApiResponse<null>>('/api/users/password', body);
    return data;
}

export function useChangePassword() {
    return useMutation({
        mutationFn: changePassword,
    });
}

// 회원 탈퇴 — DELETE /api/users (현재 비밀번호로 본인 확인)
export async function withdrawUser(password: string): Promise<ApiResponse<null>> {
    const { data } = await apiClient.delete<ApiResponse<null>>('/api/users', {
        data: { password },
    });
    return data;
}

export function useWithdrawUser() {
    return useMutation({
        mutationFn: withdrawUser,
    });
}
