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
}));
