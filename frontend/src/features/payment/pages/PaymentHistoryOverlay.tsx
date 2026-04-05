import { useRef, useEffect } from 'react';
import { ArrowLeft, Loader2, CreditCard, CheckCircle2, XCircle, RotateCcw } from 'lucide-react';
import gsap from 'gsap';
import { useGSAP } from '@gsap/react';
import { useUiStore } from '@/stores/uiStore';
import { usePaymentHistory } from '../api/paymentHistoryApi';
import type { PaymentHistoryItem } from '../types';

function formatDateTime(dateStr?: string | null): string {
    if (!dateStr) return '-';
    const d = new Date(dateStr);
    return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}

const STATUS_CONFIG: Record<string, { label: string; icon: React.ReactNode; className: string }> = {
    DONE:     { label: '결제 완료', icon: <CheckCircle2 size={13} />, className: 'text-emerald-600 bg-emerald-50' },
    FAILED:   { label: '결제 실패', icon: <XCircle size={13} />,     className: 'text-rose-500 bg-rose-50' },
    CANCELED: { label: '환불 완료', icon: <RotateCcw size={13} />,   className: 'text-slate-500 bg-slate-100' },
};

function HistoryItem({ item }: { item: PaymentHistoryItem }) {
    const s = STATUS_CONFIG[item.status] ?? STATUS_CONFIG.FAILED;
    return (
        <div className="bg-white rounded-2xl px-4 py-4 border border-slate-100 shadow-sm">
            <div className="flex items-start justify-between mb-2">
                <p className="text-[13px] font-black text-slate-800">{item.orderName}</p>
                <span className={`flex items-center gap-1 text-[10px] font-black px-2 py-0.5 rounded-full shrink-0 ml-2 ${s.className}`}>
                    {s.icon}{s.label}
                </span>
            </div>
            <p className="text-[18px] font-black text-slate-900 mb-2">
                ₩{item.amount.toLocaleString()}
            </p>
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-1.5 text-slate-400">
                    <CreditCard size={11} />
                    <span className="text-[11px] font-medium">
                        {item.paymentMethod.providerName} {item.paymentMethod.displayNumber}
                    </span>
                </div>
                <span className="text-[10px] text-slate-400 font-medium">
                    {formatDateTime(item.approvedAt ?? item.createdAt)}
                </span>
            </div>
            {item.failReason && (
                <p className="mt-1.5 text-[10px] text-rose-400 font-medium">{item.failReason}</p>
            )}
        </div>
    );
}

export function PaymentHistoryOverlay() {
    const { isPaymentHistoryOpen, closePaymentHistory, unmountPaymentHistory } = useUiStore();
    const overlayRef = useRef<HTMLDivElement>(null);
    const { contextSafe } = useGSAP({ scope: overlayRef });

    const {
        data,
        status,
        fetchNextPage,
        hasNextPage,
        isFetchingNextPage,
    } = usePaymentHistory();

    const items = data?.pages.flatMap((p) => p.content) ?? [];

    useEffect(() => {
        contextSafe(() => {
            if (isPaymentHistoryOpen) {
                gsap.to(overlayRef.current, { x: 0, opacity: 1, duration: 0.4, ease: 'power3.out' });
            } else {
                gsap.to(overlayRef.current, {
                    x: '100%',
                    opacity: 0,
                    duration: 0.3,
                    ease: 'power2.in',
                    onComplete: unmountPaymentHistory,
                });
            }
        })();
    }, [isPaymentHistoryOpen, unmountPaymentHistory, contextSafe]);

    return (
        <div
            ref={overlayRef}
            className="absolute inset-0 z-[110] bg-[#F8FAFC] flex justify-center translate-x-full opacity-0 overflow-y-auto"
        >
            <div className="w-full max-w-[480px] flex flex-col min-h-screen">
                <div className="flex items-center gap-3 px-5 pt-10 pb-6 sticky top-0 bg-[#F8FAFC]/80 backdrop-blur-xl z-10">
                    <button
                        onClick={closePaymentHistory}
                        className="p-2 bg-white border border-slate-200 rounded-full text-slate-600 shadow-sm active:scale-90 transition-transform"
                    >
                        <ArrowLeft size={20} />
                    </button>
                    <h2 className="text-[18px] font-black text-slate-800 tracking-tight">결제 내역</h2>
                </div>

                <div className="px-5 pb-24 flex-1 space-y-2">
                    {status === 'pending' ? (
                        <div className="flex justify-center py-20">
                            <Loader2 className="w-8 h-8 animate-spin text-slate-300" />
                        </div>
                    ) : status === 'error' ? (
                        <div className="text-center py-20">
                            <p className="text-[12px] font-bold text-slate-400">조회에 실패했습니다.</p>
                        </div>
                    ) : items.length === 0 ? (
                        <div className="text-center py-20">
                            <CreditCard size={36} className="mx-auto mb-3 text-slate-200" strokeWidth={1.5} />
                            <p className="text-[12px] font-bold text-slate-400">결제 내역이 없습니다.</p>
                        </div>
                    ) : (
                        <>
                            {items.map((item) => (
                                <HistoryItem key={item.paymentId} item={item} />
                            ))}
                            {hasNextPage && (
                                <button
                                    onClick={() => fetchNextPage()}
                                    disabled={isFetchingNextPage}
                                    className="w-full py-3.5 rounded-2xl bg-white border border-slate-200 text-[12px] font-bold text-slate-500 flex items-center justify-center gap-2 active:scale-95 transition-transform disabled:opacity-50"
                                >
                                    {isFetchingNextPage
                                        ? <Loader2 size={14} className="animate-spin" />
                                        : '더 보기'
                                    }
                                </button>
                            )}
                        </>
                    )}
                </div>
            </div>
        </div>
    );
}
