import { useMutation } from '@tanstack/react-query';
import { requestPasswordReset } from '../api/auth';
import type { PasswordResetRequest } from '../types/auth.types';
import type { ApiResponse } from '@/types/api.types';

export function useRequestPasswordReset() {
    return useMutation<ApiResponse<null>, Error, PasswordResetRequest>({
        mutationFn: (data: PasswordResetRequest) => requestPasswordReset(data),
    });
}
