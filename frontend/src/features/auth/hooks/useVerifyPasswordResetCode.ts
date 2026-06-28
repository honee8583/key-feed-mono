import { useMutation } from '@tanstack/react-query';
import { verifyPasswordResetCode } from '../api/auth';
import type { PasswordResetVerifyRequest, VerificationResponse } from '../types/auth.types';
import type { ApiResponse } from '@/types/api.types';

export function useVerifyPasswordResetCode() {
    return useMutation<ApiResponse<VerificationResponse>, Error, PasswordResetVerifyRequest>({
        mutationFn: (data: PasswordResetVerifyRequest) => verifyPasswordResetCode(data),
    });
}
