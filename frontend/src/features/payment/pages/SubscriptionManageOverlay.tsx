import { useRef, useEffect } from 'react';
import { ArrowLeft, Crown, CreditCard, AlertCircle, CheckCircle2, Clock, XCircle, Loader2, RotateCcw } from 'lucide-react';
import gsap from 'gsap';
import { useGSAP } from '@gsap/react';
import { useUiStore } from '@/stores/uiStore';
import { useMySubscription, useCancelSubscription, useResumeSubscription, useRefundSubscription } from '../api/subscriptionApi';
import { usePaymentMethods } from '../api/paymentApi';
import type { Subscription } from '../types';

function formatDate(dateStr?: string | null): string {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' });
}

function InfoRow({ label, value }: { label: string; value: string }) {
    return (
        <div className="flex items-center justify-between">
            <span className="text-[11px] font-bold text-slate-400">{label}</span>
            <span className="text-[12px] font-bold text-slate-700">{value}</span>
        </div>
    );
}

function InfoCard({ subscription }: { subscription: Subscription }) {
    return (
        <div className="bg-white border border-slate-100 rounded-2xl p-5 space-y-3.5 shadow-sm">
            <InfoRow label="플랜" value="TECHSTACK PRO" />
            {subscription.startedAt && (
                <InfoRow label="구독 시작일" value={formatDate(subscription.startedAt)} />
            )}
            {subscription.price != null && (
                <InfoRow label="결제 금액" value={`₩${subscription.price.toLocaleString()}/월`} />
            )}
            {subscription.providerName && (
                <InfoRow
                    label="결제 수단"
                    value={`${subscription.providerName} ${subscription.displayNumber ?? ''}`}
                />
            )}
            {subscription.status === 'ACTIVE' && subscription.nextBillingAt && (
                <InfoRow label="다음 결제일" value={formatDate(subscription.nextBillingAt)} />
            )}
            {subscription.status !== 'ACTIVE' && subscription.expiredAt && (
                <InfoRow label="만료일" value={formatDate(subscription.expiredAt)} />
            )}
            {subscription.canceledAt && (
                <InfoRow label="해지 신청일" value={formatDate(subscription.canceledAt)} />
            )}
        </div>
    );
}

const STATUS_CONFIG = {
    emerald: 'bg-emerald-50 text-emerald-600 border-emerald-100',
    amber:   'bg-amber-50 text-amber-600 border-amber-100',
    rose:    'bg-rose-50 text-rose-600 border-rose-100',
};

function StatusBanner({
    color,
    icon,
    label,
    desc,
}: {
    color: keyof typeof STATUS_CONFIG;
    icon: React.ReactNode;
    label: string;
    desc: string;
}) {
    return (
        <div className={`flex items-start gap-3 p-4 rounded-2xl border mb-4 ${STATUS_CONFIG[color]}`}>
            <div className="mt-0.5 shrink-0">{icon}</div>
            <div>
                <p className="text-sm font-black">{label}</p>
                <p className="text-xs opacity-80 mt-0.5">{desc}</p>
            </div>
        </div>
    );
}

