import { useState } from 'react';
import { Bookmark, FolderOpen } from 'lucide-react';
import type { Post } from '@/types';
import { cn } from '@/utils/cn';
import { formatRelativeTime } from '@/utils/time';
import { useBookmarkToggle } from '../hooks/useBookmarkToggle';
import { FolderChangeOverlay } from './FolderChangeOverlay';

interface PostCardProps {
    post: Post;
    onClick: (post: Post) => void;
}

export function PostCard({ post, onClick }: PostCardProps) {
    const { saved, toggle } = useBookmarkToggle(post);
    const [isFolderOverlayOpen, setIsFolderOverlayOpen] = useState(false);

    return (
        <article
            id={`post-${post.id}`}
            onClick={() => onClick(post)}
            className="group cursor-pointer border-b border-card-border py-[18px]"
        >
            <div className="mb-[7px] flex items-center justify-between gap-3">
                <span className="truncate font-mono text-[11px] tracking-[0.4px] text-slate">
                    {post.company}
                </span>
                <div className="flex shrink-0 items-center gap-1">
                    <span className="text-[12px] text-muted">{formatRelativeTime(post.date)}</span>
                    {saved && (
                        <button
                            type="button"
                            aria-label="폴더 변경"
                            onClick={(e) => {
                                e.stopPropagation();
                                setIsFolderOverlayOpen(true);
                            }}
                            className="ml-1 flex h-7 w-7 items-center justify-center rounded-full text-muted transition-colors hover:bg-soft-stone hover:text-ink"
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
                            saved ? 'text-coral' : 'text-muted hover:bg-soft-stone hover:text-ink'
                        )}
                    >
                        <Bookmark size={15} strokeWidth={1.8} fill={saved ? 'currentColor' : 'none'} />
                    </button>
                </div>
            </div>

            <h3 className="mb-1.5 text-[17px] font-semibold leading-[1.3] tracking-[-0.2px] text-primary">
                {post.title}
            </h3>
            <p className="line-clamp-2 text-[14px] leading-[1.5] text-[#616161]">
                {post.excerpt}
            </p>

            {isFolderOverlayOpen && (
                <FolderChangeOverlay
                    post={post}
                    onClose={() => setIsFolderOverlayOpen(false)}
                />
            )}
        </article>
    );
}
