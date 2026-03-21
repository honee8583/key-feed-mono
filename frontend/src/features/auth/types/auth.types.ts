export interface User {
    id: number;
    email: string;
    name: string;
    role: 'USER' | 'ADMIN';
}

export interface LoginResponse {
    id: number;
    email: string;
    name: string;
    role: 'USER' | 'ADMIN';
    accessToken: string;
}

export interface LoginRequest {
    email: string;
    password?: string;
}

export interface JoinRequest {
    email: string;
    password?: string;
    name: string;
}

export interface VerificationRequest {
    email: string;
}

export interface VerificationConfirmRequest {
    email: string;
    code: string;
}

export interface VerificationResponse {
    status: 'PENDING' | 'VERIFIED' | 'EXPIRED' | 'LOCKED';
    attempts: number;
    retryAt: string | null;
    expiresAt: string;
}
