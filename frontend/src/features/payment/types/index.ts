export interface PaymentMethod {
    methodId: number;
    methodType: string; // "CARD" | "BANK"
    providerName: string;
    displayNumber: string;
    isDefault: boolean;
    createdAt: string;
}

export type SubscriptionStatus = 'NONE' | 'ACTIVE' | 'PAUSED' | 'CANCELED' | 'REFUNDED' | 'INACTIVE';

export interface Subscription {
    subscriptionId: number | null;
    status: SubscriptionStatus;
    price?: number;
    startedAt?: string | null;
    nextBillingAt?: string | null;
    expiredAt?: string | null;
    canceledAt?: string | null;
    retryCount?: number;
    providerName?: string | null;
    displayNumber?: string | null;
}
