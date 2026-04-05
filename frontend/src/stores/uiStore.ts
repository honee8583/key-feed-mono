import { create } from 'zustand';

export interface UiState {
    isSearchMounted: boolean;
    isSearchOpen: boolean;
    isNotificationsMounted: boolean;
    isNotificationsOpen: boolean;
    isFolderMounted: boolean;
    isFolderOpen: boolean;
    isUpgradeMounted: boolean;
    isUpgradeOpen: boolean;
    isSourcesMounted: boolean;
    isSourcesOpen: boolean;
    isPaymentMethodMounted: boolean;
    isPaymentMethodOpen: boolean;
    isSubscriptionMounted: boolean;
    isSubscriptionOpen: boolean;
    isPaymentHistoryMounted: boolean;
    isPaymentHistoryOpen: boolean;

    openSearch: () => void;
    closeSearch: () => void;
    unmountSearch: () => void;

    openNotifications: () => void;
    closeNotifications: () => void;
    unmountNotifications: () => void;

    openFolderManagement: () => void;
    closeFolderManagement: () => void;
    unmountFolderManagement: () => void;

    openUpgradePlan: () => void;
    closeUpgradePlan: () => void;
    unmountUpgradePlan: () => void;

    openSourcesManagement: () => void;
    closeSourcesManagement: () => void;
    unmountSourcesManagement: () => void;

    openPaymentMethod: () => void;
    closePaymentMethod: () => void;
    unmountPaymentMethod: () => void;

    openSubscriptionManage: () => void;
    closeSubscriptionManage: () => void;
    unmountSubscriptionManage: () => void;

    openPaymentHistory: () => void;
    closePaymentHistory: () => void;
    unmountPaymentHistory: () => void;
}

export const useUiStore = create<UiState>((set) => ({
    isSearchMounted: false,
    isSearchOpen: false,
    isNotificationsMounted: false,
    isNotificationsOpen: false,
    isFolderMounted: false,
    isFolderOpen: false,
    isUpgradeMounted: false,
    isUpgradeOpen: false,
    isSourcesMounted: false,
    isSourcesOpen: false,
    isPaymentMethodMounted: false,
    isPaymentMethodOpen: false,
    isSubscriptionMounted: false,
    isSubscriptionOpen: false,
    isPaymentHistoryMounted: false,
    isPaymentHistoryOpen: false,

    openSearch: () => set({ isSearchMounted: true, isSearchOpen: true }),
    closeSearch: () => set({ isSearchOpen: false }),
    unmountSearch: () => set({ isSearchMounted: false }),

    openNotifications: () => set({ isNotificationsMounted: true, isNotificationsOpen: true }),
    closeNotifications: () => set({ isNotificationsOpen: false }),
    unmountNotifications: () => set({ isNotificationsMounted: false }),

    openFolderManagement: () => set({ isFolderMounted: true, isFolderOpen: true }),
    closeFolderManagement: () => set({ isFolderOpen: false }),
    unmountFolderManagement: () => set({ isFolderMounted: false }),

    openUpgradePlan: () => set({ isUpgradeMounted: true, isUpgradeOpen: true }),
    closeUpgradePlan: () => set({ isUpgradeOpen: false }),
    unmountUpgradePlan: () => set({ isUpgradeMounted: false }),

    openSourcesManagement: () => set({ isSourcesMounted: true, isSourcesOpen: true }),
    closeSourcesManagement: () => set({ isSourcesOpen: false }),
    unmountSourcesManagement: () => set({ isSourcesMounted: false }),

    openPaymentMethod: () => set({ isPaymentMethodMounted: true, isPaymentMethodOpen: true }),
    closePaymentMethod: () => set({ isPaymentMethodOpen: false }),
    unmountPaymentMethod: () => set({ isPaymentMethodMounted: false }),

    openSubscriptionManage: () => set({ isSubscriptionMounted: true, isSubscriptionOpen: true }),
    closeSubscriptionManage: () => set({ isSubscriptionOpen: false }),
    unmountSubscriptionManage: () => set({ isSubscriptionMounted: false }),

    openPaymentHistory: () => set({ isPaymentHistoryMounted: true, isPaymentHistoryOpen: true }),
    closePaymentHistory: () => set({ isPaymentHistoryOpen: false }),
    unmountPaymentHistory: () => set({ isPaymentHistoryMounted: false }),
}));
