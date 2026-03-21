import { create } from 'zustand';

export interface UiState {
    isSearchMounted: boolean;
    isSearchOpen: boolean;
    isNotificationsMounted: boolean;
    isNotificationsOpen: boolean;
    isFolderMounted: boolean;
    isFolderOpen: boolean;

    openSearch: () => void;
    closeSearch: () => void;
    unmountSearch: () => void;

    openNotifications: () => void;
    closeNotifications: () => void;
    unmountNotifications: () => void;

    openFolderManagement: () => void;
    closeFolderManagement: () => void;
    unmountFolderManagement: () => void;
}

export const useUiStore = create<UiState>((set) => ({
    isSearchMounted: false,
    isSearchOpen: false,
    isNotificationsMounted: false,
    isNotificationsOpen: false,
    isFolderMounted: false,
    isFolderOpen: false,

    openSearch: () => set({ isSearchMounted: true, isSearchOpen: true }),
    closeSearch: () => set({ isSearchOpen: false }),
    unmountSearch: () => set({ isSearchMounted: false }),

    openNotifications: () => set({ isNotificationsMounted: true, isNotificationsOpen: true }),
    closeNotifications: () => set({ isNotificationsOpen: false }),
    unmountNotifications: () => set({ isNotificationsMounted: false }),

    openFolderManagement: () => set({ isFolderMounted: true, isFolderOpen: true }),
    closeFolderManagement: () => set({ isFolderOpen: false }),
    unmountFolderManagement: () => set({ isFolderMounted: false }),
}));
