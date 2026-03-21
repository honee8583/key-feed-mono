import { apiClient } from '@/lib/axios';
import type { ApiResponse } from '@/types/api.types';
import type { LoginRequest, LoginResponse, JoinRequest, VerificationRequest, VerificationConfirmRequest, VerificationResponse } from '../types/auth.types';

export async function login(data: LoginRequest): Promise<ApiResponse<LoginResponse>> {
    const response = await apiClient.post<ApiResponse<LoginResponse>>('/api/auth/login', data);
    return response.data;
}

export async function join(data: JoinRequest): Promise<ApiResponse<null>> {
    const response = await apiClient.post<ApiResponse<null>>('/api/auth/join', data);
    return response.data;
}

export async function requestVerification(data: VerificationRequest): Promise<ApiResponse<null>> {
    const response = await apiClient.post<ApiResponse<null>>('/api/auth/email-verification/request', data);
    return response.data;
}

export async function confirmVerification(data: VerificationConfirmRequest): Promise<ApiResponse<VerificationResponse>> {
    const response = await apiClient.post<ApiResponse<VerificationResponse>>('/api/auth/email-verification/confirm', data);
    return response.data;
}
