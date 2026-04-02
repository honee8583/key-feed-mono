import { useRef, useEffect } from 'react';
import { ArrowLeft, Loader2, Link as LinkIcon } from 'lucide-react';
import gsap from 'gsap';
import { useGSAP } from '@gsap/react';
import { useUiStore } from '@/stores/uiStore';
import { useMySources, useDeleteSource } from '../api/sourceApi';

export function MySourcesOverlay() {
    const { isSourcesOpen, closeSourcesManagement, unmountSourcesManagement } = useUiStore();
    const overlayRef = useRef<HTMLDivElement>(null);
    const { contextSafe } = useGSAP({ scope: overlayRef });

    const { data: mySources, status } = useMySources();
    const { mutate: deleteSource, isPending: isDeleting } = useDeleteSource();

    useEffect(() => {
        contextSafe(() => {
            if (isSourcesOpen) {
                gsap.to(overlayRef.current, {
                    x: 0,
                    opacity: 1,
                    duration: 0.4,
                    ease: "power3.out"
                });
            } else {
                gsap.to(overlayRef.current, {
                    x: "100%",
                    opacity: 0,
                    duration: 0.3,
                    ease: "power2.in",
                    onComplete: unmountSourcesManagement
                });
            }
        })();
    }, [isSourcesOpen, unmountSourcesManagement, contextSafe]);

    return (
        <div 
            ref={overlayRef}
            className="absolute inset-0 z-[100] bg-[#F1F5F9] flex justify-center translate-x-full opacity-0 overflow-y-auto"
        >
            <div className="w-full max-w-[480px] flex flex-col min-h-screen">
                {/* Header */}
                <div className="flex items-center gap-3 px-5 pt-10 pb-6 sticky top-0 bg-[#F1F5F9]/80 backdrop-blur-xl z-10">
                    <button
                        onClick={closeSourcesManagement}
                        className="p-2 bg-white border border-slate-100 rounded-full text-slate-600 shadow-sm active:scale-90 transition-transform"
                    >
                        <ArrowLeft size={20} />
                    </button>
                    <h2 className="text-[18px] font-black text-slate-800 tracking-tight">내소스목록</h2>
                </div>

                {/* List Content */}
                <div className="px-5 pb-20 flex-1">
                    {status === 'pending' ? (
                        <div className="flex justify-center py-20">
                            <Loader2 className="w-8 h-8 animate-spin text-slate-300" />
                        </div>
                    ) : status === 'error' ? (
                        <div className="text-center py-20 opacity-50">
                            <p className="text-[12px] font-bold text-slate-500">목록을 불러오는 데 실패했습니다.</p>
                        </div>
                    ) : mySources && mySources.length > 0 ? (
                        <div className="space-y-4">
                            {mySources.map((source) => (
                                <div 
                                    key={source.userSourceId} 
                                    className="bg-white rounded-[20px] p-5 border border-white shadow-sm flex items-start justify-between"
                                >
                                    <div className="flex items-start gap-4 overflow-hidden">
                                        <div className="w-12 h-12 bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden shrink-0 flex items-center justify-center p-2">
                                            <img 
                                                src={source.logoUrl || `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(source.userDefinedName)}&backgroundColor=0f172a`} 
                                                alt="" 
                                                onError={(e) => {
                                                    e.currentTarget.onerror = null;
                                                    e.currentTarget.src = `https://api.dicebear.com/7.x/initials/svg?seed=${encodeURIComponent(source.userDefinedName)}&backgroundColor=0f172a`;
                                                }}
                                                className="w-full h-full object-contain" 
                                            />
                                        </div>
                                        <div className="flex flex-col min-w-0 pt-0.5">
                                            <div className="flex items-center gap-1.5 mb-1">
                                                <h3 className="text-[15px] font-black text-slate-800 truncate">
                                                    {source.userDefinedName}
                                                </h3>
                                            </div>
                                            <p className="text-[11px] font-bold text-slate-400 mb-2 truncate opacity-80 flex items-center gap-1">
                                                <LinkIcon size={10} />
                                                {(new URL(source.url)).hostname.replace('www.', '')}
                                            </p>
                                            <div className="flex flex-wrap gap-1.5">
                                                <div className="px-2.5 py-0.5 rounded-full bg-slate-50 text-slate-500 text-[8px] font-black uppercase tracking-widest border border-slate-100">
                                                    SUBSCRIBED
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                    <div className="shrink-0 ml-3">
                                        <button
                                            disabled={isDeleting}
                                            onClick={() => deleteSource(source.userSourceId)}
                                            className="px-3.5 py-1.5 rounded-full bg-rose-50/50 text-rose-500 hover:bg-rose-100/50 active:scale-95 transition-all text-[10px] font-black tracking-wide border border-rose-100 disabled:opacity-50 disabled:scale-100"
                                        >
                                            구독취소
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <div className="text-center py-20 opacity-40 flex flex-col items-center">
                            <p className="text-[12px] font-bold text-slate-500">구독 중인 소스가 없습니다.</p>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
