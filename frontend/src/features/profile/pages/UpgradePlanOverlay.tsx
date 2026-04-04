import { useRef, useEffect } from 'react';
import gsap from 'gsap';
import { useGSAP } from '@gsap/react';
import { loadTossPayments } from '@tosspayments/tosspayments-sdk';
import { ArrowLeft, Bookmark, Folder, Star, Zap, CheckCircle2, Crown, Sparkles, Tag } from 'lucide-react';
import { useUiStore } from '@/stores/uiStore';
import { getCustomerKey } from '@/features/payment/api/paymentApi';

const clientKey = import.meta.env.VITE_TOSS_CLIENT_KEY || "test_ck_D5GePWvyJnrK0W0k6q8gLzN97Eoq";

const FEATURES = [
    { icon: Bookmark, title: '무제한 북마크', desc: '원하는 만큼 저장하고 관리하세요' },
    { icon: Folder, title: '무제한 폴더', desc: '폴더를 자유롭게 생성하세요' },
    { icon: Tag, title: '무제한 키워드', desc: '관심 있는 키워드를 제한 없이 추가하세요' },
    { icon: Star, title: '프리미엄 테마', desc: '독점 테마와 아이콘 팩 제공' },
    { icon: Zap, title: 'AI 추천 엔진', desc: '개인화된 콘텐츠 큐레이션' }
];

