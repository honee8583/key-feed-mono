import { useRef, useEffect } from 'react';
import { ArrowLeft, Loader2, CreditCard, Trash2, PlusCircle } from 'lucide-react';
import gsap from 'gsap';
import { useGSAP } from '@gsap/react';
import { loadTossPayments } from '@tosspayments/tosspayments-sdk';
import { useUiStore } from '@/stores/uiStore';
import { usePaymentMethods, useDeletePaymentMethod, useSetDefaultPaymentMethod, getCustomerKey } from '../api/paymentApi';

const clientKey = import.meta.env.VITE_TOSS_CLIENT_KEY || "test_ck_D5GePWvyJnrK0W0k6q8gLzN97Eoq";

export function PaymentMethodManageOverlay() {
    const { isPaymentMethodOpen, closePaymentMethod, unmountPaymentMethod } = useUiStore();
    const overlayRef = useRef<HTMLDivElement>(null);
    const { contextSafe } = useGSAP({ scope: overlayRef });

    const { data: methods, status } = usePaymentMethods();
    const { mutate: deleteMethod, isPending: isDeleting } = useDeletePaymentMethod();
    const { mutate: setDefaultMethod, isPending: isSettingDefault } = useSetDefaultPaymentMethod();

    useEffect(() => {
        contextSafe(() => {
            if (isPaymentMethodOpen) {
                gsap.to(overlayRef.current, { x: 0, opacity: 1, duration: 0.4, ease: "power3.out" });
            } else {
                gsap.to(overlayRef.current, { x: "100%", opacity: 0, duration: 0.3, ease: "power2.in", onComplete: unmountPaymentMethod });
            }
        })();
    }, [isPaymentMethodOpen, unmountPaymentMethod, contextSafe]);

    const handleAddCard = async () => {
        try {
            const customerKey = await getCustomerKey();
            console.log('[Toss] customerKey:', customerKey);
            const tossPayments = await loadTossPayments(clientKey);
            const payment = tossPayments.payment({ customerKey });
            
            // 토스페이먼츠창 호의 (성공/에러 리다이렉트)
            await payment.requestBillingAuth({
                method: "CARD",
                successUrl: window.location.origin + "/payment/callback",
                failUrl: window.location.origin + "/payment/callback?fail=true",
            });
        } catch (error) {
            console.error("결제창 연동 오류", error);
            alert("결제 창을 불러올 수 없습니다.");
        }
    };

    return (
        <div 
            ref={overlayRef}
            className="absolute inset-0 z-[100] bg-[#F8FAFC] flex justify-center translate-x-full opacity-0 overflow-y-auto"
        >
            <div className="w-full max-w-[480px] flex flex-col min-h-screen">
                <div className="flex items-center gap-3 px-5 pt-10 pb-6 sticky top-0 bg-[#F8FAFC]/80 backdrop-blur-xl z-10">
                    <button
                        onClick={closePaymentMethod}
                        className="p-2 bg-white border border-slate-200 rounded-full text-slate-600 shadow-sm active:scale-90 transition-transform"
                    >
                        <ArrowLeft size={20} />
                    </button>
                    <h2 className="text-[18px] font-black text-slate-800 tracking-tight">결제 수단 관리</h2>
                </div>

                <div className="px-5 pb-24 flex-1">
                    <button
                        onClick={handleAddCard}
                        className="w-full bg-white border border-slate-200 border-dashed rounded-3xl p-5 mb-6 flex flex-col items-center justify-center gap-3 active:scale-[0.98] transition-transform group shadow-sm hover:border-indigo-300 hover:bg-indigo-50/30"
                    >
                        <div className="w-12 h-12 bg-slate-50 group-hover:bg-white rounded-full flex items-center justify-center text-indigo-500 shadow-inner group-hover:shadow-lg group-hover:shadow-indigo-500/20 transition-all">
                            <PlusCircle size={24} strokeWidth={2} />
                        </div>
                        <span className="text-sm font-bold text-slate-700">새로운 결제 수단 등록</span>
                    </button>

                    <h4 className="text-[10px] font-black text-slate-400 uppercase tracking-widest px-2 mb-3">
                        등록된 카드
                    </h4>

                    {status === 'pending' ? (
                        <div className="flex justify-center py-20">
                            <Loader2 className="w-8 h-8 animate-spin text-slate-300" />
                        </div>
                    ) : status === 'error' ? (
                        <div className="text-center py-20 opacity-50">
                            <p className="text-[12px] font-bold text-slate-500">조회에 실패했습니다.</p>
                        </div>
                    ) : methods && methods.length > 0 ? (
                        <div className="space-y-3">
                            {methods.map((method) => {
                                const isPrimary = method.isDefault;
                                return (
                                    <div 
                                        key={method.methodId} 
                                        className={`bg-white rounded-[24px] p-5 shadow-sm border transition-colors ${isPrimary ? 'border-amber-400 shadow-[0_4px_20px_rgba(251,191,36,0.1)]' : 'border-slate-100 hover:border-slate-300'}`}
                                    >
                                        <div className="flex items-start justify-between mb-4">
                                            <div className="flex items-center gap-3">
                                                <div className={`w-10 h-10 ${isPrimary ? 'bg-amber-100/50 text-amber-600' : 'bg-slate-50 text-slate-500'} rounded-xl flex items-center justify-center shadow-inner`}>
                                                    <CreditCard size={18} />
                                                </div>
                                                <div>
                                                    <div className="flex items-center gap-2 mb-0.5">
                                                        <h3 className="text-sm font-bold text-slate-800">{method.providerName}</h3>
                                                        {isPrimary && (
                                                            <span className="px-2 py-0.5 rounded-full bg-amber-500 text-white text-[8px] font-black uppercase tracking-wider">Default</span>
                                                        )}
                                                    </div>
                                                    <p className="text-[11px] font-bold text-slate-400 font-mono tracking-widest">
                                                        {method.displayNumber}
                                                    </p>
                                                </div>
                                            </div>
                                        </div>
                                        
                                        <div className="flex items-center justify-end gap-2 border-t border-slate-100 pt-4 mt-2">
                                            {!isPrimary && (
                                                <button
                                                    disabled={isSettingDefault}
                                                    onClick={() => setDefaultMethod(method.methodId)}
                                                    className="px-3.5 py-1.5 bg-slate-50 text-slate-600 text-[10px] font-bold rounded-full hover:bg-slate-100 transition-colors border border-slate-200"
                                                >
                                                    기본 수단으로 설정
                                                </button>
                                            )}
                                            <button
                                                disabled={isDeleting}
                                                onClick={() => deleteMethod(method.methodId)}
                                                className="w-8 h-8 flex items-center justify-center bg-rose-50 text-rose-500 rounded-full hover:bg-rose-100 transition-colors border border-rose-100"
                                            >
                                                <Trash2 size={14} />
                                            </button>
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    ) : (
                        <div className="text-center py-20 opacity-40 flex flex-col items-center">
                            <CreditCard size={40} className="mb-4 text-slate-300" strokeWidth={1.5} />
                            <p className="text-[11px] font-bold text-slate-500">등록된 결제 수단이 없습니다.</p>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
