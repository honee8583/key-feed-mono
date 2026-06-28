import { useState, useMemo } from 'react';
import { Bookmark, SlidersHorizontal, Loader2 } from 'lucide-react';
import { useFolderStore } from '@/stores/folderStore';
import { useUiStore } from '@/stores/uiStore';
import { PostCard } from '@/features/feed/components/PostCard';
import { PostDetailOverlay } from '@/features/feed/components/PostDetailOverlay';
import { cn } from '@/utils/cn';
import type { Post } from '@/types';
import { useBookmarks, useBookmarkFolders } from '../api/bookmarkApi';
import type { BookmarkItem } from '../types';
import { useIntersectionObserver } from '@/hooks/useIntersectionObserver';

function transformBookmarkToPost(item: BookmarkItem): Post {
    return {
        id: item.content.contentId,
        company: item.content.sourceName,
        logo: item.content.thumbnailUrl || '',
        title: item.content.title,
        excerpt: item.content.summary,
        date: item.content.publishedAt,
        category: '',
        tags: [],
        color: '',
        readTime: '',
        content: '',
        thumbnail: item.content.thumbnailUrl,
        folder: item.folderName,
        bookmarkId: item.bookmarkId,
        originalUrl: item.content.originalUrl,
    };
}

export function SavedTab() {
    const { activeFolder, setActiveFolder } = useFolderStore();
    const { openFolderManagement } = useUiStore();

    const [selectedPost, setSelectedPost] = useState<Post | null>(null);

    const { data: folderListResponse } = useBookmarkFolders();
    const fetchedFolders = folderListResponse || [];

    const { data, fetchNextPage, hasNextPage, isFetchingNextPage, status } = useBookmarks(undefined, 20);

    const { targetRef } = useIntersectionObserver({
        onIntersect: fetchNextPage,
        enabled: hasNextPage && !isFetchingNextPage,
    });

    const allItems = useMemo(
        () => (data ? data.pages.flatMap((page) => page.content || []).filter((it) => it && it.content) : []),
        [data]
    );

    // 불러온 북마크 기준 폴더별 개수(페이지네이션 특성상 근사치).
    const counts = useMemo(() => {
        const map: Record<string, number> = {};
        for (const item of allItems) {
            if (item.folderName) map[item.folderName] = (map[item.folderName] ?? 0) + 1;
        }
        return map;
    }, [allItems]);

    const savedPosts = useMemo(
        () =>
            allItems
                .filter((item) => activeFolder === '전체' || item.folderName === activeFolder)
                .map(transformBookmarkToPost),
        [allItems, activeFolder]
    );

    return (
        <>
            <div className="px-5 pb-24 pt-2">
                {/* Header */}
                <div className="flex items-center justify-between pt-3">
                    <h1 className="font-display text-[30px] font-medium tracking-tightest text-primary">북마크</h1>
                    <button
                        onClick={openFolderManagement}
                        aria-label="폴더 관리"
                        className="flex h-[38px] w-[38px] items-center justify-center rounded-full text-ink transition-colors hover:bg-soft-stone active:bg-soft-stone"
                    >
                        <SlidersHorizontal size={19} strokeWidth={1.7} />
                    </button>
                </div>

                {/* Folder filter chips */}
                <div className="no-scrollbar -mx-5 mt-3.5 flex gap-2 overflow-x-auto px-5 pb-3.5">
                    {[{ name: '전체' }, ...fetchedFolders].map((f) => {
                        const name = f.name;
                        const isActive = activeFolder === name;
                        const count = name === '전체' ? allItems.length : counts[name] ?? 0;

                        return (
                            <button
                                key={name}
                                onClick={() => setActiveFolder(name)}
                                className={cn(
                                    'flex shrink-0 items-center gap-1.5 whitespace-nowrap rounded-full border px-3.5 py-[7px] text-[13px] font-medium transition-colors',
                                    isActive
                                        ? 'border-primary bg-primary text-white'
                                        : 'border-[#f2f2f2] bg-[#f7f7f8] text-[#75758a] hover:bg-[#f0f0f2]'
                                )}
                            >
                                {name}
                                <span className={cn('font-mono text-[11px]', isActive ? 'text-white/60' : 'text-muted')}>
                                    {count}
                                </span>
                            </button>
                        );
                    })}
                </div>

                {/* Sub label */}
                <div className="py-2 font-mono text-[11px] tracking-[0.6px] text-muted">
                    {activeFolder} · {savedPosts.length}
                </div>

                {/* Saved list */}
                {status === 'pending' ? (
                    <div className="flex justify-center py-16">
                        <Loader2 className="h-6 w-6 animate-spin text-muted" />
                    </div>
                ) : status === 'error' ? (
                    <div className="py-20 text-center text-[14px] text-muted">북마크를 불러오지 못했어요.</div>
                ) : savedPosts.length > 0 ? (
                    <div>
                        {savedPosts.map((post) => (
                            <PostCard key={post.id} post={post} onClick={setSelectedPost} />
                        ))}
                        <div ref={targetRef} className="flex h-10 items-center justify-center">
                            {isFetchingNextPage && <Loader2 className="h-5 w-5 animate-spin text-muted" />}
                        </div>
                    </div>
                ) : (
                    <div className="py-24 text-center text-muted">
                        <Bookmark size={34} strokeWidth={1.5} className="mx-auto mb-4 text-[#c4c4cc]" />
                        <div className="mb-2 font-mono text-[12px] tracking-[0.5px]">NO BOOKMARKS</div>
                        <div className="text-[15px] leading-[1.5]">
                            저장된 글이 없어요.
                            <br />
                            피드에서 북마크 아이콘을 눌러 저장해보세요.
                        </div>
                    </div>
                )}
            </div>

            {selectedPost && <PostDetailOverlay post={selectedPost} onClose={() => setSelectedPost(null)} />}
        </>
    );
}
