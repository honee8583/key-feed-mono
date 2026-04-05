export interface PaymentMethod {
    methodId: number;
    methodType: string; // "CARD" | "BANK"
    providerName: string;
    displayNumber: string;
    isDefault: boolean;
    createdAt: string;
}

export type SubscriptionStatus = 'NONE' | 'ACTIVE' | 'PAUSED' | 'CANCELED' | 'REFUNDED' | 'INACTIVE';

export type PaymentHistoryStatus = 'DONE' | 'FAILED' | 'CANCELED';

export interface PaymentHistoryItem {
    paymentId: number;
    orderId: string;
    orderName: string;
    amount: number;
    status: PaymentHistoryStatus;
    failReason: string | null;
    approvedAt: string | null;
    createdAt: string;
    paymentMethod: {
        providerName: string;
        displayNumber: string;
        methodType: string;
    };
}

export interface PaymentHistoryPage {
    content: PaymentHistoryItem[];
    nextCursorId: number | null;
    hasNext: boolean;
}

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
