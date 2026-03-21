import { Bookmark } from 'lucide-react';
import type { Post } from '@/types';
import { usePostStore } from '@/stores/postStore';

interface PostCardProps {
    post: Post;
    onClick: (post: Post) => void;
}

export function PostCard({ post, onClick }: PostCardProps) {
    const { savedPostIds, readPostIds, toggleSave } = usePostStore();

    const isSaved = savedPostIds.includes(post.id);
    const isRead = readPostIds.includes(post.id);

    const handleToggleSave = (e: React.MouseEvent) => {
        e.stopPropagation();
        toggleSave(post.id);
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
                <button
                    onClick={handleToggleSave}
                    className={`p-2 rounded-xl transition-all border active:scale-95 ${isSaved ? 'text-indigo-600 bg-white/70 border-indigo-100 shadow-sm' : 'text-slate-200 bg-white/30 border-white/40'}`}
                >
                    <Bookmark size={16} fill={isSaved ? "currentColor" : "none"} />
                </button>
            </div>
        </div>
    );
}
