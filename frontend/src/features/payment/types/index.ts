export interface PaymentMethod {
    methodId: number;
    methodType: string; // "CARD" | "BANK"
    providerName: string;
    displayNumber: string;
    isDefault: boolean;
    createdAt: string;
}
