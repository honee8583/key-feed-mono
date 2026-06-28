import { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft, AlertCircle, Check, Loader2 } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import gsap from 'gsap';
import { useGSAP } from '@gsap/react';
import { cn } from '@/utils/cn';
import { useRequestPasswordReset } from '../hooks/useRequestPasswordReset';
import { useVerifyPasswordResetCode } from '../hooks/useVerifyPasswordResetCode';
import { useConfirmPasswordReset } from '../hooks/useConfirmPasswordReset';

type Step = 'email' | 'verify' | 'reset' | 'done';

// Field styling shared with the login/verify screens (Claude Design tracer).
const inputClass =
    'w-full h-[50px] border border-hairline rounded-[4px] px-3.5 text-[16px] text-ink ' +
    'placeholder:text-muted outline-none transition-colors focus:border-form-focus';

const emailSchema = z.string().email('올바른 이메일 형식이 아닙니다.');

const passwordSchema = z
    .object({
        newPassword: z
            .string()
            .min(8, '비밀번호는 8자 이상 20자 이하로 입력해주세요.')
            .max(20, '비밀번호는 8자 이상 20자 이하로 입력해주세요.'),
        confirmPassword: z.string().min(1, '비밀번호 확인을 입력해주세요.'),
    })
    .refine((data) => data.newPassword === data.confirmPassword, {
        message: '비밀번호가 일치하지 않습니다.',
        path: ['confirmPassword'],
    });

type PasswordFormValues = z.infer<typeof passwordSchema>;

function getErrorMessage(error: unknown, fallback: string): string {
    const apiError = error as { response?: { data?: { message?: string } } };
    return apiError?.response?.data?.message || fallback;
}

