import { useState, useMemo, useRef, useEffect } from 'react';
import gsap from 'gsap';
import { useGSAP } from '@gsap/react';
import { Search, X, Loader2 } from 'lucide-react';
import { useUiStore } from '@/stores/uiStore';
import { TRENDING_KEYWORDS } from '@/lib/mock';
import type { Post } from '@/types';
import { PostCard } from '@/features/feed/components/PostCard';
import { PostDetailOverlay } from '@/features/feed/components/PostDetailOverlay';
import { useFeed } from '@/features/feed/api/feedApi';
import { useIntersectionObserver } from '@/hooks/useIntersectionObserver';
import { cn } from '@/utils/cn';
import { useRecentSearches } from '../hooks/useRecentSearches';

export function SearchOverlay() {
    const { isSearchOpen, closeSearch, unmountSearch } = useUiStore();
    const overlayRef = useRef<HTMLDivElement>(null);
    const inputRef = useRef<HTMLInputElement>(null);
    const { contextSafe } = useGSAP({ scope: overlayRef });

    const [searchQuery, setSearchQuery] = useState('');
    const [committedQuery, setCommittedQuery] = useState('');
    const [selectedPost, setSelectedPost] = useState<Post | null>(null);

    const { recent, addRecent, clearRecent } = useRecentSearches();

    const isSearching = committedQuery.trim().length > 0;

    const { data, fetchNextPage, hasNextPage, isFetchingNextPage, status } = useFeed({
        keyword: committedQuery,
        enabled: isSearching,
    });

    const { targetRef } = useIntersectionObserver({
        onIntersect: fetchNextPage,
        enabled: hasNextPage && !isFetchingNextPage,
    });

    const searchResults: Post[] = useMemo(() => {
        if (!data || !isSearching) return [];
        return data.pages.flatMap((page) =>
            page.content.map((item) => ({
                id: item.contentId,
                company: item.sourceName,
                logo: item.thumbnailUrl || '',
                title: item.title,
                excerpt: item.summary,
                date: item.publishedAt,
                category: '',
                tags: [],
                color: '',
                readTime: '',
                originalUrl: item.originalUrl,
                bookmarkId: item.bookmarkId,
            }))
        );
    }, [data, isSearching]);

    const runSearch = (query: string) => {
        const trimmed = query.trim();
        if (!trimmed) return;
        setSearchQuery(trimmed);
        setCommittedQuery(trimmed);
        addRecent(trimmed);
    };

    const handleClear = () => {
        setSearchQuery('');
        setCommittedQuery('');
        inputRef.current?.focus();
    };

    const handleCancel = () => {
        closeSearch();
        setSearchQuery('');
        setCommittedQuery('');
    };

    useEffect(() => {
        contextSafe(() => {
            if (isSearchOpen) {
                gsap.to(overlayRef.current, {
                    y: 0,
                    opacity: 1,
                    duration: 0.4,
                    ease: 'power3.out',
                    onComplete: () => inputRef.current?.focus(),
                });
            } else {
                gsap.to(overlayRef.current, {
                    y: '100%',
                    opacity: 0,
                    duration: 0.3,
                    ease: 'power2.in',
                    onComplete: unmountSearch,
                });
            }
        })();
    }, [isSearchOpen, unmountSearch, contextSafe]);

    return (
        <>
            <div
                ref={overlayRef}
                className="absolute inset-0 z-[100] flex translate-y-full justify-center bg-canvas opacity-0"
            >
                <div className="flex w-full max-w-[480px] flex-col">
                    {/* search bar */}
                    <div className="flex items-center gap-3 px-5 pb-3.5 pt-14">
                        <div className="flex h-[46px] flex-1 items-center gap-[9px] rounded-lg border border-hairline px-[13px]">
                            <Search size={17} strokeWidth={1.8} className="shrink-0 text-muted" />
                            <input
                                ref={inputRef}
                                type="text"
                                value={searchQuery}
                                onChange={(e) => {
                                    setSearchQuery(e.target.value);
                                    if (e.target.value.trim() === '') setCommittedQuery('');
                                }}
                                onKeyDown={(e) => e.key === 'Enter' && runSearch(searchQuery)}
                                placeholder="블로그, 글, 주제 검색"
                                className="min-w-0 flex-1 bg-transparent text-[16px] text-ink outline-none placeholder:text-muted"
                            />
                            {searchQuery && (
                                <button
                                    type="button"
                                    aria-label="검색어 지우기"
                                    onClick={handleClear}
                                    className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-hairline text-canvas"
                                >
                                    <X size={12} strokeWidth={2.4} />
                                </button>
                            )}
                        </div>
                        <button
                            type="button"
                            onClick={handleCancel}
                            className="shrink-0 text-[15px] text-action-blue"
                        >
                            취소
                        </button>
                    </div>

                    {/* body */}
                    <div className="no-scrollbar flex-1 overflow-y-auto px-5 pb-8">
                        {!isSearching ? (
                            <>
                                {/* 최근 검색 */}
                                <div className="flex items-center justify-between py-2.5 pt-3.5">
                                    <span className="font-mono text-[11px] tracking-[0.6px] text-muted">
                                        최근 검색
                                    </span>
                                    {recent.length > 0 && (
                                        <button
                                            type="button"
                                            onClick={clearRecent}
                                            className="text-[13px] text-[#75758a]"
                                        >
                                            전체 삭제
                                        </button>
                                    )}
                                </div>
                                {recent.length > 0 ? (
                                    <div className="mb-7 flex flex-wrap gap-[9px]">
                                        {recent.map((keyword) => (
                                            <button
                                                key={keyword}
                                                type="button"
                                                onClick={() => runSearch(keyword)}
                                                className="rounded-full bg-[#f2f2f2] px-3.5 py-[7px] text-[14px] text-ink"
                                            >
                                                {keyword}
                                            </button>
                                        ))}
                                    </div>
                                ) : (
                                    <p className="pb-7 pt-0.5 text-[14px] text-muted">
                                        최근 검색 기록이 없어요.
                                    </p>
                                )}

                                {/* 인기 검색어 */}
                                <div className="pb-3.5 font-mono text-[11px] tracking-[0.6px] text-muted">
                                    인기 검색어
                                </div>
                                <div>
                                    {TRENDING_KEYWORDS.map((item) => (
                                        <button
                                            key={item.keyword}
                                            type="button"
                                            onClick={() => runSearch(item.keyword)}
                                            className="flex w-full items-center gap-3.5 border-b border-[#f2f2f2] py-3 text-left"
                                        >
                                            <span
                                                className={cn(
                                                    'w-[18px] font-mono text-[14px] font-bold',
                                                    item.rank <= 3 ? 'text-coral' : 'text-muted'
                                                )}
                                            >
                                                {item.rank}
                                            </span>
                                            <span className="flex-1 text-[16px] text-primary">
                                                {item.keyword}
                                            </span>
                                        </button>
                                    ))}
                                </div>
                            </>
                        ) : (
                            <>
                                <div className="py-3.5 pb-1.5 font-mono text-[11px] tracking-[0.6px] text-muted">
                                    검색 결과{searchResults.length > 0 ? ` ${searchResults.length}` : ''}
                                </div>

                                {status === 'pending' ? (
                                    <div className="flex justify-center py-16">
                                        <Loader2 className="h-6 w-6 animate-spin text-muted" />
                                    </div>
                                ) : status === 'error' ? (
                                    <div className="py-16 text-center text-[14px] text-muted">
                                        검색 중 오류가 발생했어요.
                                    </div>
                                ) : searchResults.length > 0 ? (
                                    <>
                                        <div>
                                            {searchResults.map((post) => (
                                                <PostCard key={post.id} post={post} onClick={setSelectedPost} />
                                            ))}
                                        </div>
                                        <div ref={targetRef} className="mt-4 flex h-10 items-center justify-center">
                                            {isFetchingNextPage && (
                                                <Loader2 className="h-5 w-5 animate-spin text-muted" />
                                            )}
                                        </div>
                                    </>
                                ) : (
                                    <div className="py-[70px] text-center text-muted">
                                        <div className="font-mono text-[12px] tracking-[0.5px]">NO RESULTS</div>
                                        <div className="mt-2 text-[15px]">
                                            ‘{committedQuery}’에 대한 결과가 없어요.
                                        </div>
                                    </div>
                                )}
                            </>
                        )}
                    </div>
                </div>
            </div>

            {selectedPost && (
                <PostDetailOverlay post={selectedPost} onClose={() => setSelectedPost(null)} />
            )}
        </>
    );
}
