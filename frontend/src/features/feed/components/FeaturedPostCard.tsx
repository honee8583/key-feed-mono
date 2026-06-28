import { useState } from 'react';
import { Bookmark, FolderOpen } from 'lucide-react';
import type { Post } from '@/types';
import { cn } from '@/utils/cn';
import { formatRelativeTime } from '@/utils/time';
import { useBookmarkToggle } from '../hooks/useBookmarkToggle';
import { FolderChangeOverlay } from './FolderChangeOverlay';

interface FeaturedPostCardProps {
    post: Post;
    onClick: (post: Post) => void;
}

/** Hero treatment for the top item of the feed — deep-green band over a white body. */
export function FeaturedPostCard({ post, onClick }: FeaturedPostCardProps) {
    const { saved, toggle } = useBookmarkToggle(post);
    const [isFolderOverlayOpen, setIsFolderOverlayOpen] = useState(false);

    return (
        <article
            onClick={() => onClick(post)}
            className="mb-6 cursor-pointer overflow-hidden rounded-[22px] border border-card-border"
        >
            <div className="bg-deep-green px-5 pb-[22px] pt-5">
                <div className="mb-[42px] flex items-center justify-between gap-3">
                    <span className="truncate font-mono text-[11px] tracking-[0.5px] text-coral-soft">
                        {post.company}
                    </span>
                    <div className="flex shrink-0 items-center gap-1">
                        {saved && (
                            <button
                                type="button"
                                aria-label="폴더 변경"
                                onClick={(e) => {
                                    e.stopPropagation();
                                    setIsFolderOverlayOpen(true);
                                }}
                                className="flex h-7 w-7 items-center justify-center rounded-full text-white/70 transition-colors hover:bg-white/10 hover:text-white"
                            >
                                <FolderOpen size={15} strokeWidth={1.8} />
                            </button>
                        )}
                        <button
                            type="button"
                            aria-label={saved ? '북마크 해제' : '북마크'}
                            onClick={toggle}
                            className={cn(
                                'flex h-7 w-7 items-center justify-center rounded-full transition-colors',
                                saved ? 'text-coral' : 'text-white/70 hover:bg-white/10 hover:text-white'
                            )}
                        >
                            <Bookmark size={15} strokeWidth={1.8} fill={saved ? 'currentColor' : 'none'} />
                        </button>
                    </div>
                </div>
                <h2 className="m-0 font-display text-[22px] font-medium leading-[1.22] tracking-[-0.3px] text-white">
                    {post.title}
                </h2>
            </div>

            <div className="px-5 pb-[18px] pt-4">
                <p className="m-0 mb-3.5 line-clamp-2 text-[14px] leading-[1.55] text-[#616161]">
                    {post.excerpt}
                </p>
                <div className="flex items-center justify-end">
                    <span className="text-[12px] text-muted">{formatRelativeTime(post.date)}</span>
                </div>
            </div>

            {isFolderOverlayOpen && (
                <FolderChangeOverlay
                    post={post}
                    onClose={() => setIsFolderOverlayOpen(false)}
                />
            )}
        </article>
    );
}
