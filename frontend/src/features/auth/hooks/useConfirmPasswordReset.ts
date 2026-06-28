import { useMutation } from '@tanstack/react-query';
import { confirmPasswordReset } from '../api/auth';
import type { PasswordResetConfirmRequest } from '../types/auth.types';
import type { ApiResponse } from '@/types/api.types';

export function useConfirmPasswordReset() {
    return useMutation<ApiResponse<null>, Error, PasswordResetConfirmRequest>({
        mutationFn: (data: PasswordResetConfirmRequest) => confirmPasswordReset(data),
    });
}
