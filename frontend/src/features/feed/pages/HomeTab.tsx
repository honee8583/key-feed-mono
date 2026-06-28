import { useState } from 'react';
import { Loader2 } from 'lucide-react';
import type { Post } from '@/types';
import { PostCard } from '@/features/feed/components/PostCard';
import { FeaturedPostCard } from '@/features/feed/components/FeaturedPostCard';
import { PostDetailOverlay } from '@/features/feed/components/PostDetailOverlay';
import { useIntersectionObserver } from '@/hooks/useIntersectionObserver';
import { formatTodayLabel } from '@/utils/time';
import { useFeed } from '../api/feedApi';

export function HomeTab() {
    const [selectedPost, setSelectedPost] = useState<Post | null>(null);

    const { data, fetchNextPage, hasNextPage, isFetchingNextPage, status } = useFeed();

    const { targetRef } = useIntersectionObserver({
        onIntersect: fetchNextPage,
        enabled: hasNextPage && !isFetchingNextPage,
    });

    // Map the API feed items onto the shared Post shape. Fields the API does not
    // provide (category, tags, …) are intentionally left empty — the redesigned
    // feed cards don't render them.
    const posts: Post[] = data?.pages.flatMap((page) =>
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
    ) ?? [];

    const [featured, ...rest] = posts;

    return (
        <>
            <div className="px-5 pb-24">
                {/* Header */}
                <div className="pb-4 pt-5">
                    <p className="mb-1 font-mono text-[11px] tracking-[0.6px] text-muted">
                        오늘의 피드 · {formatTodayLabel()}
                    </p>
                    <h1 className="font-display text-[27px] font-medium tracking-[-0.5px] text-primary">
                        새로 올라온 글
                    </h1>
                </div>

                {/* Feed */}
                {status === 'pending' ? (
                    <div className="flex items-center justify-center py-16">
                        <Loader2 className="h-6 w-6 animate-spin text-muted" />
                    </div>
                ) : status === 'error' ? (
                    <div className="py-16 text-center text-[14px] text-muted">
                        피드를 불러오지 못했어요.
                    </div>
                ) : posts.length === 0 ? (
                    <div className="py-16 text-center text-[14px] text-muted">
                        아직 새로 올라온 글이 없어요.
                    </div>
                ) : (
                    <>
                        <FeaturedPostCard post={featured} onClick={setSelectedPost} />

                        <p className="mb-1.5 font-mono text-[11px] tracking-[0.6px] text-muted">
                            최신 글
                        </p>
                        <div>
                            {rest.map((post) => (
                                <PostCard key={post.id} post={post} onClick={setSelectedPost} />
                            ))}
                        </div>
                    </>
                )}

                {/* Infinite scroll trigger */}
                <div ref={targetRef} className="mt-4 flex h-10 items-center justify-center">
                    {isFetchingNextPage && <Loader2 className="h-5 w-5 animate-spin text-muted" />}
                </div>
            </div>

            {selectedPost && (
                <PostDetailOverlay
                    post={selectedPost}
                    onClose={() => setSelectedPost(null)}
                />
            )}
        </>
    );
}
