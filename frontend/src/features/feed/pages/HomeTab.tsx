import { PostCard } from '@/features/feed/components/PostCard';
import type { Post } from '@/types';
import { useState } from 'react';
import { PostDetailOverlay } from '@/features/feed/components/PostDetailOverlay';
import { useFeed } from '../api/feedApi';
import { useIntersectionObserver } from '@/hooks/useIntersectionObserver';
import { Loader2 } from 'lucide-react';

export function HomeTab() {
    const [selectedPost, setSelectedPost] = useState<Post | null>(null);

    const { data, fetchNextPage, hasNextPage, isFetchingNextPage, status } = useFeed();

    const { targetRef } = useIntersectionObserver({
        onIntersect: fetchNextPage,
        enabled: hasNextPage && !isFetchingNextPage,
    });

    const handlePostClick = (post: Post) => {
        setSelectedPost(post);
    };

    // Flatten pages to a single array of items and map to Post interface
    const posts: Post[] = data?.pages.flatMap((page) =>
        page.content.map((item) => ({
            id: item.contentId, // Using contentId as id
            company: item.sourceName,
            logo: item.thumbnailUrl || '/favicon.ico', // provide fallback
            title: item.title,
            excerpt: item.summary,
            date: item.publishedAt,
            category: 'Tech', // Fallback or derived
            tags: [], // Fallback
            color: 'bg-indigo-500', // Fallback
            readTime: '3 min read', // Fallback
            originalUrl: item.originalUrl,
            bookmarkId: item.bookmarkId,
        }))
    ) || [];

    return (
        <>
            <div className="px-5 pt-4 pb-24">
                {status === 'pending' ? (
                    <div className="flex justify-center items-center py-10">
                        <Loader2 className="w-6 h-6 animate-spin text-slate-400" />
                    </div>
                ) : status === 'error' ? (
                    <div className="text-center py-10 text-slate-500">
                        피드를 불러오는 데 실패했습니다.
                    </div>
                ) : (
                    <div className="space-y-4">
                        {posts.map((post) => (
                            <PostCard
                                key={post.id}
                                post={post}
                                onClick={handlePostClick}
                            />
                        ))}
                    </div>
                )}

                {/* Infinite Scroll trigger area */}
                <div ref={targetRef} className="h-10 mt-4 flex items-center justify-center">
                    {isFetchingNextPage && (
                        <Loader2 className="w-5 h-5 animate-spin text-slate-400" />
                    )}
                </div>
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
