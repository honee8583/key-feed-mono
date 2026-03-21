import { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { Bookmark, ExternalLink, X, FolderKanban } from 'lucide-react';
import type { Post } from '@/types';
import { useCreateBookmark, useDeleteBookmark, useBookmarkFolders, useMoveBookmarkFolder, useRemoveBookmarkFromFolder } from '@/features/saved/api/bookmarkApi';
import { useQueryClient } from '@tanstack/react-query';

interface PostDetailOverlayProps {
    post: Post;
    onClose: () => void;
}

export function PostDetailOverlay({ post, onClose }: PostDetailOverlayProps) {
    const queryClient = useQueryClient();
    const createBookmark = useCreateBookmark();
    const deleteBookmark = useDeleteBookmark();
    
    const { data: folderListResponse } = useBookmarkFolders();
    const fetchedFolders = folderListResponse || [];
    const moveFolderMutation = useMoveBookmarkFolder();
    const removeFolderMutation = useRemoveBookmarkFromFolder();

    const [localSaved, setLocalSaved] = useState(post.bookmarkId != null);
    const [isVisible, setIsVisible] = useState(false);
    const [isFolderPickerOpen, setIsFolderPickerOpen] = useState(false);

    useEffect(() => {
        setLocalSaved(post.bookmarkId != null);
    }, [post.bookmarkId]);

    useEffect(() => {
        const originalOverflow = document.body.style.overflow;
        document.body.style.overflow = 'hidden';

        // Trigger open animation slightly after mount
        const frameId = requestAnimationFrame(() => setIsVisible(true));

        return () => {
            document.body.style.overflow = originalOverflow;
            cancelAnimationFrame(frameId);
        };
    }, []);

    const handleClose = () => {
        setIsVisible(false);
        setIsFolderPickerOpen(false);
        // Wait for close animation to finish
        setTimeout(() => {
            onClose();
        }, 300);
    };

    const handleFolderSelect = (folderId: number | null) => {
        if (!post.bookmarkId) return;

        const handleSuccess = () => {
            queryClient.invalidateQueries({ queryKey: ['feed'] });
            queryClient.invalidateQueries({ queryKey: ['bookmarks'] });
            setIsFolderPickerOpen(false);
        };

        if (folderId === null) {
            removeFolderMutation.mutate(post.bookmarkId, { onSuccess: handleSuccess });
        } else {
            moveFolderMutation.mutate({ bookmarkId: post.bookmarkId, folderId }, { onSuccess: handleSuccess });
        }
    };

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

    return createPortal(
        <div
            className={`fixed inset-0 bg-slate-900/10 backdrop-blur-md z-[110] flex justify-center transition-opacity duration-300 ${isVisible ? 'opacity-100' : 'opacity-0'}`}
        >
            <div className="w-full max-w-[480px] md:max-w-[540px] lg:max-w-[600px] h-full relative" onClick={handleClose}>
                <div
                    onClick={(e) => e.stopPropagation()}
                    className={`absolute bottom-0 w-full h-[88%] bg-white/70 backdrop-blur-[60px] rounded-t-[40px] p-8 shadow-2xl flex flex-col border-t border-white/60 overflow-y-auto no-scrollbar transition-transform duration-300 ease-out ${isVisible ? 'translate-y-0' : 'translate-y-full'}`}
                >
                    <div className="w-10 h-1 bg-slate-200 rounded-full mx-auto mb-8 shrink-0" />
                    <div className="flex items-center justify-between mb-8">
                        <div className="flex items-center gap-4">
                            <img
                                src={post.logo}
                                alt=""
                                className="w-9 h-9 object-contain p-1.5 bg-white rounded-xl border border-white/60 shadow-sm"
                            />
                            <div>
                                <p className="text-sm font-black text-slate-900 uppercase leading-none mb-1">
                                    {post.company}
                                </p>
                                <p className="text-[10px] text-slate-400 font-bold">
                                    {post.date}
                                </p>
                            </div>
                        </div>
                        <button
                            onClick={handleClose}
                            className="p-2.5 bg-white/60 backdrop-blur-md rounded-full text-slate-400 border border-white/60 shadow-sm active:scale-90 transition-transform"
                        >
                            <X size={18} />
                        </button>
                    </div>

                    {post.thumbnail && (
                        <div className="w-full h-44 mb-6 shrink-0 rounded-3xl overflow-hidden shadow-md">
                            <img src={post.thumbnail} alt="" className="w-full h-full object-cover" />
                        </div>
                    )}

                    <h2 className="text-xl font-black text-slate-800 leading-tight mb-5">
                        {post.title}
                    </h2>

                    <div className="text-slate-600 text-[15px] leading-relaxed mb-24 font-medium">
                        {post.content || post.excerpt}
                    </div>

                    {/* Action Footer */}
                    <div className="mt-auto absolute bottom-8 left-8 right-8 flex gap-3">
                        {localSaved && (
                            <div className="relative">
                                <button
                                    onClick={(e) => {
                                        e.stopPropagation();
                                        setIsFolderPickerOpen(!isFolderPickerOpen);
                                    }}
                                    className={`w-14 h-14 rounded-2xl flex items-center justify-center backdrop-blur-md border active:scale-95 transition-all ${isFolderPickerOpen ? 'bg-slate-900 border-slate-900 text-white shadow-lg' : 'bg-white/60 border-white/80 text-slate-500 shadow-sm'}`}
                                >
                                    <FolderKanban size={22} />
                                </button>

                                {isFolderPickerOpen && (
                                    <div 
                                        onClick={(e) => e.stopPropagation()}
                                        className="absolute bottom-[calc(100%+12px)] left-0 bg-white shadow-2xl rounded-2xl p-2 min-w-[150px] border border-slate-100 flex flex-col gap-1 z-20 origin-bottom-left animate-in fade-in slide-in-from-bottom-2 duration-200"
                                    >
                                        <div className="text-[10px] font-black uppercase text-slate-400 px-3 py-2 border-b border-slate-50 mb-1">
                                            폴더로 이동
                                        </div>
                                        {fetchedFolders.map(f => (
                                            <button
                                                key={f.folderId}
                                                onClick={() => handleFolderSelect(f.folderId)}
                                                className="text-left px-3 py-2.5 rounded-xl text-xs font-bold text-slate-700 hover:bg-slate-50 active:scale-[0.98] transition-all flex items-center gap-2"
                                            >
                                                <span>{f.icon || '📁'}</span>
                                                <span className="truncate">{f.name}</span>
                                            </button>
                                        ))}
                                        <div className="h-px bg-slate-50 my-1 mx-2" />
                                        <button
                                            onClick={() => handleFolderSelect(null)}
                                            className="text-left px-3 py-2.5 rounded-xl text-xs font-bold text-slate-500 hover:bg-slate-50 active:scale-[0.98] transition-all flex items-center gap-2"
                                        >
                                            <span className="opacity-70">📂</span>
                                            <span>미분류로 이동</span>
                                        </button>
                                    </div>
                                )}
                            </div>
                        )}
                        <button 
                            onClick={() => window.open(post.originalUrl || post.url || '#', '_blank', 'noopener,noreferrer')}
                            className="flex-1 py-4 bg-slate-900 text-white rounded-2xl font-black text-xs uppercase shadow-xl flex items-center justify-center gap-2 active:scale-95 transition-transform"
                        >
                            <ExternalLink size={18} /> 원문 읽기
                        </button>
                        <button
                            onClick={handleToggleSave}
                            className={`w-14 h-14 rounded-2xl flex items-center justify-center backdrop-blur-md border active:scale-95 transition-all ${localSaved ? 'bg-indigo-50 border-indigo-100 text-indigo-600 shadow-lg' : 'bg-white/60 border-white/80 text-slate-300 shadow-sm'}`}
                        >
                            <Bookmark size={22} fill={localSaved ? "currentColor" : "none"} />
                        </button>
                    </div>
                </div>
            </div>
        </div>,
        document.body
    );
}
