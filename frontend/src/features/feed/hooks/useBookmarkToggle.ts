import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import type { Post } from '@/types';
import { useCreateBookmark, useDeleteBookmark } from '@/features/saved/api/bookmarkApi';

interface UseBookmarkToggle {
    saved: boolean;
    toggle: (e: React.MouseEvent) => void;
}

/**
 * Optimistic bookmark toggle shared by the feed cards.
 * Reverts the local state if the mutation fails.
 */
export function useBookmarkToggle(post: Post): UseBookmarkToggle {
    const queryClient = useQueryClient();
    const createBookmark = useCreateBookmark();
    const deleteBookmark = useDeleteBookmark();

    const [saved, setSaved] = useState(post.bookmarkId != null);
    // Re-sync the optimistic state when the server value for this post changes,
    // adjusting during render (React's recommended alternative to an effect).
    const [prevBookmarkId, setPrevBookmarkId] = useState(post.bookmarkId);
    if (post.bookmarkId !== prevBookmarkId) {
        setPrevBookmarkId(post.bookmarkId);
        setSaved(post.bookmarkId != null);
    }

    const invalidate = () => {
        queryClient.invalidateQueries({ queryKey: ['feed'] });
        queryClient.invalidateQueries({ queryKey: ['bookmarks'] });
    };

    const toggle = (e: React.MouseEvent) => {
        e.stopPropagation();

        if (saved) {
            setSaved(false);
            if (post.bookmarkId) {
                deleteBookmark.mutate(post.bookmarkId, {
                    onSuccess: invalidate,
                    onError: () => setSaved(true),
                });
            }
        } else {
            setSaved(true);
            createBookmark.mutate(String(post.id), {
                onSuccess: invalidate,
                onError: () => setSaved(false),
            });
        }
    };

    return { saved, toggle };
}
