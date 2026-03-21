import { useState, useRef, useEffect } from 'react';
import gsap from 'gsap';
import { useGSAP } from '@gsap/react';
import { ArrowLeft, FolderPlus, MoreHorizontal, Edit2, Trash2 } from 'lucide-react';
import { usePostStore } from '@/stores/postStore';
import { useUiStore } from '@/stores/uiStore';
import { ICON_MAP, AVAILABLE_COLORS, type IconName, type ColorName } from '@/utils/constants';
import { FolderActionModal } from './FolderActionModal';
import { useBookmarkFolders, useCreateBookmarkFolder, useUpdateBookmarkFolder, useDeleteBookmarkFolder } from '../api/bookmarkApi';

export function FolderOverlay() {
    const { posts } = usePostStore();
    const { isFolderOpen, closeFolderManagement, unmountFolderManagement } = useUiStore();
    
    const { data: folderListResponse } = useBookmarkFolders();
    const folders = folderListResponse || [];
    
    const createFolderMutation = useCreateBookmarkFolder();
    const updateFolderMutation = useUpdateBookmarkFolder();
    const deleteFolderMutation = useDeleteBookmarkFolder();
    const overlayRef = useRef<HTMLDivElement>(null);
    const { contextSafe } = useGSAP({ scope: overlayRef });

    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
    const [isEditModalOpen, setIsEditModalOpen] = useState(false);
    const [editingFolderData, setEditingFolderData] = useState<{
        folderId: number;
        name: string;
        icon: IconName;
        color: ColorName;
    } | null>(null);

    const [menuOpenFolderName, setMenuOpenFolderName] = useState<string | null>(null);

    const handleCreateConfirm = (name: string, icon: IconName, color: ColorName) => {
        createFolderMutation.mutate({ name, icon, color }, {
            onSuccess: () => setIsCreateModalOpen(false)
        });
    };

    const handleEditConfirm = (newName: string, icon: IconName, color: ColorName) => {
        if (editingFolderData) {
            updateFolderMutation.mutate({ folderId: editingFolderData.folderId, name: newName, icon, color }, {
                onSuccess: () => {
                    setIsEditModalOpen(false);
                    setEditingFolderData(null);
                }
            });
        }
    };

    useEffect(() => {
        contextSafe(() => {
            if (isFolderOpen) {
                gsap.to(overlayRef.current, { y: 0, opacity: 1, duration: 0.4, ease: "power3.out" });
            } else {
                gsap.to(overlayRef.current, {
                    y: "100%",
                    opacity: 0,
                    duration: 0.3,
                    ease: "power2.in",
                    onComplete: unmountFolderManagement
                });
            }
        })();
    }, [isFolderOpen, unmountFolderManagement, contextSafe]);

    return (
        <div
            ref={overlayRef}
            className="absolute inset-0 z-[100] bg-white flex justify-center translate-y-full opacity-0"
        >
            <div className="w-full max-w-[480px] flex flex-col px-6 pt-10">
                <div className="flex items-center justify-between mb-8">
                    <div className="flex items-center gap-3">
                        <button
                            onClick={closeFolderManagement}
                            className="p-2 bg-slate-50 border border-slate-100 rounded-full text-slate-600 shadow-sm active:scale-90 transition-transform"
                        >
                            <ArrowLeft size={20} />
                        </button>
                        <h3 className="text-xl font-black text-slate-900 uppercase tracking-tighter">
                            폴더 관리
                        </h3>
                    </div>
                    <button
                        onClick={() => setIsCreateModalOpen(true)}
                        className="p-2.5 bg-slate-900 text-white rounded-xl shadow-lg active:scale-90 transition-transform"
                    >
                        <FolderPlus size={20} />
                    </button>
                </div>

                <div className="flex-1 overflow-y-auto no-scrollbar space-y-3 pb-10">
                    {folders.map((f) => {
                        const isHexColor = typeof f.color === 'string' && f.color.startsWith('#');
                        const colorConfig = AVAILABLE_COLORS.find(c => c.name === f.color) || AVAILABLE_COLORS[0];
                        const ActiveIcon = f.icon ? ICON_MAP[f.icon as keyof typeof ICON_MAP] : null;
                        const postCount = posts.filter(p => p.folder === f.name).length;

                        return (
                            <div
                                key={f.folderId || f.name}
                                className="relative bg-white border border-slate-100 p-5 rounded-[28px] flex items-center justify-between shadow-sm group hover:border-slate-200 transition-all"
                            >
                                <div className="flex items-center gap-4">
                                    <div 
                                        className={`w-11 h-11 rounded-2xl flex items-center justify-center group-hover:scale-105 transition-transform shadow-sm ${!isHexColor ? ` ${colorConfig.light} ${colorConfig.text}` : ''}`}
                                        style={isHexColor ? { backgroundColor: `${f.color}20`, color: f.color } : {}}
                                    >
                                        {ActiveIcon ? <ActiveIcon size={20} /> : (f.icon ? <span className="text-[20px]">{f.icon}</span> : <ICON_MAP.Folder size={20} />)}
                                    </div>
                                    <div>
                                        <h4 className="text-sm font-bold text-slate-800">{f.name}</h4>
                                        <p className="text-[10px] text-slate-400 font-bold uppercase">{postCount} Articles</p>
                                    </div>
                                </div>

                                <div className="flex items-center gap-1">
                                    <button
                                        onClick={() => setMenuOpenFolderName(menuOpenFolderName === f.name ? null : f.name)}
                                        className="p-2 text-slate-300 hover:text-slate-600 transition-colors"
                                    >
                                        <MoreHorizontal size={18} />
                                    </button>

                                    <div className={`absolute right-12 bg-white border border-slate-100 rounded-2xl shadow-xl z-10 overflow-hidden flex divide-x divide-slate-50 transition-all origin-right ${menuOpenFolderName === f.name ? 'opacity-100 scale-100 pointer-events-auto' : 'opacity-0 scale-95 pointer-events-none'}`}>
                                        <button
                                            onClick={() => {
                                                setEditingFolderData({ folderId: f.folderId, name: f.name, icon: f.icon as IconName, color: f.color as ColorName });
                                                setIsEditModalOpen(true);
                                                setMenuOpenFolderName(null);
                                            }}
                                            className="px-4 py-2 text-[10px] font-black uppercase text-indigo-500 hover:bg-slate-50 transition-colors flex items-center gap-1.5"
                                        >
                                            <Edit2 size={12} /> 수정
                                        </button>
                                        <button
                                            onClick={() => handleDeleteFolder(f.folderId, f.name)}
                                            className="px-4 py-2 text-[10px] font-black uppercase text-rose-500 hover:bg-slate-50 transition-colors flex items-center gap-1.5"
                                        >
                                            <Trash2 size={12} /> 삭제
                                        </button>
                                    </div>
                                </div>
                            </div>
                        );
                    })}
                </div>
            </div>

            {isCreateModalOpen && (
                <FolderActionModal
                    type="create"
                    onConfirm={handleCreateConfirm}
                    onClose={() => setIsCreateModalOpen(false)}
                />
            )}
            {isEditModalOpen && editingFolderData && (
                <FolderActionModal
                    type="edit"
                    initialName={editingFolderData.name}
                    initialIcon={editingFolderData.icon}
                    initialColor={editingFolderData.color}
                    onConfirm={handleEditConfirm}
                    onClose={() => setIsEditModalOpen(false)}
                />
            )}
        </div>
    );

    function handleDeleteFolder(folderId: number, name: string) {
        if (confirm(`"${name}" 폴더를 정말 삭제하시겠습니까? 안의 글들은 '전체'로 이동됩니다.`)) {
            deleteFolderMutation.mutate(folderId, {
                onSuccess: () => setMenuOpenFolderName(null)
            });
        }
    }
}
