import { useMutation } from '@tanstack/react-query';
import { requestVerification } from '../api/auth';
import type { VerificationRequest } from '../types/auth.types';
import type { ApiResponse } from '@/types/api.types';

export function useRequestVerification() {
    return useMutation<ApiResponse<null>, Error, VerificationRequest>({
        mutationFn: (data: VerificationRequest) => requestVerification(data),
    });
}