export function SubscriptionManageOverlay() {
    const { isSubscriptionOpen, closeSubscriptionManage, unmountSubscriptionManage } = useUiStore();
    const overlayRef = useRef<HTMLDivElement>(null);
    const { contextSafe } = useGSAP({ scope: overlayRef });

    const { data: subscription, status: subStatus } = useMySubscription();
    const { data: methods } = usePaymentMethods();
    const { mutateAsync: cancelAsync, isPending: isCanceling } = useCancelSubscription();
    const { mutate: resume, isPending: isResuming } = useResumeSubscription();
    const { mutateAsync: refundAsync, isPending: isRefunding } = useRefundSubscription();

    const defaultMethod = methods?.find((m) => m.isDefault) ?? methods?.[0];

    const isWithin1Day = (() => {
        if (!subscription?.startedAt) return false;
        return Date.now() - new Date(subscription.startedAt).getTime() < 24 * 60 * 60 * 1000;
    })();

    useEffect(() => {
        contextSafe(() => {
            if (isSubscriptionOpen) {
                gsap.to(overlayRef.current, { x: 0, opacity: 1, duration: 0.4, ease: 'power3.out' });
            } else {
                gsap.to(overlayRef.current, {
                    x: '100%',
                    opacity: 0,
                    duration: 0.3,
                    ease: 'power2.in',
                    onComplete: unmountSubscriptionManage,
                });
            }
        })();
    }, [isSubscriptionOpen, unmountSubscriptionManage, contextSafe]);

    const handleRefund = async () => {
        if (!confirm('구독을 즉시 취소하고 환불받으시겠습니까?')) return;
        try {
            await refundAsync(undefined);
        } catch (err: any) {
            alert(err?.response?.data?.message || '구독 취소에 실패했습니다.');
        }
    };

    const handleCancel = async () => {
        if (!confirm('구독을 해지하시겠습니까?\n만료일까지 서비스는 계속 이용 가능합니다.')) return;
        try {
            await cancelAsync(undefined);
        } catch (err: any) {
            alert(err?.response?.data?.message || '구독 해지에 실패했습니다.');
        }
    };

    const handleResume = () => {
        if (!defaultMethod) {
            alert('등록된 결제 수단이 없습니다.');
            return;
        }
        resume(defaultMethod.methodId, {
            onError: (err: any) => {
                alert(err?.response?.data?.message || '구독 재개에 실패했습니다.');
            },
        });
    };

    return (
        <div
            ref={overlayRef}
            className="absolute inset-0 z-[100] bg-[#F8FAFC] flex justify-center translate-x-full opacity-0 overflow-y-auto"
        >
            <div className="w-full max-w-[480px] flex flex-col min-h-screen">
                <div className="flex items-center gap-3 px-5 pt-10 pb-6 sticky top-0 bg-[#F8FAFC]/80 backdrop-blur-xl z-10">
                    <button
                        onClick={closeSubscriptionManage}
                        className="p-2 bg-white border border-slate-200 rounded-full text-slate-600 shadow-sm active:scale-90 transition-transform"
                    >
                        <ArrowLeft size={20} />
                    </button>
                    <h2 className="text-[18px] font-black text-slate-800 tracking-tight">구독 관리</h2>
                </div>

                <div className="px-5 pb-24 flex-1">
                    {subStatus === 'pending' ? (
                        <div className="flex justify-center py-20">
                            <Loader2 className="w-8 h-8 animate-spin text-slate-300" />
                        </div>
                    ) : subStatus === 'error' ? (
                        <div className="text-center py-20 opacity-50">
                            <p className="text-[12px] font-bold text-slate-500">조회에 실패했습니다.</p>
                        </div>
                    ) : subscription?.status === 'ACTIVE' ? (
                        <>
                            <StatusBanner
                                color="emerald"
                                icon={<CheckCircle2 size={20} />}
                                label="구독 활성"
                                desc="TECHSTACK PRO가 활성화되어 있습니다."
                            />
                            <InfoCard subscription={subscription} />
                            {isWithin1Day ? (
                                <button
                                    onClick={handleRefund}
                                    disabled={isRefunding}
                                    className="w-full mt-6 py-4 rounded-2xl bg-rose-50 text-rose-600 text-[13px] font-black border border-rose-100 active:scale-95 transition-transform flex items-center justify-center gap-2 disabled:opacity-50"
                                >
                                    {isRefunding
                                        ? <Loader2 size={16} className="animate-spin" />
                                        : <RotateCcw size={16} />
                                    }
                                    구독 취소
                                </button>
                            ) : (
                                <button
                                    onClick={handleCancel}
                                    disabled={isCanceling}
                                    className="w-full mt-6 py-4 rounded-2xl bg-rose-50 text-rose-600 text-[13px] font-black border border-rose-100 active:scale-95 transition-transform flex items-center justify-center gap-2 disabled:opacity-50"
                                >
                                    {isCanceling
                                        ? <Loader2 size={16} className="animate-spin" />
                                        : <XCircle size={16} />
                                    }
                                    구독 해지
                                </button>
                            )}
                        </>
                    ) : subscription?.status === 'CANCELED' ? (
                        <>
                            <StatusBanner
                                color="amber"
                                icon={<Clock size={20} />}
                                label="해지 예정"
                                desc={`${formatDate(subscription.expiredAt)}까지 서비스가 유지됩니다.`}
                            />
                            <InfoCard subscription={subscription} />
                        </>
                    ) : subscription?.status === 'PAUSED' ? (
                        <>
                            <StatusBanner
                                color="rose"
                                icon={<AlertCircle size={20} />}
                                label="결제 실패 — 일시 정지"
                                desc="결제에 실패하여 구독이 일시 정지되었습니다. 결제 수단을 확인해주세요."
                            />
                            <InfoCard subscription={subscription} />

                            {defaultMethod ? (
                                <div className="mt-6 bg-white border border-slate-100 rounded-2xl p-4 shadow-sm">
                                    <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-3">
                                        재개에 사용할 결제 수단
                                    </p>
                                    <div className="flex items-center gap-3">
                                        <div className="w-10 h-10 bg-slate-50 rounded-xl flex items-center justify-center text-slate-500 border border-slate-100">
                                            <CreditCard size={18} />
                                        </div>
                                        <div>
                                            <p className="text-sm font-bold text-slate-800">{defaultMethod.providerName}</p>
                                            <p className="text-[11px] text-slate-400 font-mono tracking-wider">{defaultMethod.displayNumber}</p>
                                        </div>
                                    </div>
                                </div>
                            ) : (
                                <div className="mt-6 p-4 bg-slate-50 rounded-2xl border border-slate-100 text-center">
                                    <p className="text-[12px] font-bold text-slate-500">
                                        등록된 결제 수단이 없습니다. 결제 수단을 먼저 등록해주세요.
                                    </p>
                                </div>
                            )}

                            <button
                                onClick={handleResume}
                                disabled={isResuming || !defaultMethod}
                                className="w-full mt-4 py-4 rounded-2xl bg-slate-900 text-white text-[13px] font-black active:scale-95 transition-transform flex items-center justify-center gap-2 disabled:opacity-40"
                            >
                                {isResuming && <Loader2 size={16} className="animate-spin" />}
                                구독 재개하기
                            </button>
                        </>
                    ) : subscription?.status === 'REFUNDED' ? (
                        <>
                            <StatusBanner
                                color="rose"
                                icon={<RotateCcw size={20} />}
                                label="환불 완료"
                                desc="구독이 즉시 취소되었으며 환불이 처리되었습니다."
                            />
                            <InfoCard subscription={subscription} />
                        </>
                    ) : (
                        <div className="text-center py-20 opacity-40 flex flex-col items-center">
                            <Crown size={40} className="mb-4 text-slate-300" strokeWidth={1.5} />
                            <p className="text-[11px] font-bold text-slate-500">활성 구독이 없습니다.</p>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