export function UpgradePlanOverlay() {
    const { isUpgradeOpen, closeUpgradePlan, unmountUpgradePlan } = useUiStore();
    const overlayRef = useRef<HTMLDivElement>(null);
    const { contextSafe } = useGSAP({ scope: overlayRef });

    const handleStartNow = async () => {
        try {
            const customerKey = await getCustomerKey();
            console.log('[Toss] customerKey:', customerKey);
            const tossPayments = await loadTossPayments(clientKey);
            const payment = tossPayments.payment({ customerKey });
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

    useEffect(() => {
        contextSafe(() => {
            if (isUpgradeOpen) {
                // Fade and slight scale intro to mimic AnimatePresence screen/fade transition
                gsap.fromTo(overlayRef.current, 
                    { opacity: 0, scale: 0.98, y: 15 }, 
                    { opacity: 1, scale: 1, y: 0, duration: 0.35, ease: "power2.out" }
                );
            } else {
                gsap.to(overlayRef.current, {
                    opacity: 0,
                    scale: 0.98,
                    duration: 0.25,
                    ease: "power2.in",
                    onComplete: unmountUpgradePlan
                });
            }
        })();
    }, [isUpgradeOpen, unmountUpgradePlan, contextSafe]);

    return (
        <div
            ref={overlayRef}
            className="absolute inset-0 z-[120] bg-slate-100/80 backdrop-blur-xl flex flex-col opacity-0"
        >
            <div className="absolute top-4 left-4 z-10">
                <button
                    onClick={closeUpgradePlan}
                    className="w-10 h-10 bg-white shadow-sm rounded-full flex items-center justify-center text-slate-700 active:scale-90 transition-transform"
                >
                    <ArrowLeft size={20} />
                </button>
            </div>
            <div className="absolute top-4 w-full flex justify-center z-0 pt-2.5">
                <span className="font-black tracking-tighter text-slate-900 text-lg uppercase shadow-sm">UPGRADE PLAN</span>
            </div>

            <div className="flex-1 overflow-y-auto no-scrollbar px-6 pt-24 pb-48">
                <div className="flex flex-col items-center text-center mb-8 relative">
                    <div className="relative mb-5">
                        <div className="absolute -top-3 -right-3 text-amber-500 z-10 animate-bounce">
                            <Sparkles size={28} strokeWidth={2} />
                        </div>
                        <div className="w-24 h-24 bg-gradient-to-tr from-amber-400 to-orange-500 rounded-[2rem] shadow-xl shadow-orange-500/30 flex items-center justify-center text-white border-[6px] border-white z-0 relative">
                            <Crown size={40} strokeWidth={2.5} />
                        </div>
                    </div>
                    <h2 className="text-2xl font-black text-slate-900 tracking-tighter mb-2">TECHSTACK PRO</h2>
                    <p className="text-xs font-bold text-slate-500">프리미엄 기능으로 생산성을 극대화하세요</p>
                </div>

                <div className="space-y-3 mb-10">
                    {FEATURES.map((feat, idx) => {
                        const Icon = feat.icon;
                        return (
                            <div key={idx} className="bg-white border border-slate-100 p-4 rounded-[24px] shadow-[0_4px_20px_rgba(0,0,0,0.03)] flex items-center justify-between group hover:border-orange-200 hover:shadow-[0_8px_30px_rgba(249,115,22,0.1)] transition-all">
                                <div className="flex items-center gap-4">
                                    <div className="w-12 h-12 rounded-[16px] bg-orange-500 flex items-center justify-center text-white shadow-md shadow-orange-500/20 group-hover:scale-105 transition-transform">
                                        <Icon size={20} strokeWidth={2.5} />
                                    </div>
                                    <div>
                                        <h4 className="text-[13px] font-black text-slate-800 mb-0.5">{feat.title}</h4>
                                        <p className="text-[11px] text-slate-400 font-bold">{feat.desc}</p>
                                    </div>
                                </div>
                                <div className="text-emerald-400 pr-2">
                                    <CheckCircle2 size={20} strokeWidth={2.5} />
                                </div>
                            </div>
                        );
                    })}
                </div>

                <div className="bg-[#1a202c] rounded-[32px] p-8 pb-10 text-center text-white relative overflow-hidden shadow-2xl border border-slate-800/50">
                    <div className="absolute top-0 right-0 w-40 h-40 bg-blue-500/10 rounded-full blur-3xl -translate-y-1/2 translate-x-1/2"></div>
                    <div className="absolute bottom-0 left-0 w-40 h-40 bg-orange-500/10 rounded-full blur-3xl translate-y-1/2 -translate-x-1/2"></div>
                    
                    <div className="relative z-10">
                        <div className="flex items-end justify-center gap-1 mb-2">
                            <span className="text-[2.25rem] leading-[1] font-black tracking-tighter">₩9,900</span>
                            <span className="text-xs font-bold text-slate-400 mb-1">/월</span>
                        </div>
                        <p className="text-[10px] font-medium text-slate-400 opacity-80 mb-6">언제든지 취소 가능합니다</p>
                        
                        <div className="flex items-center justify-center gap-4 text-[10px] font-bold text-slate-300">
                            <div className="flex items-center gap-1.5">
                                <CheckCircle2 size={12} className="text-emerald-400" />
                                <span>7일 무료 체험</span>
                            </div>
                            <div className="w-1 h-1 rounded-full bg-slate-600"></div>
                            <div className="flex items-center gap-1.5">
                                <CheckCircle2 size={12} className="text-emerald-400" />
                                <span>자동 갱신</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div className="absolute bottom-0 left-0 w-full bg-gradient-to-t from-slate-100 via-slate-100/95 to-transparent pt-16 pb-8 px-6 z-20">
                <button onClick={handleStartNow} className="w-full bg-gradient-to-r from-amber-400 to-orange-500 hover:from-amber-500 hover:to-orange-600 text-white rounded-full py-4 text-[13px] font-black tracking-widest shadow-xl shadow-orange-500/30 flex items-center justify-center gap-2 mb-3 active:scale-95 transition-transform">
                    <Crown size={16} strokeWidth={3} /> 지금 시작하기
                </button>
                <button 
                    onClick={closeUpgradePlan}
                    className="w-full bg-white/60 backdrop-blur-md text-slate-600 rounded-full py-4 text-[13px] font-black tracking-widest border border-white active:scale-95 transition-transform shadow-sm hover:bg-white"
                >
                    나중에 하기
                </button>
            </div>
        </div>
    );
}
