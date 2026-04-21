import { useState, useMemo, useRef, useEffect } from 'react';
import gsap from 'gsap';
import { useGSAP } from '@gsap/react';
import { ArrowLeft, Search, X, ChevronRight, SearchX, Loader2 } from 'lucide-react';
import { useUiStore } from '@/stores/uiStore';
import { TRENDING_KEYWORDS } from '@/lib/mock';
import type { Post } from '@/types';
import { PostDetailOverlay } from '@/features/feed/components/PostDetailOverlay';
import { useFeed } from '@/features/feed/api/feedApi';
import { useIntersectionObserver } from '@/hooks/useIntersectionObserver';

export function SearchOverlay() {
    const { isSearchOpen, closeSearch, unmountSearch } = useUiStore();
    const overlayRef = useRef<HTMLDivElement>(null);
    const inputRef = useRef<HTMLInputElement>(null);
    const { contextSafe } = useGSAP({ scope: overlayRef });

    const [searchQuery, setSearchQuery] = useState("");
    const [isSearching, setIsSearching] = useState(false);
    const [selectedPost, setSelectedPost] = useState<Post | null>(null);

    const {
        data,
        fetchNextPage,
        hasNextPage,
        isFetchingNextPage,
        status
    } = useFeed({ keyword: searchQuery, enabled: isSearching && searchQuery.trim().length > 0 });

    const observerTarget = useIntersectionObserver({
        onIntersect: fetchNextPage,
        enabled: hasNextPage && !isFetchingNextPage,
    });

    const searchResults: Post[] = useMemo(() => {
        if (!data || !isSearching) return [];
        return data.pages.flatMap((page) =>
            page.content.map((item) => ({
                id: item.contentId,
                company: item.sourceName,
                logo: item.thumbnailUrl || '/favicon.ico',
                title: item.title,
                excerpt: item.summary,
                date: new Date(item.publishedAt).toLocaleDateString(),
                category: 'Tech',
                tags: [],
                color: 'bg-indigo-500',
                readTime: '',
                originalUrl: item.originalUrl,
                bookmarkId: item.bookmarkId,
            }))
        );
    }, [data, isSearching]);

    const handleSearchTrigger = (query: string) => {
        if (query.trim()) {
            setSearchQuery(query);
            setIsSearching(true);
        }
    };

    const handlePostClick = (post: Post) => {
        setSelectedPost(post);
    };

    const handleBack = () => {
        if (isSearching) {
            setIsSearching(false);
        } else {
            closeSearch();
            setSearchQuery("");
        }
    };

    const handleClearSearch = () => {
        setSearchQuery("");
        setIsSearching(false);
    };

    useEffect(() => {
        contextSafe(() => {
            if (isSearchOpen) {
                gsap.to(overlayRef.current, { 
                    y: 0, 
                    opacity: 1, 
                    duration: 0.4, 
                    ease: "power3.out",
                    onComplete: () => {
                        inputRef.current?.focus();
                    }
                });
            } else {
                gsap.to(overlayRef.current, {
                    y: "100%",
                    opacity: 0,
                    duration: 0.3,
                    ease: "power2.in",
                    onComplete: unmountSearch
                });
            }
        })();
    }, [isSearchOpen, unmountSearch, contextSafe]);

    return (
        <>
            <div
                ref={overlayRef}
                className="absolute inset-0 z-[100] bg-white flex justify-center translate-y-full opacity-0"
            >
                <div className="w-full max-w-[480px] flex flex-col px-6 pt-10">
                    <div className="flex items-center gap-3 mb-6">
                        <button
                            onClick={handleBack}
                            className="p-2 bg-slate-50 border border-slate-100 rounded-full text-slate-600 shadow-sm active:scale-90 transition-transform"
                        >
                            <ArrowLeft size={20} />
                        </button>
                        <div className="flex-1 relative">
                            <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
                            <input
                                ref={inputRef}
                                type="text"
                                value={searchQuery}
                                onChange={(e) => {
                                    setSearchQuery(e.target.value);
                                    if (isSearching) setIsSearching(false);
                                }}
                                onKeyDown={(e) => e.key === 'Enter' && handleSearchTrigger(searchQuery)}
                                placeholder="관심 있는 키워드 검색"
                                className="w-full bg-slate-50 border border-slate-100 rounded-2xl py-3 pl-11 pr-4 text-sm font-bold focus:ring-4 focus:ring-slate-100 outline-none transition-all placeholder:text-slate-400"
                            />
                            {searchQuery && (
                                <button
                                    onClick={handleClearSearch}
                                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-300 hover:text-slate-500"
                                >
                                    <X size={18} />
                                </button>
                            )}
                        </div>
                    </div>

                    <div className="flex-1 overflow-y-auto no-scrollbar pb-10">
                        {!isSearching ? (
                            <div className="animate-in fade-in slide-in-from-bottom-2 duration-300">
                                <div className="mb-6">
                                    <h4 className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-3 px-1">
                                        최근 검색어
                                    </h4>
                                    <div className="flex flex-wrap gap-2">
                                        {["React", "Node.js", "DeepSeek", "Toss"].map(tag => (
                                            <button
                                                key={tag}
                                                onClick={() => handleSearchTrigger(tag)}
                                                className="px-4 py-2 bg-slate-50 border border-slate-100 rounded-xl text-xs font-bold text-slate-600 hover:bg-slate-100 transition-colors"
                                            >
                                                {tag}
                                            </button>
                                        ))}
                                    </div>
                                </div>
                                <div>
                                    <h4 className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-3 px-1">
                                        추천 트렌드
                                    </h4>
                                    <div className="space-y-2">
                                        {TRENDING_KEYWORDS.slice(0, 3).map((item) => (
                                            <div
                                                key={item.keyword}
                                                onClick={() => handleSearchTrigger(item.keyword)}
                                                className="flex items-center justify-between p-4 bg-slate-50 border border-slate-100 rounded-2xl hover:bg-slate-100 transition-all cursor-pointer group"
                                            >
                                                <span className="text-xs font-bold text-slate-700 group-hover:text-slate-900">
                                                    {item.keyword}
                                                </span>
                                                <ChevronRight size={14} className="text-slate-300" />
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            </div>
                        ) : (
                            <div className="animate-in fade-in slide-in-from-bottom-2 duration-300">
                                <div className="mb-4 px-1">
                                    <h3 className="text-[10px] font-black text-slate-400 uppercase tracking-widest">
                                        검색 결과
                                    </h3>
                                </div>

                                {status === 'pending' ? (
                                    <div className="flex justify-center py-20">
                                        <Loader2 className="w-8 h-8 animate-spin text-slate-300" />
                                    </div>
                                ) : status === 'error' ? (
                                    <div className="flex flex-col items-center justify-center py-20 opacity-40">
                                        <SearchX size={48} className="mb-4" strokeWidth={1.5} />
                                        <p className="text-[11px] font-black uppercase tracking-widest">
                                            검색 중 오류가 발생했습니다
                                        </p>
                                    </div>
                                ) : searchResults.length > 0 ? (
                                    <div className="space-y-4">
                                        {searchResults.map((post) => (
                                            <div
                                                key={post.id}
                                                onClick={() => handlePostClick(post)}
                                                className="bg-white border border-slate-100 p-4 rounded-3xl shadow-sm hover:border-indigo-100 transition-all cursor-pointer group flex gap-3"
                                            >
                                                <div className="flex-1">
                                                    <div className="flex items-center gap-2 mb-1.5">
                                                        <img src={post.logo} alt="" className="w-4 h-4 object-contain opacity-60" />
                                                        <span className="text-[10px] font-bold text-slate-400 uppercase">
                                                            {post.company}
                                                        </span>
                                                    </div>
                                                    <h4 className="text-sm font-bold text-slate-800 leading-tight mb-1 group-hover:text-indigo-600 transition-colors">
                                                        {post.title}
                                                    </h4>
                                                    <p className="text-[11px] text-slate-500 line-clamp-1 opacity-80">
                                                        {post.excerpt}
                                                    </p>
                                                </div>
                                                {post.thumbnail && (
                                                    <div className="w-16 h-16 shrink-0 rounded-2xl overflow-hidden shadow-inner bg-slate-50 border border-slate-100">
                                                        <img src={post.thumbnail} alt="" className="w-full h-full object-cover" />
                                                    </div>
                                                )}
                                            </div>
                                        ))}
                                        <div ref={observerTarget.targetRef} className="h-10 flex items-center justify-center">
                                            {isFetchingNextPage && <Loader2 className="w-5 h-5 animate-spin text-slate-400" />}
                                        </div>
                                    </div>
                                ) : (
                                    <div className="flex flex-col items-center justify-center py-20 opacity-40">
                                        <SearchX size={48} className="mb-4" strokeWidth={1.5} />
                                        <p className="text-[11px] font-black uppercase tracking-widest">
                                            검색 결과가 없습니다
                                        </p>
                                    </div>
                                )}
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {/* Search also shares the PostDetail logic when a result is clicked */}
            {selectedPost && (
                <PostDetailOverlay
                    post={selectedPost}
                    onClose={() => setSelectedPost(null)}
                />
            )}
        </>
    );
}
