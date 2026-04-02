import { useRef, useEffect } from 'react';
import gsap from 'gsap';
import { useGSAP } from '@gsap/react';
import { ArrowLeft, BellOff, Briefcase, TrendingUp } from 'lucide-react';
import { useUiStore } from '@/stores/uiStore';
import { useNotifications } from '../api/notificationApi';
import { useIntersectionObserver } from '@/hooks/useIntersectionObserver';

export function NotificationOverlay() {
    const { isNotificationsOpen, closeNotifications, unmountNotifications } = useUiStore();
    
    const { data, fetchNextPage, hasNextPage, isFetchingNextPage, isLoading, isError } = useNotifications();
    const notifications = data?.pages.flatMap(page => page.content) || [];
    
    const { targetRef } = useIntersectionObserver({
        onIntersect: fetchNextPage,
        enabled: hasNextPage && !isFetchingNextPage,
    });

    const overlayRef = useRef<HTMLDivElement>(null);
    const { contextSafe } = useGSAP({ scope: overlayRef });

    useEffect(() => {
        contextSafe(() => {
            if (isNotificationsOpen) {
                gsap.to(overlayRef.current, { y: 0, opacity: 1, duration: 0.4, ease: "power3.out" });
            } else {
                gsap.to(overlayRef.current, {
                    y: "100%",
                    opacity: 0,
                    duration: 0.3,
                    ease: "power2.in",
                    onComplete: unmountNotifications
                });
            }
        })();
    }, [isNotificationsOpen, unmountNotifications, contextSafe]);

    return (
        <div
            ref={overlayRef}
            className="absolute inset-0 z-[100] bg-white flex justify-center translate-y-full opacity-0"
        >
            <div className="w-full max-w-[480px] flex flex-col px-6 pt-10">
                <div className="flex items-center justify-between mb-8">
                    <div className="flex items-center gap-3">
                        <button
                            onClick={closeNotifications}
                            className="p-2 bg-slate-50 border border-slate-100 rounded-full text-slate-600 shadow-sm active:scale-90 transition-transform"
                        >
                            <ArrowLeft size={20} />
                        </button>
                        <h3 className="text-xl font-black text-slate-900 uppercase tracking-tighter">
                            알림
                        </h3>
                    </div>
                </div>

                <div className="flex-1 overflow-y-auto no-scrollbar space-y-3 pb-10">
                    {isLoading ? (
                        <div className="flex justify-center py-10">
                            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-slate-400"></div>
                        </div>
                    ) : isError ? (
                        <div className="text-center py-20 opacity-50">
                            <p className="text-sm font-bold text-slate-500">알림을 불러오지 못했습니다.</p>
                        </div>
                    ) : notifications.length > 0 ? (
                        <>
                            {notifications.map((n) => (
                                <div
                                    key={n.id}
                                    className={`p-5 rounded-3xl border transition-all ${!n.isRead ? 'bg-indigo-50/30 border-indigo-100/50' : 'bg-white border-slate-100 shadow-sm'}`}
                                >
                                    <div className="flex items-start gap-4">
                                        <div className={`w-10 h-10 rounded-2xl flex items-center justify-center shrink-0 ${!n.isRead ? 'bg-blue-100 text-blue-600' : 'bg-slate-100 text-slate-500'}`}>
                                            {!n.isRead ? <TrendingUp size={18} /> : <Briefcase size={18} />}
                                        </div>
                                        <div className="flex-1">
                                            <div className="flex justify-between items-start mb-1">
                                                <h4 className="text-xs font-black text-slate-800 uppercase tracking-tight">{n.title}</h4>
                                                <span className="text-[9px] font-bold text-slate-400">{new Date(n.createdAt).toLocaleDateString()}</span>
                                            </div>
                                            <p className="text-[13px] text-slate-600 font-medium leading-snug">{n.message}</p>
                                        </div>
                                    </div>
                                </div>
                            ))}
                            {hasNextPage && (
                                <div ref={targetRef} className="py-4 flex justify-center">
                                    {isFetchingNextPage ? (
                                        <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-slate-400"></div>
                                    ) : (
                                        <div className="h-5"></div>
                                    )}
                                </div>
                            )}
                        </>
                    ) : (
                        <div className="flex flex-col items-center justify-center py-32 opacity-30">
                            <BellOff size={48} className="mb-4" strokeWidth={1.5} />
                            <p className="text-[11px] font-black uppercase tracking-widest text-slate-500 text-center">
                                알림 목록이 비어있습니다
                            </p>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
