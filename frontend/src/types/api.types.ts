export interface ApiResponse<T> {
    status: number;
    message: string;
    data: T;
}

export interface PaginatedResponse<T> {
    content: T[];
    nextCursorId: number | null;
    hasNext: boolean;
}

export interface ApiError {
    status: number;
    message: string;
    data: Record<string, string> | null;
}
