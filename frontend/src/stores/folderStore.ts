import { create } from 'zustand';
import type { FolderConfig } from '@/types';
import { usePostStore } from './postStore';

interface FolderState {
    folders: FolderConfig[];
    activeFolder: string;
    addFolder: (folder: FolderConfig) => void;
    updateFolder: (oldName: string, newFolder: FolderConfig) => void;
    deleteFolder: (name: string) => void;
    setActiveFolder: (name: string) => void;
}

export const useFolderStore = create<FolderState>((set) => ({
    folders: [
        { name: "나중에 읽을 글", icon: "Clock", color: "blue" },
        { name: "프로젝트", icon: "Briefcase", color: "purple" },
        { name: "학습", icon: "Book", color: "emerald" }
    ],
    activeFolder: "전체",

    addFolder: (folder) => set((state) => {
        if (state.folders.find(f => f.name === folder.name)) return state;
        return { folders: [...state.folders, folder] };
    }),

    updateFolder: (oldName, newFolder) => set((state) => {
        // We also need to update posts that reference this folder. We do it imperatively here.
        const postStore = usePostStore.getState();
        const updatedPosts = postStore.posts.map(p =>
            p.folder === oldName ? { ...p, folder: newFolder.name } : p
        );
        postStore.setPosts(updatedPosts);

        return {
            folders: state.folders.map(f => f.name === oldName ? newFolder : f),
            activeFolder: state.activeFolder === oldName ? newFolder.name : state.activeFolder
        };
    }),

    deleteFolder: (name) => set((state) => {
        const postStore = usePostStore.getState();
        const updatedPosts = postStore.posts.map(p =>
            p.folder === name ? { ...p, folder: null } : p
        );
        postStore.setPosts(updatedPosts);

        return {
            folders: state.folders.filter(f => f.name !== name),
            activeFolder: state.activeFolder === name ? "전체" : state.activeFolder
        };
    }),

    setActiveFolder: (name) => set({ activeFolder: name })
}));
