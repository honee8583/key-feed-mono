import { useMutation } from '@tanstack/react-query';
import { confirmVerification } from '../api/auth';
import type { VerificationConfirmRequest, VerificationResponse } from '../types/auth.types';
import type { ApiResponse } from '@/types/api.types';

export function useConfirmVerification() {
    return useMutation<ApiResponse<VerificationResponse>, Error, VerificationConfirmRequest>({
        mutationFn: (data: VerificationConfirmRequest) => confirmVerification(data),
    });
}
