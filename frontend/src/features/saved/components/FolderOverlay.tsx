import { useState, useRef, useEffect, useMemo } from 'react';
import gsap from 'gsap';
import { useGSAP } from '@gsap/react';
import { ChevronLeft, Folder, Trash2 } from 'lucide-react';
import { useUiStore } from '@/stores/uiStore';
import {
    useBookmarks,
    useBookmarkFolders,
    useCreateBookmarkFolder,
    useUpdateBookmarkFolder,
    useDeleteBookmarkFolder,
} from '../api/bookmarkApi';
import type { BookmarkFolder } from '../types';

export function FolderOverlay() {
    const { isFolderOpen, closeFolderManagement, unmountFolderManagement } = useUiStore();

    const { data: folderListResponse } = useBookmarkFolders();
    const folders = folderListResponse || [];

    const { data: bookmarkData } = useBookmarks(undefined, 20);

    const createFolderMutation = useCreateBookmarkFolder();
    const updateFolderMutation = useUpdateBookmarkFolder();
    const deleteFolderMutation = useDeleteBookmarkFolder();

    const overlayRef = useRef<HTMLDivElement>(null);
    const { contextSafe } = useGSAP({ scope: overlayRef });

    const [newFolder, setNewFolder] = useState('');
    const [editingId, setEditingId] = useState<number | null>(null);
    const [draftName, setDraftName] = useState('');

    // 불러온 북마크 기준 폴더별 개수(근사치)
    const counts = useMemo(() => {
        const items = bookmarkData ? bookmarkData.pages.flatMap((page) => page.content || []) : [];
        const map: Record<string, number> = {};
        for (const item of items) {
            if (item?.folderName) map[item.folderName] = (map[item.folderName] ?? 0) + 1;
        }
        return map;
    }, [bookmarkData]);

    useEffect(() => {
        contextSafe(() => {
            if (isFolderOpen) {
                gsap.to(overlayRef.current, { y: 0, opacity: 1, duration: 0.4, ease: 'power3.out' });
            } else {
                gsap.to(overlayRef.current, {
                    y: '100%',
                    opacity: 0,
                    duration: 0.3,
                    ease: 'power2.in',
                    onComplete: unmountFolderManagement,
                });
            }
        })();
    }, [isFolderOpen, unmountFolderManagement, contextSafe]);

    const handleAdd = () => {
        const name = newFolder.trim();
        if (!name || createFolderMutation.isPending) return;
        createFolderMutation.mutate({ name }, { onSuccess: () => setNewFolder('') });
    };

    const startEdit = (folder: BookmarkFolder) => {
        setEditingId(folder.folderId);
        setDraftName(folder.name);
    };

    const saveEdit = (folder: BookmarkFolder) => {
        const name = draftName.trim();
        if (!name) return;
        // 기존 아이콘/색상 정보는 보존하고 이름만 변경한다.
        updateFolderMutation.mutate(
            { folderId: folder.folderId, name, icon: folder.icon, color: folder.color },
            { onSuccess: () => setEditingId(null) }
        );
    };

    const handleDelete = (folder: BookmarkFolder) => {
        if (confirm(`"${folder.name}" 폴더를 삭제할까요? 저장한 글은 사라지지 않아요.`)) {
            deleteFolderMutation.mutate(folder.folderId, {
                onSuccess: () => {
                    if (editingId === folder.folderId) setEditingId(null);
                },
            });
        }
    };

    const addEnabled = newFolder.trim().length > 0;

    return (
        <div
            ref={overlayRef}
            className="absolute inset-0 z-[100] flex translate-y-full flex-col bg-canvas opacity-0"
        >
            {/* Header */}
            <div className="px-5 pb-3 pt-[calc(env(safe-area-inset-top)+34px)]">
                <button
                    onClick={closeFolderManagement}
                    className="mb-[18px] inline-flex items-center gap-1.5 text-[14px] text-[#75758a] transition-colors hover:text-ink"
                >
                    <ChevronLeft size={16} strokeWidth={1.8} />
                    북마크
                </button>
                <h1 className="mb-1 font-display text-[30px] font-medium tracking-tightest text-primary">폴더 관리</h1>
                <p className="text-[15px] text-[#616161]">폴더를 추가하거나 이름을 바꾸고, 삭제할 수 있어요.</p>
            </div>

            {/* Body */}
            <div className="no-scrollbar flex-1 overflow-y-auto px-5 pb-24 pt-[18px]">
                {/* Add new folder */}
                <div className="mb-[26px] flex gap-[9px]">
                    <input
                        value={newFolder}
                        onChange={(e) => setNewFolder(e.target.value)}
                        onKeyDown={(e) => e.key === 'Enter' && handleAdd()}
                        placeholder="새 폴더 이름"
                        className="h-12 min-w-0 flex-1 rounded-lg border border-hairline px-3.5 text-[15px] text-ink outline-none transition-colors focus:border-form-focus"
                    />
                    <button
                        onClick={handleAdd}
                        disabled={!addEnabled || createFolderMutation.isPending}
                        className={`h-12 shrink-0 rounded-lg px-5 text-[15px] font-medium text-white transition-colors ${
                            addEnabled ? 'bg-primary' : 'cursor-default bg-hairline'
                        }`}
                    >
                        추가
                    </button>
                </div>

                <div className="mb-2.5 font-mono text-[11px] tracking-[0.6px] text-muted">내 폴더</div>

                {folders.length > 0 ? (
                    <>
                        <div className="overflow-hidden rounded-2xl border border-[#f2f2f2]">
                            {folders.map((folder) => {
                                const isEditing = editingId === folder.folderId;
                                return (
                                    <div
                                        key={folder.folderId}
                                        className="flex items-center gap-2 border-b border-[#f2f2f2] px-3.5 py-3 last:border-b-0"
                                    >
                                        <Folder size={19} strokeWidth={1.6} className="shrink-0 text-[#75758a]" />

                                        {isEditing ? (
                                            <>
                                                <input
                                                    autoFocus
                                                    value={draftName}
                                                    onChange={(e) => setDraftName(e.target.value)}
                                                    onKeyDown={(e) => e.key === 'Enter' && saveEdit(folder)}
                                                    className="min-w-0 flex-1 rounded-t border-b-[1.5px] border-form-focus bg-[#faf7fc] px-2 py-1.5 text-[16px] font-medium text-primary outline-none"
                                                />
                                                <button
                                                    onClick={() => saveEdit(folder)}
                                                    className="shrink-0 px-2 py-1.5 text-[14px] font-semibold text-deep-green"
                                                >
                                                    저장
                                                </button>
                                            </>
                                        ) : (
                                            <>
                                                <span className="min-w-0 flex-1 truncate py-1.5 text-[16px] font-medium text-primary">
                                                    {folder.name}
                                                </span>
                                                <span className="shrink-0 font-mono text-[12px] text-muted">
                                                    {counts[folder.name] ?? 0}
                                                </span>
                                                <button
                                                    onClick={() => startEdit(folder)}
                                                    className="shrink-0 px-2 py-1.5 text-[14px] font-medium text-action-blue"
                                                >
                                                    수정
                                                </button>
                                            </>
                                        )}

                                        <button
                                            onClick={() => handleDelete(folder)}
                                            aria-label="폴더 삭제"
                                            className="flex h-[34px] w-[34px] shrink-0 items-center justify-center rounded-full text-brand-error transition-colors hover:bg-[#fff5f5]"
                                        >
                                            <Trash2 size={17} strokeWidth={1.6} />
                                        </button>
                                    </div>
                                );
                            })}
                        </div>
                        <p className="mt-3.5 px-0.5 text-[13px] leading-[1.55] text-muted">
                            <span className="font-semibold text-action-blue">수정</span>을 눌러 이름을 바꾼 뒤{' '}
                            <span className="font-semibold text-deep-green">저장</span>하세요. 폴더를 삭제해도 저장한 글은
                            사라지지 않아요.
                        </p>
                    </>
                ) : (
                    <div className="px-6 py-14 text-center text-muted">
                        <div className="mb-2 font-mono text-[12px] tracking-[0.5px]">NO FOLDERS</div>
                        <div className="text-[15px] leading-[1.5]">폴더가 없어요. 위에서 새 폴더를 추가해보세요.</div>
                    </div>
                )}
            </div>
        </div>
    );
}
