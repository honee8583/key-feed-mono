import { useMutation } from '@tanstack/react-query';
import { join } from '../api/auth';
import type { JoinRequest } from '../types/auth.types';
import type { ApiResponse } from '@/types/api.types';

export function useJoin() {
    return useMutation<ApiResponse<null>, Error, JoinRequest>({
        mutationFn: (data: JoinRequest) => join(data),
    });
}
