import { create } from 'zustand';
import type { Post } from '@/types';
import { INITIAL_POSTS } from '@/lib/mock';

interface PostState {
    posts: Post[];
    savedPostIds: (string | number)[];
    readPostIds: (string | number)[];

    toggleSave: (id: string | number) => void;
    markAsRead: (id: string | number) => void;
    setPosts: (posts: Post[]) => void;
}

export const usePostStore = create<PostState>((set) => ({
    posts: INITIAL_POSTS,
    savedPostIds: [1, 2, 3, 4], // Default mocked saved items from base.txt
    readPostIds: [],

    toggleSave: (id) => set((state) => ({
        savedPostIds: state.savedPostIds.includes(id)
            ? state.savedPostIds.filter(postId => postId !== id)
            : [...state.savedPostIds, id]
    })),

    markAsRead: (id) => set((state) => ({
        readPostIds: state.readPostIds.includes(id)
            ? state.readPostIds
            : [...state.readPostIds, id]
    })),

    setPosts: (posts) => set({ posts })
}));
