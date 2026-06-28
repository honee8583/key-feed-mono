import { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { Inbox, Check, Plus } from 'lucide-react';
import type { Post } from '@/types';
import { useQueryClient } from '@tanstack/react-query';
import { useBookmarkFolders, useMoveBookmarkFolder, useRemoveBookmarkFromFolder } from '@/features/saved/api/bookmarkApi';
import { ICON_MAP } from '@/utils/constants';
import { useUiStore } from '@/stores/uiStore';
import { cn } from '@/utils/cn';

interface FolderChangeOverlayProps {
    post: Post;
    onClose: () => void;
}

export function FolderChangeOverlay({ post, onClose }: FolderChangeOverlayProps) {
    const [isVisible, setIsVisible] = useState(false);
    const queryClient = useQueryClient();
    const { data: folderListResponse } = useBookmarkFolders();
    const fetchedFolders = folderListResponse || [];
    const { openFolderManagement } = useUiStore();

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

    const handleClose = (e?: React.MouseEvent, after?: () => void) => {
        if (e) e.stopPropagation();
        setIsVisible(false);
        setTimeout(() => {
            onClose();
            after?.();
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

    const handleManageFolders = (e: React.MouseEvent) => {
        handleClose(e, openFolderManagement);
    };

    return createPortal(
        <div
            className={cn(
                'fixed inset-0 z-[120] flex items-end justify-center bg-black/40 transition-opacity duration-300',
                isVisible ? 'opacity-100' : 'opacity-0',
            )}
            onClick={handleClose}
        >
            <div
                className={cn(
                    'flex w-full max-w-[480px] flex-col rounded-t-[22px] bg-canvas pb-[34px] pt-2.5 shadow-2xl transition-transform duration-300 ease-out md:max-w-[540px]',
                    isVisible ? 'translate-y-0' : 'translate-y-full',
                )}
                onClick={e => e.stopPropagation()}
                style={{ maxHeight: '78vh' }}
            >
                {/* drag handle */}
                <div className="mx-auto mb-3.5 mt-2 h-1 w-[38px] rounded-full bg-[#e5e7eb]" />

                {/* header */}
                <div className="border-b border-[#f2f2f2] px-[22px] pb-3.5">
                    <p className="mb-2 font-mono text-[11px] tracking-[0.6px] text-coral">폴더에 저장</p>
                    <h2 className="line-clamp-1 text-[15px] font-semibold leading-[1.4] text-primary">{post.title}</h2>
                </div>

                {/* folder list */}
                <div className="no-scrollbar flex-1 overflow-y-auto py-1.5">
                    {/* 전체 (미분류) */}
                    <button
                        onClick={(e) => handleSelect(e, null)}
                        className={cn(
                            'flex w-full items-center gap-[13px] px-[22px] py-[15px] text-left transition-colors',
                            !post.folder ? 'bg-[#edfce9]' : 'hover:bg-[#f7f7f5]',
                        )}
                    >
                        <Inbox size={20} strokeWidth={1.6} className={cn('shrink-0', !post.folder ? 'text-deep-green' : 'text-[#75758a]')} />
                        <span className={cn('flex-1 text-[16px] font-medium', !post.folder ? 'text-deep-green' : 'text-primary')}>전체</span>
                        {!post.folder && <Check size={19} strokeWidth={2.2} className="shrink-0 text-deep-green" />}
                    </button>

                    {/* API로 불러온 폴더 리스트 */}
                    {fetchedFolders.map(f => {
                        const isSelected = post.folder === f.name;
                        const IconComp = f.icon ? ICON_MAP[f.icon as keyof typeof ICON_MAP] : null;
                        const iconColor = isSelected ? 'text-deep-green' : 'text-[#75758a]';

                        return (
                            <button
                                key={f.folderId}
                                onClick={(e) => handleSelect(e, f.folderId)}
                                className={cn(
                                    'flex w-full items-center gap-[13px] px-[22px] py-[15px] text-left transition-colors',
                                    isSelected ? 'bg-[#edfce9]' : 'hover:bg-[#f7f7f5]',
                                )}
                            >
                                {IconComp ? (
                                    <IconComp size={20} strokeWidth={1.6} className={cn('shrink-0', iconColor)} />
                                ) : f.icon ? (
                                    <span className="shrink-0 text-[18px] leading-none">{f.icon}</span>
                                ) : (
                                    <ICON_MAP.Folder size={20} strokeWidth={1.6} className={cn('shrink-0', iconColor)} />
                                )}
                                <span className={cn('flex-1 truncate text-[16px] font-medium', isSelected ? 'text-deep-green' : 'text-primary')}>{f.name}</span>
                                {isSelected && <Check size={19} strokeWidth={2.2} className="shrink-0 text-deep-green" />}
                            </button>
                        );
                    })}
                </div>

                {/* footer: 폴더 관리 */}
                <div className="border-t border-[#f2f2f2] px-[22px] pt-2">
                    <button
                        onClick={handleManageFolders}
                        className="flex w-full items-center gap-2.5 py-3 text-left text-action-blue"
                    >
                        <Plus size={20} strokeWidth={1.7} className="shrink-0" />
                        <span className="text-[15px] font-medium">폴더 관리</span>
                    </button>
                </div>
            </div>
        </div>,
        document.body
    );
}
