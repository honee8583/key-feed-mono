import { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { X, Inbox, Check } from 'lucide-react';
import type { Post } from '@/types';
import { useQueryClient } from '@tanstack/react-query';
import { useBookmarkFolders, useMoveBookmarkFolder, useRemoveBookmarkFromFolder } from '@/features/saved/api/bookmarkApi';
import { ICON_MAP, AVAILABLE_COLORS } from '@/utils/constants';

interface FolderChangeOverlayProps {
    post: Post;
    onClose: () => void;
}

export function FolderChangeOverlay({ post, onClose }: FolderChangeOverlayProps) {
    const [isVisible, setIsVisible] = useState(false);
    const queryClient = useQueryClient();
    const { data: folderListResponse } = useBookmarkFolders();
    const fetchedFolders = folderListResponse || [];
    
    // UI상에서 낙관적 업데이트 등을 편하게 처리하기 위해 현재 선택값을 로컬상태로 둘 수 있지만, 
    // 여기서는 props로 받은 post.folder 기준으로 렌더링하고 바로 요청을 보냅니다.
    const moveFolderMutation = useMoveBookmarkFolder();
    const removeFolderMutation = useRemoveBookmarkFromFolder();

    useEffect(() => {
        const originalOverflow = document.body.style.overflow;
        document.body.style.overflow = 'hidden';

        const frameId = requestAnimationFrame(() => setIsVisible(true));

        return () => {
            document.body.style.overflow = originalOverflow;
            cancelAnimationFrame(frameId);
        };
    }, []);

    const handleClose = (e?: React.MouseEvent) => {
        if (e) e.stopPropagation();
        setIsVisible(false);
        setTimeout(() => {
            onClose();
        }, 300);
    };

    const handleSelect = (e: React.MouseEvent, folderId: number | null) => {
        e.stopPropagation();
        if (!post.bookmarkId) return;
        
        const handleSuccess = () => {
            queryClient.invalidateQueries({ queryKey: ['feed'] });
            queryClient.invalidateQueries({ queryKey: ['bookmarks'] });
            handleClose();
        };

        if (folderId === null) {
            removeFolderMutation.mutate(post.bookmarkId, { onSuccess: handleSuccess });
        } else {
            moveFolderMutation.mutate({ bookmarkId: post.bookmarkId, folderId }, { onSuccess: handleSuccess });
        }
    };

    return createPortal(
        <div 
            className={`fixed inset-0 z-[120] bg-slate-900/40 backdrop-blur-sm transition-opacity duration-300 flex items-end justify-center ${isVisible ? 'opacity-100' : 'opacity-0'}`} 
            onClick={handleClose}
        >
            <div 
                className={`w-full max-w-[480px] md:max-w-[540px] bg-white rounded-t-[40px] pt-8 pb-10 px-6 shadow-2xl transition-transform duration-300 ease-out flex flex-col ${isVisible ? 'translate-y-0' : 'translate-y-full'}`}
                onClick={e => e.stopPropagation()}
                style={{ maxHeight: '85vh' }}
            >
                <div className="flex items-start justify-between mb-8">
                    <div>
                        <h2 className="text-2xl font-black text-slate-800 leading-tight">폴더 변경</h2>
                        <p className="text-[10px] font-black tracking-widest text-slate-400 mt-1 uppercase">Select a folder</p>
                    </div>
                    <button 
                        onClick={handleClose} 
                        className="w-10 h-10 rounded-full bg-slate-50 flex items-center justify-center text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors"
                    >
                        <X size={20} />
                    </button>
                </div>

                <div className="overflow-y-auto no-scrollbar space-y-3 flex-1 pb-4">
                    {/* 전체 아이템 (미분류) */}
                    <button 
                        onClick={(e) => handleSelect(e, null)}
                        className={`w-full text-left rounded-3xl p-4 flex items-center justify-between transition-all border-2 ${!post.folder ? 'bg-blue-500 border-blue-500 text-white shadow-lg shadow-blue-500/30' : 'bg-white border-slate-50 text-slate-800 hover:border-slate-100'}`}
                    >
                        <div className="flex items-center gap-4">
                            <div className={`w-12 h-12 rounded-[20px] flex items-center justify-center ${!post.folder ? 'bg-white/20 text-white' : 'bg-slate-50 text-slate-400'}`}>
                                <Inbox size={22} strokeWidth={2.5} />
                            </div>
                            <div>
                                <h3 className={`text-base font-black ${!post.folder ? 'text-white' : 'text-slate-800'}`}>전체</h3>
                                <p className={`text-[10px] font-black tracking-wider uppercase mt-0.5 ${!post.folder ? 'text-blue-100' : 'text-slate-400'}`}>No Folder</p>
                            </div>
                        </div>
                        {!post.folder && <Check size={24} className="text-white mr-2" strokeWidth={3} />}
                    </button>

                    {/* API로 불러온 폴더 리스트 */}
                    {fetchedFolders.map(f => {
                        const isSelected = post.folder === f.name;
                        const isHex = typeof f.color === 'string' && f.color.startsWith('#');
                        const config = AVAILABLE_COLORS.find(c => c.name === f.color) || AVAILABLE_COLORS[0];
                        const IconComp = f.icon ? ICON_MAP[f.icon as keyof typeof ICON_MAP] : null;

                        return (
                            <button 
                                key={f.folderId}
                                onClick={(e) => handleSelect(e, f.folderId)}
                                className={`w-full text-left rounded-3xl p-4 flex items-center justify-between transition-all border-2 ${isSelected ? 'bg-blue-500 border-blue-500 text-white shadow-lg shadow-blue-500/30' : 'bg-white border-slate-50 text-slate-800 hover:border-slate-100'}`}
                            >
                                <div className="flex items-center gap-4">
                                    <div 
                                        className={`w-12 h-12 rounded-[20px] flex items-center justify-center ${isSelected ? 'bg-white/20 text-white' : (!isHex ? `${config.light} ${config.text}` : '')}`}
                                        style={!isSelected && isHex ? { backgroundColor: `${f.color}15`, color: f.color } : {}}
                                    >
                                        {IconComp ? <IconComp size={22} strokeWidth={2.5} /> : (f.icon ? <span className="text-[22px]">{f.icon}</span> : <ICON_MAP.Folder size={22} />)}
                                    </div>
                                    <div>
                                        <h3 className={`text-base font-black ${isSelected ? 'text-white' : 'text-slate-800'}`}>{f.name}</h3>
                                        <p className={`text-[10px] font-black tracking-wider uppercase mt-0.5 ${isSelected ? 'text-blue-100' : 'text-slate-400'}`}>Folder</p>
                                    </div>
                                </div>
                                {isSelected && <Check size={24} className="text-white mr-2" strokeWidth={3} />}
                            </button>
                        );
                    })}
                </div>
            </div>
        </div>,
        document.body
    );
}
