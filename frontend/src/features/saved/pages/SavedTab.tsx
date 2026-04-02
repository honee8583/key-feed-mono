import { useState, useMemo } from 'react';
import { Bookmark, FolderKanban, Loader2 } from 'lucide-react';
import { usePostStore } from '@/stores/postStore';
import { useFolderStore } from '@/stores/folderStore';
import { useUiStore } from '@/stores/uiStore';
import { PostCard } from '@/features/feed/components/PostCard';
import { PostDetailOverlay } from '@/features/feed/components/PostDetailOverlay';
import { ICON_MAP, AVAILABLE_COLORS } from '@/utils/constants';
import type { Post } from '@/types';
import { useBookmarks, useBookmarkFolders } from '../api/bookmarkApi';
import type { BookmarkItem } from '../types';
import { useIntersectionObserver } from '@/hooks/useIntersectionObserver';

function transformBookmarkToPost(item: BookmarkItem): Post {
    return {
        id: item.content.contentId,
        company: item.content.sourceName,
        logo: '/favicon.ico', // 기본 로고 폴백
        title: item.content.title,
        excerpt: item.content.summary,
        date: new Date(item.content.publishedAt).toLocaleDateString(),
        category: item.folderName || 'Uncategorized',
        tags: [],
        color: 'bg-slate-100', // 기본 색상
        readTime: '',
        content: '', // 원문 링크로 나가는 경우가 많으므로 비워둠
        thumbnail: item.content.thumbnailUrl,
        folder: item.folderName,
        bookmarkId: item.bookmarkId,
        originalUrl: item.content.originalUrl
    };
}

export function SavedTab() {
    const { markAsRead } = usePostStore();
    const { activeFolder, setActiveFolder } = useFolderStore();
    const { openFolderManagement } = useUiStore();

    const [selectedPost, setSelectedPost] = useState<Post | null>(null);

    const { data: folderListResponse } = useBookmarkFolders();
    const fetchedFolders = folderListResponse || [];

    // API 연동: 전체 폴더("전체")면 folderId를 undefined로, 아니면 해당 폴더 ID(현재는 Mock UI 구조상 activeFolder가 string이므로 Name 매칭 혹은 전체조회)
    // TODO: 완벽한 폴더 연동 시 activeFolder를 객체나 ID 기반으로 통일 필요. 일단은 전체 조회로 적용
    const {
        data,
        fetchNextPage,
        hasNextPage,
        isFetchingNextPage,
        status
    } = useBookmarks(undefined, 20);

    const observerTarget = useIntersectionObserver({
        onIntersect: fetchNextPage,
        enabled: hasNextPage && !isFetchingNextPage,
    });

    const savedPosts = useMemo(() => {
        if (!data) return [];
        const allItems = data.pages.flatMap(page => page.content || []);

        // 프론트엔드 임시 필터: activeFolder 기반 (추후 folderId 기반 쿼리로 개선 가능)
        const filteredItems = allItems.filter(item => {
            if (!item || !item.content) return false;
            return activeFolder === "전체" || item.folderName === activeFolder;
        });

        // Adapter 적용
        return filteredItems.map(transformBookmarkToPost);
    }, [data, activeFolder]);

    const handlePostClick = (post: Post) => {
        setSelectedPost(post);
        markAsRead(post.id);
    };

    return (
        <>
            <div className="px-5 pt-2 pb-24">
                <div className="flex items-end justify-between mb-4 px-1">
                    <div>
                        <h3 className="text-lg font-black text-slate-800 leading-none uppercase tracking-tighter">
                            북마크
                        </h3>
                        <p className="text-[8px] text-slate-400 font-black uppercase tracking-widest mt-1">
                            {savedPosts.length} Items in {activeFolder}
                        </p>
                    </div>
                    <button
                        onClick={openFolderManagement}
                        className="p-2 bg-white/60 border border-white/60 rounded-xl text-slate-500 hover:text-slate-800 transition-colors shadow-sm active:scale-90 transition-transform"
                    >
                        <FolderKanban size={18} />
                    </button>
                </div>

                {/* Folder Tabs */}
                <div className="flex gap-1.5 overflow-x-auto pb-4 no-scrollbar mb-2">
                    {[{ name: "전체" } as any, ...fetchedFolders].map((f) => {
                        const name = f.name;
                        const isAll = name === "전체";
                        
                        const isHexColor = typeof f.color === 'string' && f.color.startsWith('#');
                        const IconComponent = f.icon ? ICON_MAP[f.icon as keyof typeof ICON_MAP] : null;
                        const isActive = activeFolder === name;
                        
                        let buttonClassName = "px-4 py-1.5 rounded-xl whitespace-nowrap text-[9px] font-black transition-all border ";
                        let style = {};

                        if (isActive) {
                            if (isHexColor) {
                                buttonClassName += "text-white border-transparent shadow-md";
                                style = { backgroundColor: f.color };
                            } else {
                                const config = AVAILABLE_COLORS.find(c => c.name === f.color);
                                buttonClassName += (config ? `${config.bg} text-white border-transparent shadow-md` : "bg-indigo-600 text-white border-transparent shadow-md");
                            }
                        } else {
                            buttonClassName += "bg-white/40 text-slate-400 border-white/40";
                        }

                        return (
                            <button
                                key={name}
                                onClick={() => setActiveFolder(name)}
                                className={buttonClassName}
                                style={style}
                            >
                                <div className="flex items-center gap-1.5">
                                    {!isAll && (IconComponent ? <IconComponent size={10} /> : f.icon ? <span>{f.icon}</span> : <ICON_MAP.Folder size={10} />)}
                                    {name}
                                </div>
                            </button>
                        );
                    })}
                </div>

                {/* Saved Posts List */}
                {status === 'pending' ? (
                    <div className="flex justify-center py-20">
                        <Loader2 className="w-8 h-8 animate-spin text-slate-300" />
                    </div>
                ) : status === 'error' ? (
                    <div className="flex flex-col items-center justify-center py-20 text-center opacity-50">
                        <Bookmark size={32} className="mb-3 text-slate-400" />
                        <p className="text-[12px] font-bold text-slate-500">
                            북마크를 불러오는데 실패했습니다.
                        </p>
                    </div>
                ) : savedPosts.length > 0 ? (
                    <div className="space-y-3">
                        {savedPosts.map((post) => (
                            <PostCard
                                key={post.id}
                                post={post}
                                onClick={handlePostClick}
                            />
                        ))}
                        {/* 무한 스크롤 옵저버 타겟 */}
                        <div ref={observerTarget.targetRef} className="h-10 flex items-center justify-center">
                            {isFetchingNextPage && <Loader2 className="w-5 h-5 animate-spin text-slate-400" />}
                        </div>
                    </div>
                ) : (
                    <div className="flex flex-col items-center justify-center py-20 text-center opacity-30">
                        <Bookmark size={32} className="mb-3 text-slate-400" />
                        <p className="text-[10px] font-black uppercase text-slate-500">
                            저장된 콘텐츠가 없습니다
                        </p>
                    </div>
                )}
            </div>

            {/* Detail Overlay */}
            {selectedPost && (
                <PostDetailOverlay
                    post={selectedPost}
                    onClose={() => setSelectedPost(null)}
                />
            )}
        </>
    );
}
