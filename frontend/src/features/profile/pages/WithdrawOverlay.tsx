import { useRef, useEffect, useState } from 'react';
import { ArrowLeft, Loader2, AlertTriangle } from 'lucide-react';
import gsap from 'gsap';
import { useGSAP } from '@gsap/react';
import { cn } from '@/utils/cn';
import { useUiStore } from '@/stores/uiStore';
import { useAuthStore } from '@/stores/authStore';
import { useWithdrawUser } from '../api/userApi';

const inputClass =
    'w-full h-[50px] border border-hairline rounded-[4px] px-3.5 text-[16px] text-ink ' +
    'placeholder:text-muted outline-none transition-colors focus:border-form-focus';

export function WithdrawOverlay() {
    const { isWithdrawOpen, closeWithdraw, unmountWithdraw } = useUiStore();
    const logout = useAuthStore((state) => state.logout);
    const overlayRef = useRef<HTMLDivElement>(null);
    const { contextSafe } = useGSAP({ scope: overlayRef });

    const { mutateAsync: withdraw, isPending } = useWithdrawUser();
    const [password, setPassword] = useState('');
    const [confirmed, setConfirmed] = useState(false);
    const [apiError, setApiError] = useState<string | null>(null);

    useEffect(() => {
        contextSafe(() => {
            if (isWithdrawOpen) {
                gsap.to(overlayRef.current, { x: 0, opacity: 1, duration: 0.4, ease: 'power3.out' });
            } else {
                gsap.to(overlayRef.current, {
                    x: '100%',
                    opacity: 0,
                    duration: 0.3,
                    ease: 'power2.in',
                    onComplete: unmountWithdraw,
                });
            }
        })();
    }, [isWithdrawOpen, unmountWithdraw, contextSafe]);

    const handleWithdraw = async () => {
        if (!password || !confirmed) return;
        setApiError(null);
        try {
            await withdraw(password);
            // 탈퇴 성공 → 인증 정보 정리 후 로그인 화면으로 (authStore.logout이 리다이렉트 처리)
            logout();
        } catch (error) {
            const apiErr = error as { response?: { data?: { message?: string } } };
            setApiError(apiErr?.response?.data?.message || '회원 탈퇴에 실패했습니다. 비밀번호를 확인해주세요.');
        }
    };

    return (
        <div
            ref={overlayRef}
            className="absolute inset-0 z-[100] flex justify-center overflow-y-auto bg-[#fafafa] font-pretendard translate-x-full opacity-0"
        >
            <div className="flex min-h-full w-full max-w-[480px] flex-col">
                {/* Header */}
                <div className="sticky top-0 z-10 flex items-center gap-3 bg-[#fafafa]/85 px-5 pb-5 pt-10 backdrop-blur-xl">
                    <button
                        onClick={closeWithdraw}
                        className="rounded-full border border-[#f2f2f2] bg-white p-2 text-ink shadow-sm transition-transform active:scale-90"
                        aria-label="뒤로"
                    >
                        <ArrowLeft size={20} />
                    </button>
                    <h2 className="font-display text-[20px] font-medium tracking-tightest text-primary">회원 탈퇴</h2>
                </div>

                <div className="flex-1 px-5 pb-20 pt-4">
                    {/* Warning */}
                    <div className="mb-7 flex gap-3 rounded-2xl border border-brand-error/15 bg-brand-error/5 p-4">
                        <AlertTriangle size={20} className="mt-0.5 shrink-0 text-brand-error" />
                        <div className="text-[13px] leading-relaxed text-[#616161]">
                            탈퇴 시 저장한 글, 팔로잉한 블로그, 구독 정보 등 모든 데이터가 삭제되며
                            <span className="font-semibold text-brand-error"> 복구할 수 없습니다.</span>
                        </div>
                    </div>

                    <label className="mb-[7px] block text-[13px] text-[#616161]">비밀번호 확인</label>
                    <input
                        type="password"
                        autoComplete="current-password"
                        placeholder="현재 비밀번호를 입력해주세요"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        className={cn(inputClass, 'mb-5')}
                    />

                    <label className="mb-7 flex cursor-pointer items-start gap-3">
                        <input
                            type="checkbox"
                            checked={confirmed}
                            onChange={(e) => setConfirmed(e.target.checked)}
                            className="mt-0.5 h-[18px] w-[18px] shrink-0 accent-brand-error"
                        />
                        <span className="text-[13px] leading-relaxed text-[#616161]">
                            위 내용을 모두 확인했으며, 계정을 영구적으로 삭제하는 것에 동의합니다.
                        </span>
                    </label>

                    {apiError && (
                        <div className="mb-4 rounded-[4px] border border-brand-error/20 bg-brand-error/5 px-3.5 py-3 text-[13px] text-brand-error">
                            {apiError}
                        </div>
                    )}

                    <button
                        onClick={handleWithdraw}
                        disabled={isPending || !password || !confirmed}
                        className="flex h-[52px] w-full items-center justify-center rounded-[32px] bg-brand-error text-[16px] font-medium text-white transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-40"
                    >
                        {isPending ? <Loader2 className="animate-spin" size={20} /> : '회원 탈퇴하기'}
                    </button>
                    <button
                        onClick={closeWithdraw}
                        className="mt-3 h-[52px] w-full rounded-[32px] border border-hairline bg-white text-[15px] font-medium text-primary transition-colors hover:bg-[#f7f7f5]"
                    >
                        취소
                    </button>
                </div>
            </div>
        </div>
    );
}
