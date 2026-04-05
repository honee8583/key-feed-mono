import { useRef, useEffect } from 'react';
import { ArrowLeft, Loader2, CreditCard, Trash2, Plus, ChevronRight } from 'lucide-react';
import gsap from 'gsap';
import { useGSAP } from '@gsap/react';
import { loadTossPayments } from '@tosspayments/tosspayments-sdk';
import { useUiStore } from '@/stores/uiStore';
import { usePaymentMethods, useDeletePaymentMethod, useSetDefaultPaymentMethod, getCustomerKey } from '../api/paymentApi';
import { useMySubscription, useCancelSubscription, useRefundSubscription } from '../api/subscriptionApi';

const clientKey = import.meta.env.VITE_TOSS_CLIENT_KEY || "test_ck_D5GePWvyJnrK0W0k6q8gLzN97Eoq";

function formatDate(dateStr?: string | null): string {
    if (!dateStr) return '-';
    const d = new Date(dateStr);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

const SUBSCRIPTION_STATUS_LABEL: Record<string, { text: string; className: string }> = {
    ACTIVE:   { text: '구독 중',   className: 'bg-emerald-100 text-emerald-600' },
    CANCELED: { text: '해지 예정', className: 'bg-amber-100 text-amber-600' },
    PAUSED:   { text: '결제 실패', className: 'bg-rose-100 text-rose-500' },
};

export function PaymentMethodManageOverlay() {
    const { isPaymentMethodOpen, closePaymentMethod, unmountPaymentMethod, openPaymentHistory } = useUiStore();
    const overlayRef = useRef<HTMLDivElement>(null);
    const { contextSafe } = useGSAP({ scope: overlayRef });

    const { data: methods, status: methodStatus } = usePaymentMethods();
    const { mutate: deleteMethod, isPending: isDeleting } = useDeletePaymentMethod();
    const { mutate: setDefaultMethod, isPending: isSettingDefault } = useSetDefaultPaymentMethod();
    const { data: subscription } = useMySubscription();
    const { mutate: cancelSub, isPending: isCanceling } = useCancelSubscription();
    const { mutate: refundSub, isPending: isRefunding } = useRefundSubscription();

    useEffect(() => {
        contextSafe(() => {
            if (isPaymentMethodOpen) {
                gsap.to(overlayRef.current, { x: 0, opacity: 1, duration: 0.4, ease: 'power3.out' });
            } else {
                gsap.to(overlayRef.current, {
                    x: '100%',
                    opacity: 0,
                    duration: 0.3,
                    ease: 'power2.in',
                    onComplete: unmountPaymentMethod,
                });
            }
        })();
    }, [isPaymentMethodOpen, unmountPaymentMethod, contextSafe]);

    const handleAddCard = async () => {
        try {
            const customerKey = await getCustomerKey();
            console.log('[Toss] customerKey:', customerKey);
            const tossPayments = await loadTossPayments(clientKey);
            const payment = tossPayments.payment({ customerKey });
            await payment.requestBillingAuth({
                method: 'CARD',
                successUrl: window.location.origin + '/payment/callback',
                failUrl: window.location.origin + '/payment/callback?fail=true',
            });
        } catch (error) {
            console.error('결제창 연동 오류', error);
            alert('결제 창을 불러올 수 없습니다.');
        }
    };

    const isWithin1Day = (() => {
        if (!subscription?.startedAt) return false;
        return Date.now() - new Date(subscription.startedAt).getTime() < 24 * 60 * 60 * 1000;
    })();

    const handleCancelSubscription = () => {
        if (!confirm('구독을 해지하시겠습니까?\n만료일까지 서비스는 계속 이용 가능합니다.')) return;
        cancelSub(undefined, {
            onError: (err: any) => {
                alert(err?.response?.data?.message || '구독 해지에 실패했습니다.');
            },
        });
    };

    const handleRefundSubscription = () => {
        if (!confirm('구독을 즉시 취소하고 환불받으시겠습니까?')) return;
        refundSub(undefined, {
            onError: (err: any) => {
                alert(err?.response?.data?.message || '구독 취소에 실패했습니다.');
            },
        });
    };

    const isSubscribed =
        subscription?.status === 'ACTIVE' ||
        subscription?.status === 'CANCELED' ||
        subscription?.status === 'PAUSED';

    const statusConfig = subscription?.status ? SUBSCRIPTION_STATUS_LABEL[subscription.status] : null;

    return (
        <div
            ref={overlayRef}
            className="absolute inset-0 z-[100] bg-[#F8FAFC] flex justify-center translate-x-full opacity-0 overflow-y-auto"
        >
            <div className="w-full max-w-[480px] flex flex-col min-h-screen">

                {/* Header */}
                <div className="flex items-center gap-3 px-5 pt-10 pb-6 sticky top-0 bg-[#F8FAFC]/80 backdrop-blur-xl z-10">
                    <button
                        onClick={closePaymentMethod}
                        className="p-2 bg-white border border-slate-200 rounded-full text-slate-600 shadow-sm active:scale-90 transition-transform"
                    >
                        <ArrowLeft size={20} />
                    </button>
                    <h2 className="text-[18px] font-black text-slate-800 tracking-tight">결제 및 구독</h2>
                </div>

                <div className="px-5 pb-24 flex-1 space-y-5">

                    {/* 현재 플랜 */}
                    {isSubscribed && statusConfig && (
                        <div className="bg-white rounded-2xl p-5 shadow-sm border border-slate-100">
                            <div className="flex items-center justify-between mb-3">
                                <span className="text-[11px] font-bold text-slate-400">현재 플랜</span>
                                <span className={`text-[10px] font-black px-2.5 py-1 rounded-full ${statusConfig.className}`}>
                                    {statusConfig.text}
                                </span>
                            </div>
                            <p className="text-[20px] font-black text-slate-800 tracking-tight mb-1">플랜 월간</p>
                            <p className="text-sm font-bold text-slate-600">
                                ₩{(subscription?.price ?? 9900).toLocaleString()} / 월
                            </p>
                            {subscription?.status === 'ACTIVE' && subscription.nextBillingAt && (
                                <p className="text-[11px] text-slate-400 font-medium mt-1">
                                    다음 결제: {formatDate(subscription.nextBillingAt)}
                                </p>
                            )}
                            {subscription?.status === 'CANCELED' && subscription.expiredAt && (
                                <p className="text-[11px] text-slate-400 font-medium mt-1">
                                    만료일: {formatDate(subscription.expiredAt)}
                                </p>
                            )}
                            {subscription?.status === 'ACTIVE' && (
                                <div className="mt-3 flex justify-end">
                                    {isWithin1Day ? (
                                        <button
                                            onClick={handleRefundSubscription}
                                            disabled={isRefunding}
                                            className="flex items-center gap-1 text-[11px] font-bold text-rose-400 hover:text-rose-500 transition-colors disabled:opacity-50"
                                        >
                                            {isRefunding && <Loader2 size={11} className="animate-spin" />}
                                            구독 취소
                                        </button>
                                    ) : (
                                        <button
                                            onClick={handleCancelSubscription}
                                            disabled={isCanceling}
                                            className="flex items-center gap-1 text-[11px] font-bold text-rose-400 hover:text-rose-500 transition-colors disabled:opacity-50"
                                        >
                                            {isCanceling && <Loader2 size={11} className="animate-spin" />}
                                            구독 해지
                                        </button>
                                    )}
                                </div>
                            )}
                        </div>
                    )}

                    {/* 결제 수단 */}
                    <div>
                        <div className="flex items-center justify-between mb-3">
                            <span className="text-[11px] font-black text-slate-400 uppercase tracking-widest">결제 수단</span>
                            <button
                                onClick={handleAddCard}
                                className="flex items-center gap-1 text-[11px] font-bold text-indigo-500 hover:text-indigo-600 transition-colors active:scale-95"
                            >
                                <Plus size={13} strokeWidth={2.5} />
                                추가
                            </button>
                        </div>

                        {methodStatus === 'pending' ? (
                            <div className="flex justify-center py-10">
                                <Loader2 className="w-6 h-6 animate-spin text-slate-300" />
                            </div>
                        ) : methodStatus === 'error' ? (
                            <div className="bg-white rounded-2xl p-5 border border-slate-100 text-center">
                                <p className="text-[12px] font-bold text-slate-400">조회에 실패했습니다.</p>
                            </div>
                        ) : methods && methods.length > 0 ? (
                            <div className="space-y-2">
                                {methods.map((method) => (
                                    <div
                                        key={method.methodId}
                                        className="bg-white rounded-2xl px-4 py-3.5 border border-slate-100 shadow-sm flex items-center justify-between"
                                    >
                                        <div className="flex items-center gap-3">
                                            <div className="w-10 h-10 rounded-xl bg-indigo-500 flex items-center justify-center text-white shadow-sm shrink-0">
                                                <CreditCard size={17} strokeWidth={2} />
                                            </div>
                                            <div>
                                                <div className="flex items-center gap-2 mb-0.5">
                                                    <p className="text-[13px] font-black text-slate-800">
                                                        {method.providerName ?? '카드'}
                                                    </p>
                                                    {method.isDefault && (
                                                        <span className="text-[9px] font-black px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-600">
                                                            기본
                                                        </span>
                                                    )}
                                                </div>
                                                <p className="text-[11px] text-slate-400 font-mono tracking-wider">
                                                    {method.displayNumber}
                                                </p>
                                            </div>
                                        </div>

                                        <div className="flex items-center gap-1.5 shrink-0">
                                            {!method.isDefault && (
                                                <button
                                                    disabled={isSettingDefault}
                                                    onClick={() => setDefaultMethod(method.methodId)}
                                                    className="text-[10px] font-bold text-slate-400 hover:text-slate-600 px-2.5 py-1.5 rounded-lg hover:bg-slate-50 transition-colors disabled:opacity-40"
                                                >
                                                    기본으로 설정
                                                </button>
                                            )}
                                            <button
                                                disabled={isDeleting}
                                                onClick={() => deleteMethod(method.methodId, {
                                                    onError: (err: any) => {
                                                        alert(err?.response?.data?.message || '결제 수단 삭제에 실패했습니다.');
                                                    },
                                                })}
                                                className="w-7 h-7 flex items-center justify-center bg-rose-50 text-rose-400 rounded-lg hover:bg-rose-100 transition-colors disabled:opacity-40"
                                            >
                                                <Trash2 size={13} />
                                            </button>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        ) : (
                            <div className="bg-white rounded-2xl p-6 border border-slate-100 text-center">
                                <CreditCard size={28} className="mx-auto mb-2 text-slate-200" strokeWidth={1.5} />
                                <p className="text-[11px] font-bold text-slate-400">등록된 결제 수단이 없습니다.</p>
                            </div>
                        )}
                    </div>

                    {/* 결제 내역 */}
                    <button
                        onClick={openPaymentHistory}
                        className="w-full bg-white rounded-2xl px-5 py-4 border border-slate-100 shadow-sm flex items-center justify-between active:scale-[0.98] transition-transform"
                    >
                        <span className="text-[13px] font-bold text-slate-700">결제 내역 보기</span>
                        <ChevronRight size={16} className="text-slate-300" />
                    </button>

                </div>
            </div>
        </div>
    );
}
