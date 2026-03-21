import { useState, useEffect } from 'react';
import { Bookmark, FolderOpen } from 'lucide-react';
import type { Post } from '@/types';
import { usePostStore } from '@/stores/postStore';
import { useCreateBookmark, useDeleteBookmark } from '@/features/saved/api/bookmarkApi';
import { useQueryClient } from '@tanstack/react-query';
import { FolderChangeOverlay } from './FolderChangeOverlay';

interface PostCardProps {
    post: Post;
    onClick: (post: Post) => void;
}

export function PostCard({ post, onClick }: PostCardProps) {
    const { readPostIds } = usePostStore();
    const isRead = readPostIds.includes(post.id);

    const queryClient = useQueryClient();
    const createBookmark = useCreateBookmark();
    const deleteBookmark = useDeleteBookmark();

    const [localSaved, setLocalSaved] = useState(post.bookmarkId != null);
    const [isFolderOverlayOpen, setIsFolderOverlayOpen] = useState(false);

    useEffect(() => {
        setLocalSaved(post.bookmarkId != null);
    }, [post.bookmarkId]);

    const handleToggleSave = (e: React.MouseEvent) => {
        e.stopPropagation();
        
        if (localSaved) {
            setLocalSaved(false);
            if (post.bookmarkId) {
                deleteBookmark.mutate(post.bookmarkId, {
                    onSuccess: () => {
                        queryClient.invalidateQueries({ queryKey: ['feed'] });
                        queryClient.invalidateQueries({ queryKey: ['bookmarks'] });
                    },
                    onError: () => setLocalSaved(true)
                });
            }
        } else {
            setLocalSaved(true);
            createBookmark.mutate(String(post.id), {
                onSuccess: () => {
                    queryClient.invalidateQueries({ queryKey: ['feed'] });
                    queryClient.invalidateQueries({ queryKey: ['bookmarks'] });
                },
                onError: () => setLocalSaved(false)
            });
        }
    };

    return (
        <div
            id={`post-${post.id}`}
            onClick={() => onClick(post)}
            className={`bg-white/40 backdrop-blur-xl rounded-[24px] border border-white/60 p-4 shadow-sm active:scale-[0.98] transition-all cursor-pointer hover:bg-white/60 ${isRead ? 'opacity-70' : ''}`}
        >
            <div className="flex items-start gap-4">
                <div className="flex-1">
                    <div className="flex items-center gap-2 mb-2">
                        <img src={post.logo} alt="" className="w-5 h-5 object-contain shadow-sm rounded" />
                        <div className="flex items-center gap-1.5">
                            <span className="text-[10px] font-black text-slate-400 uppercase tracking-tighter">
                                {post.company}
                            </span>
                            {isRead && (
                                <span className="flex items-center gap-0.5 px-1 py-0.5 bg-white/40 text-slate-300 text-[8px] font-black rounded uppercase border border-white/20">
                                    읽음
                                </span>
                            )}
                        </div>
                    </div>
                    <h4 className={`text-[14px] font-bold text-slate-800 leading-snug mb-1 ${isRead ? 'text-slate-500' : ''}`}>
                        {post.title}
                    </h4>
                    <p className="text-[11px] text-slate-500 line-clamp-1 font-medium">
                        {post.excerpt}
                    </p>
                </div>
                <div className="flex flex-col gap-2 shrink-0">
                    {localSaved && (
                        <button
                            onClick={(e) => {
                                e.stopPropagation();
                                setIsFolderOverlayOpen(true);
                            }}
                            className="p-2.5 rounded-2xl transition-all border text-slate-500 bg-white border-slate-100 shadow-sm hover:bg-slate-50 active:scale-95"
                        >
                            <FolderOpen size={18} strokeWidth={2.5} />
                        </button>
                    )}
                    <button
                        onClick={handleToggleSave}
                        className={`p-2.5 rounded-2xl transition-all border active:scale-95 shadow-sm ${localSaved ? 'text-indigo-500 bg-white border-slate-100' : 'text-slate-300 bg-white/50 border-white/40'}`}
                    >
                        <Bookmark size={18} strokeWidth={2.5} fill={localSaved ? "currentColor" : "none"} />
                    </button>
                </div>
            </div>
            
            {isFolderOverlayOpen && (
                <FolderChangeOverlay 
                    post={post} 
                    onClose={() => setIsFolderOverlayOpen(false)} 
                />
            )}
        </div>
    );
}
