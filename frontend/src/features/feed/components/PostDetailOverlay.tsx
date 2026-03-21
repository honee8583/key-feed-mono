import { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { Bookmark, ExternalLink, X } from 'lucide-react';
import type { Post } from '@/types';
import { usePostStore } from '@/stores/postStore';

interface PostDetailOverlayProps {
    post: Post;
    onClose: () => void;
}

export function PostDetailOverlay({ post, onClose }: PostDetailOverlayProps) {
    const { savedPostIds, toggleSave } = usePostStore();
    const isSaved = savedPostIds.includes(post.id);
    const [isVisible, setIsVisible] = useState(false);

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
        // Wait for close animation to finish
        setTimeout(() => {
            onClose();
        }, 300);
    };

    const handleToggleSave = (e: React.MouseEvent) => {
        e.stopPropagation();
        toggleSave(post.id);
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
                        <button 
                            onClick={() => window.open(post.originalUrl || post.url || '#', '_blank', 'noopener,noreferrer')}
                            className="flex-1 py-4 bg-slate-900 text-white rounded-2xl font-black text-xs uppercase shadow-xl flex items-center justify-center gap-2 active:scale-95 transition-transform"
                        >
                            <ExternalLink size={18} /> 원문 읽기
                        </button>
                        <button
                            onClick={handleToggleSave}
                            className={`w-14 h-14 rounded-2xl flex items-center justify-center backdrop-blur-md border active:scale-95 transition-all ${isSaved ? 'bg-indigo-50 border-indigo-100 text-indigo-600 shadow-lg' : 'bg-white/60 border-white/80 text-slate-300 shadow-sm'}`}
                        >
                            <Bookmark size={22} fill={isSaved ? "currentColor" : "none"} />
                        </button>
                    </div>
                </div>
            </div>
        </div>,
        document.body
    );
}
