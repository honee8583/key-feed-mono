import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft, Info, AlertCircle } from 'lucide-react';
import gsap from 'gsap';
import { useGSAP } from '@gsap/react';
import { cn } from '@/utils/cn';
import { useAuthStore } from '@/stores/authStore';
import { useConfirmVerification } from '../hooks/useConfirmVerification';
import { useRequestVerification } from '../hooks/useRequestVerification';

export function VerifyPage() {
    const navigate = useNavigate();
    const pendingEmail = useAuthStore((state) => state.pendingEmail);
    const [code, setCode] = useState(['', '', '', '', '', '']);
    const inputRefs = useRef<(HTMLInputElement | null)[]>([]);
    const screenRef = useRef<HTMLDivElement>(null);

    const { mutate: confirm, isPending: isConfirming, error: confirmError } = useConfirmVerification();
    const { mutate: requestResend, isPending: isResending, isSuccess: isResent } = useRequestVerification();

    useEffect(() => {
        if (!pendingEmail) {
            navigate('/auth/signup', { replace: true });
        }
    }, [pendingEmail, navigate]);

    // Matches the design's `scrFade` screen entrance.
    useGSAP(() => {
        gsap.fromTo(
            screenRef.current,
            { opacity: 0, y: 8 },
            { opacity: 1, y: 0, duration: 0.3, ease: 'power2.out' }
        );
    }, []);

    const handleInput = (index: number, value: string) => {
        if (!/^[0-9A-Za-z]?$/.test(value)) return;

        const newCode = [...code];
        newCode[index] = value.toUpperCase();
        setCode(newCode);

        if (value && index < 5) {
            inputRefs.current[index + 1]?.focus();
        }
    };

    const handleKeyDown = (index: number, e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === 'Backspace' && !code[index] && index > 0) {
            inputRefs.current[index - 1]?.focus();
        }
    };

    const handleConfirm = () => {
        const fullCode = code.join('');
        if (fullCode.length < 6 || !pendingEmail) return;

        confirm(
            { email: pendingEmail, code: fullCode },
            {
                onSuccess: () => {
                    navigate('/auth/welcome');
                },
            }
        );
    };

    const handleResend = () => {
        if (!pendingEmail) return;
        setCode(['', '', '', '', '', '']);
        inputRefs.current[0]?.focus();
        requestResend({ email: pendingEmail });
    };

    const isCodeComplete = code.every((c) => c !== '');
    const apiError = confirmError as { response?: { data?: { message?: string } } };
    const errorMessage = apiError?.response?.data?.message || confirmError?.message;

    return (
        <div ref={screenRef} className="flex min-h-screen flex-col bg-canvas font-pretendard text-ink">
            <div className="flex flex-1 flex-col px-5 pb-12 pt-14 sm:px-10">
                <div className="mx-auto flex w-full max-w-[400px] flex-1 flex-col">
                    {/* Back to the signup form */}
                    <button
                        type="button"
                        onClick={() => navigate('/auth/signup')}
                        className="mb-9 inline-flex items-center gap-1.5 self-start text-[14px] text-[#75758a] transition-colors hover:text-primary"
                    >
                        <ChevronLeft size={16} strokeWidth={1.8} />
                        이전
                    </button>

                    <div className="mb-3 font-mono text-[12px] uppercase tracking-mono-label text-coral">
                        SIGN UP · 2 / 2
                    </div>
                    <h1 className="mb-3.5 font-display text-[32px] font-medium leading-[1.05] tracking-tightest text-primary sm:text-[34px]">
                        이메일을 확인해주세요
                    </h1>
                    <p className="mb-9 text-[16px] leading-[1.5] text-[#616161]">
                        <span className="font-semibold text-ink">{pendingEmail}</span>으로
                        <br />
                        6자리 인증코드를 보냈어요.
                    </p>

                    {/* 6-digit code boxes */}
                    <div className="mb-[18px] flex gap-[9px]">
                        {[0, 1, 2, 3, 4, 5].map((index) => (
                            <input
                                key={index}
                                ref={(el) => { inputRefs.current[index] = el; }}
                                type="text"
                                inputMode="text"
                                maxLength={1}
                                value={code[index]}
                                onChange={(e) => handleInput(index, e.target.value)}
                                onKeyDown={(e) => handleKeyDown(index, e)}
                                className={cn(
                                    'h-[60px] min-w-0 flex-1 rounded-[10px] border-[1.5px] bg-canvas text-center font-display text-[24px] font-semibold uppercase text-primary outline-none transition-colors focus:border-form-focus',
                                    code[index] ? 'border-ink' : 'border-hairline'
                                )}
                            />
                        ))}
                    </div>

                    {confirmError && (
                        <div className="mb-[18px] flex items-center gap-[7px]">
                            <AlertCircle size={15} className="flex-shrink-0 text-brand-error" />
                            <span className="text-[13px] text-brand-error">
                                {errorMessage || '인증코드가 올바르지 않아요. 다시 확인해주세요.'}
                            </span>
                        </div>
                    )}

                    <div className="mb-[30px] text-[14px] text-[#75758a]">
                        코드를 받지 못하셨나요?{' '}
                        <button
                            type="button"
                            onClick={handleResend}
                            disabled={isResending}
                            className={cn(
                                'font-medium disabled:opacity-60',
                                isResent && !confirmError ? 'text-deep-green' : 'text-action-blue'
                            )}
                        >
                            {isResending ? '발송 중...' : isResent && !confirmError ? '재전송 완료' : '코드 다시 보내기'}
                        </button>
                    </div>

                    <button
                        type="button"
                        onClick={handleConfirm}
                        disabled={!isCodeComplete || isConfirming}
                        className="flex h-[52px] w-full items-center justify-center rounded-[32px] bg-primary text-[16px] font-medium text-canvas transition-colors hover:bg-cohere-black disabled:cursor-not-allowed disabled:opacity-70"
                    >
                        {isConfirming ? '확인 중...' : '인증하고 가입 완료'}
                    </button>

                    <div className="mt-[22px] flex items-start gap-2 px-0.5">
                        <Info size={14} className="mt-0.5 flex-shrink-0 text-muted" strokeWidth={1.8} />
                        <p className="text-[12px] leading-[1.6] text-muted">
                            인증코드는 보통 1~2분 내에 도착해요. 메일이 보이지 않으면 스팸함도 확인해주세요.
                        </p>
                    </div>
                </div>
            </div>
        </div>
    );
}
