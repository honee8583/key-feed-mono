import { useRef, useEffect } from 'react';
import gsap from 'gsap';
import { useGSAP } from '@gsap/react';
import { ArrowLeft, BellOff, Briefcase, ShieldCheck, TrendingUp } from 'lucide-react';
import { useUiStore } from '@/stores/uiStore';
import { useNotificationStore } from '@/stores/notificationStore';

export function NotificationOverlay() {
    const { isNotificationsOpen, closeNotifications, unmountNotifications } = useUiStore();
    const { notifications, clearAll } = useNotificationStore();
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
                    {notifications.length > 0 && (
                        <button
                            onClick={clearAll}
                            className="text-[10px] font-black uppercase text-slate-400 hover:text-slate-600 transition-colors"
                        >
                            전체 삭제
                        </button>
                    )}
                </div>

                <div className="flex-1 overflow-y-auto no-scrollbar space-y-3 pb-10">
                    {notifications.length > 0 ? (
                        notifications.map((n) => (
                            <div
                                key={n.id}
                                className={`p-5 rounded-3xl border transition-all ${n.unread ? 'bg-indigo-50/30 border-indigo-100/50' : 'bg-white border-slate-100 shadow-sm'}`}
                            >
                                <div className="flex items-start gap-4">
                                    <div className={`w-10 h-10 rounded-2xl flex items-center justify-center shrink-0 ${n.type === 'post' ? 'bg-blue-100 text-blue-600' : n.type === 'system' ? 'bg-emerald-100 text-emerald-600' : 'bg-amber-100 text-amber-600'}`}>
                                        {n.type === 'post' ? <Briefcase size={18} /> : n.type === 'system' ? <ShieldCheck size={18} /> : <TrendingUp size={18} />}
                                    </div>
                                    <div className="flex-1">
                                        <div className="flex justify-between items-start mb-1">
                                            <h4 className="text-xs font-black text-slate-800 uppercase tracking-tight">{n.title}</h4>
                                            <span className="text-[9px] font-bold text-slate-400">{n.time}</span>
                                        </div>
                                        <p className="text-[13px] text-slate-600 font-medium leading-snug">{n.body}</p>
                                    </div>
                                </div>
                            </div>
                        ))
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