export function ForgotPasswordPage() {
    const navigate = useNavigate();
    const screenRef = useRef<HTMLDivElement>(null);

    const [step, setStep] = useState<Step>('email');
    const [email, setEmail] = useState('');
    const [emailError, setEmailError] = useState<string | null>(null);

    const [code, setCode] = useState(['', '', '', '', '', '']);
    const [codeError, setCodeError] = useState<string | null>(null);
    const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

    const requestReset = useRequestPasswordReset();
    const verifyCode = useVerifyPasswordResetCode();
    const confirmReset = useConfirmPasswordReset();

    const {
        register,
        handleSubmit,
        setError: setFormError,
        formState: { errors },
    } = useForm<PasswordFormValues>({
        resolver: zodResolver(passwordSchema),
        defaultValues: { newPassword: '', confirmPassword: '' },
    });

    // Re-run the design's `scrFade` entrance on every step change.
    useGSAP(() => {
        gsap.fromTo(
            screenRef.current,
            { opacity: 0, y: 8 },
            { opacity: 1, y: 0, duration: 0.3, ease: 'power2.out' }
        );
    }, [step]);

    const goLogin = () => navigate('/auth/login');

    // ── STEP 1: request code ──────────────────────────────────────────────
    const handleSendCode = () => {
        setEmailError(null);
        const parsed = emailSchema.safeParse(email);
        if (!parsed.success) {
            setEmailError(parsed.error.issues[0].message);
            return;
        }
        requestReset.mutate(
            { email },
            {
                onSuccess: () => {
                    setCode(['', '', '', '', '', '']);
                    setStep('verify');
                },
                onError: (error) => {
                    setEmailError(getErrorMessage(error, '가입된 이메일을 찾을 수 없어요.'));
                },
            }
        );
    };

    // ── STEP 2: verify code ───────────────────────────────────────────────
    const handleCodeInput = (index: number, value: string) => {
        if (!/^[0-9]?$/.test(value)) return;
        const next = [...code];
        next[index] = value;
        setCode(next);
        setCodeError(null);
        if (value && index < 5) inputRefs.current[index + 1]?.focus();
    };

    const handleCodeKeyDown = (index: number, e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === 'Backspace' && !code[index] && index > 0) {
            inputRefs.current[index - 1]?.focus();
        }
    };

    const handleVerify = () => {
        const fullCode = code.join('');
        if (fullCode.length < 6) return;
        setCodeError(null);
        verifyCode.mutate(
            { email, code: fullCode },
            {
                onSuccess: (response) => {
                    if (response.data.status === 'VERIFIED') {
                        setStep('reset');
                    } else {
                        setCodeError('인증코드가 올바르지 않아요. 다시 확인해주세요.');
                    }
                },
                onError: (error) => {
                    setCodeError(getErrorMessage(error, '인증코드가 올바르지 않아요. 다시 확인해주세요.'));
                },
            }
        );
    };

    const handleResendCode = () => {
        setCode(['', '', '', '', '', '']);
        setCodeError(null);
        inputRefs.current[0]?.focus();
        requestReset.mutate({ email });
    };

    // ── STEP 3: set new password ──────────────────────────────────────────
    const onSubmitNewPassword = (data: PasswordFormValues) => {
        confirmReset.mutate(
            { email, newPassword: data.newPassword, confirmPassword: data.confirmPassword },
            {
                onSuccess: () => setStep('done'),
                onError: (error) => {
                    setFormError('newPassword', {
                        message: getErrorMessage(error, '비밀번호 변경에 실패했어요. 다시 시도해주세요.'),
                    });
                },
            }
        );
    };

    const isCodeComplete = code.every((c) => c !== '');
    const isResent = requestReset.isSuccess && step === 'verify';

    return (
        <div ref={screenRef} className="flex min-h-screen flex-col bg-canvas font-pretendard text-ink">
            <div className="flex flex-1 flex-col px-5 pb-12 pt-14 sm:px-10">
                <div className="mx-auto flex w-full max-w-[400px] flex-1 flex-col">
                    {/* ── STEP 1: EMAIL ── */}
                    {step === 'email' && (
                        <>
                            <button
                                type="button"
                                onClick={goLogin}
                                className="mb-9 inline-flex items-center gap-1.5 self-start text-[14px] text-[#75758a] transition-colors hover:text-primary"
                            >
                                <ChevronLeft size={16} strokeWidth={1.8} />
                                로그인
                            </button>

                            <div className="mb-3 font-mono text-[12px] uppercase tracking-mono-label text-coral">
                                RESET · 1 / 3
                            </div>
                            <h1 className="mb-3.5 font-display text-[32px] font-medium leading-[1.05] tracking-tightest text-primary sm:text-[34px]">
                                비밀번호 재설정
                            </h1>
                            <p className="mb-9 text-[16px] leading-[1.5] text-[#616161]">
                                가입하신 이메일을 입력하면
                                <br />
                                인증코드를 보내드려요.
                            </p>

                            <label htmlFor="reset-email" className="mb-[7px] block text-[13px] text-[#616161]">
                                이메일
                            </label>
                            <input
                                id="reset-email"
                                type="email"
                                autoComplete="email"
                                placeholder="you@example.com"
                                value={email}
                                onChange={(e) => {
                                    setEmail(e.target.value);
                                    setEmailError(null);
                                }}
                                onKeyDown={(e) => e.key === 'Enter' && handleSendCode()}
                                className={cn(inputClass, 'mb-2', emailError && 'border-brand-error focus:border-brand-error')}
                            />
                            {emailError && (
                                <p className="mb-3 flex items-center gap-[7px] text-[13px] text-brand-error">
                                    <AlertCircle size={15} className="flex-shrink-0" />
                                    {emailError}
                                </p>
                            )}

                            <button
                                type="button"
                                onClick={handleSendCode}
                                disabled={requestReset.isPending}
                                className="mt-6 flex h-[52px] w-full items-center justify-center rounded-[32px] bg-primary text-[16px] font-medium text-canvas transition-colors hover:bg-cohere-black disabled:cursor-not-allowed disabled:opacity-70"
                            >
                                {requestReset.isPending ? <Loader2 className="animate-spin" size={20} /> : '인증코드 받기'}
                            </button>

                            <div className="mt-auto pt-8 text-center text-[14px] text-[#75758a]">
                                비밀번호가 기억나셨나요?{' '}
                                <button type="button" onClick={goLogin} className="font-medium text-action-blue">
                                    로그인
                                </button>
                            </div>
                        </>
                    )}

                    {/* ── STEP 2: VERIFY ── */}
                    {step === 'verify' && (
                        <>
                            <button
                                type="button"
                                onClick={() => setStep('email')}
                                className="mb-9 inline-flex items-center gap-1.5 self-start text-[14px] text-[#75758a] transition-colors hover:text-primary"
                            >
                                <ChevronLeft size={16} strokeWidth={1.8} />
                                이전
                            </button>

                            <div className="mb-3 font-mono text-[12px] uppercase tracking-mono-label text-coral">
                                RESET · 2 / 3
                            </div>
                            <h1 className="mb-3.5 font-display text-[32px] font-medium leading-[1.05] tracking-tightest text-primary sm:text-[34px]">
                                이메일을 확인해주세요
                            </h1>
                            <p className="mb-9 text-[16px] leading-[1.5] text-[#616161]">
                                <span className="font-semibold text-ink">{email}</span>으로
                                <br />
                                6자리 인증코드를 보냈어요.
                            </p>

                            <div className="mb-[18px] flex gap-[9px]">
                                {[0, 1, 2, 3, 4, 5].map((index) => (
                                    <input
                                        key={index}
                                        ref={(el) => { inputRefs.current[index] = el; }}
                                        type="text"
                                        inputMode="numeric"
                                        maxLength={1}
                                        value={code[index]}
                                        onChange={(e) => handleCodeInput(index, e.target.value)}
                                        onKeyDown={(e) => handleCodeKeyDown(index, e)}
                                        className={cn(
                                            'h-[60px] min-w-0 flex-1 rounded-[10px] border-[1.5px] bg-canvas text-center font-display text-[24px] font-semibold text-primary outline-none transition-colors focus:border-form-focus',
                                            codeError ? 'border-brand-error' : code[index] ? 'border-ink' : 'border-hairline'
                                        )}
                                    />
                                ))}
                            </div>

                            {codeError && (
                                <div className="mb-[18px] flex items-center gap-[7px]">
                                    <AlertCircle size={15} className="flex-shrink-0 text-brand-error" />
                                    <span className="text-[13px] text-brand-error">{codeError}</span>
                                </div>
                            )}

                            <div className="mb-[30px] text-[14px] text-[#75758a]">
                                코드를 받지 못하셨나요?{' '}
                                <button
                                    type="button"
                                    onClick={handleResendCode}
                                    disabled={requestReset.isPending}
                                    className={cn(
                                        'font-medium disabled:opacity-60',
                                        isResent && !codeError ? 'text-deep-green' : 'text-action-blue'
                                    )}
                                >
                                    {requestReset.isPending ? '발송 중...' : isResent && !codeError ? '재전송 완료' : '코드 다시 보내기'}
                                </button>
                            </div>

                            <button
                                type="button"
                                onClick={handleVerify}
                                disabled={!isCodeComplete || verifyCode.isPending}
                                className="flex h-[52px] w-full items-center justify-center rounded-[32px] bg-primary text-[16px] font-medium text-canvas transition-colors hover:bg-cohere-black disabled:cursor-not-allowed disabled:opacity-70"
                            >
                                {verifyCode.isPending ? <Loader2 className="animate-spin" size={20} /> : '다음'}
                            </button>
                        </>
                    )}

                    {/* ── STEP 3: NEW PASSWORD ── */}
                    {step === 'reset' && (
                        <>
                            <button
                                type="button"
                                onClick={() => setStep('verify')}
                                className="mb-9 inline-flex items-center gap-1.5 self-start text-[14px] text-[#75758a] transition-colors hover:text-primary"
                            >
                                <ChevronLeft size={16} strokeWidth={1.8} />
                                이전
                            </button>

                            <div className="mb-3 font-mono text-[12px] uppercase tracking-mono-label text-coral">
                                RESET · 3 / 3
                            </div>
                            <h1 className="mb-3.5 font-display text-[32px] font-medium leading-[1.05] tracking-tightest text-primary sm:text-[34px]">
                                새 비밀번호 설정
                            </h1>
                            <p className="mb-9 text-[16px] leading-[1.5] text-[#616161]">
                                새로 사용할 비밀번호를 입력해주세요.
                            </p>

                            <form onSubmit={handleSubmit(onSubmitNewPassword)} noValidate>
                                <label htmlFor="new-password" className="mb-[7px] block text-[13px] text-[#616161]">
                                    새 비밀번호
                                </label>
                                <input
                                    id="new-password"
                                    type="password"
                                    autoComplete="new-password"
                                    placeholder="8자 이상"
                                    {...register('newPassword')}
                                    className={cn(inputClass, 'mb-4', errors.newPassword && 'border-brand-error focus:border-brand-error')}
                                />

                                <label htmlFor="confirm-password" className="mb-[7px] block text-[13px] text-[#616161]">
                                    새 비밀번호 확인
                                </label>
                                <input
                                    id="confirm-password"
                                    type="password"
                                    autoComplete="new-password"
                                    placeholder="비밀번호 재입력"
                                    {...register('confirmPassword')}
                                    className={cn(inputClass, 'mb-2', errors.confirmPassword && 'border-brand-error focus:border-brand-error')}
                                />

                                {(errors.confirmPassword || errors.newPassword) && (
                                    <p className="mb-2 flex items-center gap-[7px] text-[13px] text-brand-error">
                                        <AlertCircle size={15} className="flex-shrink-0" />
                                        {errors.confirmPassword?.message || errors.newPassword?.message}
                                    </p>
                                )}

                                <button
                                    type="submit"
                                    disabled={confirmReset.isPending}
                                    className="mt-6 flex h-[52px] w-full items-center justify-center rounded-[32px] bg-primary text-[16px] font-medium text-canvas transition-colors hover:bg-cohere-black disabled:cursor-not-allowed disabled:opacity-70"
                                >
                                    {confirmReset.isPending ? <Loader2 className="animate-spin" size={20} /> : '비밀번호 변경'}
                                </button>
                            </form>
                        </>
                    )}

                    {/* ── STEP 4: DONE ── */}
                    {step === 'done' && (
                        <>
                            <div className="flex flex-1 flex-col items-center justify-center text-center">
                                <div className="mb-7 flex h-[72px] w-[72px] items-center justify-center rounded-full bg-[#edfce9]">
                                    <Check size={34} className="text-deep-green" strokeWidth={2} />
                                </div>
                                <h1 className="mb-3.5 font-display text-[30px] font-medium leading-[1.1] tracking-tightest text-primary">
                                    비밀번호가 변경됐어요
                                </h1>
                                <p className="max-w-[280px] text-[16px] leading-[1.55] text-[#616161]">
                                    새 비밀번호로 다시 로그인해주세요.
                                </p>
                            </div>
                            <button
                                type="button"
                                onClick={goLogin}
                                className="flex h-[52px] w-full items-center justify-center rounded-[32px] bg-primary text-[16px] font-medium text-canvas transition-colors hover:bg-cohere-black"
                            >
                                로그인하러 가기
                            </button>
                        </>
                    )}
                </div>
            </div>
        </div>
    );
}
