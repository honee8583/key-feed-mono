import { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import gsap from 'gsap';
import { useGSAP } from '@gsap/react';
import { cn } from '@/utils/cn';
import { useAuthStore } from '@/stores/authStore';
import { useLogin } from '../hooks/useLogin';
import type { LoginRequest } from '../types/auth.types';

// Brand wordmark shown top-left. (Design mock uses "tracer" — swap to your brand.)
const BRAND = 'tracer';

const loginSchema = z.object({
    email: z.string().min(1, '이메일을 입력해주세요.').email('올바른 이메일 형식이 아닙니다.'),
    password: z.string().min(1, '비밀번호를 입력해주세요.'),
});

type LoginFormValues = z.infer<typeof loginSchema>;

// Exact field styling from the Claude Design login screen.
const inputClass =
    'w-full h-[50px] border border-hairline rounded-[4px] px-3.5 text-[16px] text-ink ' +
    'placeholder:text-muted outline-none transition-colors focus:border-form-focus';

export function LoginPage() {
    const navigate = useNavigate();
    const setPendingEmail = useAuthStore((state) => state.setPendingEmail);
    const { mutateAsync: loginMutation, isPending } = useLogin();
    const [loginError, setLoginError] = useState<string | null>(null);

    const screenRef = useRef<HTMLDivElement>(null);

    const {
        register,
        handleSubmit,
        formState: { errors },
    } = useForm<LoginFormValues>({
        resolver: zodResolver(loginSchema),
        defaultValues: { email: '', password: '' },
    });

    // Matches the design's `scrFade` screen entrance.
    useGSAP(() => {
        gsap.fromTo(
            screenRef.current,
            { opacity: 0, y: 8 },
            { opacity: 1, y: 0, duration: 0.3, ease: 'power2.out' }
        );
    }, []);

    const onSubmit = async (data: LoginFormValues) => {
        setLoginError(null);
        try {
            await loginMutation(data as LoginRequest);
            navigate('/home');
        } catch (error) {
            console.error('Login failed:', error);
            const apiError = error as { response?: { status?: number; data?: { message?: string } } };
            const errorMessage = apiError?.response?.data?.message;

            if (apiError?.response?.status === 400 && errorMessage === 'EMAIL_VERIFICATION_REQUIRED') {
                setPendingEmail(data.email);
                navigate('/auth/verify');
                return;
            }

            setLoginError(errorMessage || '로그인에 실패했습니다. 이메일과 비밀번호를 확인해주세요.');
        }
    };

    return (
        <div ref={screenRef} className="flex min-h-screen flex-col bg-canvas font-pretendard text-ink">
            {/* Logo header */}
            <div className="flex items-center gap-2 px-5 py-6 sm:px-10">
                <button
                    type="button"
                    onClick={() => navigate('/')}
                    className="flex items-center gap-2"
                    aria-label={BRAND}
                >
                    <span className="h-4 w-4 rounded-[5px] bg-deep-green" />
                    <span className="font-display text-[21px] font-medium tracking-[-0.4px] text-primary">
                        {BRAND}
                    </span>
                </button>
            </div>

            {/* Centered login card */}
            <div className="flex flex-1 items-center justify-center px-5 pb-20 pt-4">
                <div className="w-full max-w-[400px]">
                    <div className="mb-3.5 font-mono text-[12px] uppercase tracking-mono-label text-coral">
                        LOGIN
                    </div>
                    <h1 className="mb-3 font-display text-[32px] font-medium leading-[1.04] tracking-tightest text-primary sm:text-[40px]">
                        다시 오셨어요
                    </h1>
                    <p className="mb-[34px] text-[16px] text-[#616161]">
                        계정에 로그인하고 피드를 이어보세요.
                    </p>

                    <form onSubmit={handleSubmit(onSubmit)} noValidate>
                        <label htmlFor="email" className="mb-[7px] block text-[13px] text-[#616161]">
                            이메일
                        </label>
                        <input
                            id="email"
                            type="email"
                            autoComplete="email"
                            placeholder="you@example.com"
                            {...register('email')}
                            className={cn(inputClass, 'mb-5', errors.email && 'border-brand-error focus:border-brand-error')}
                        />
                        {errors.email && (
                            <p className="-mt-3.5 mb-3 text-[13px] text-brand-error">{errors.email.message}</p>
                        )}

                        <div className="mb-[7px] flex items-baseline justify-between">
                            <label htmlFor="password" className="text-[13px] text-[#616161]">
                                비밀번호
                            </label>
                            <button
                                type="button"
                                className="text-[13px] text-action-blue"
                                onClick={() => navigate('/auth/forgot-password')}
                            >
                                비밀번호를 잊으셨나요?
                            </button>
                        </div>
                        <input
                            id="password"
                            type="password"
                            autoComplete="current-password"
                            placeholder="비밀번호"
                            {...register('password')}
                            className={cn(inputClass, 'mb-7', errors.password && 'border-brand-error focus:border-brand-error')}
                        />
                        {errors.password && (
                            <p className="-mt-5 mb-3 text-[13px] text-brand-error">{errors.password.message}</p>
                        )}

                        {loginError && (
                            <div className="mb-[18px] rounded-[4px] border border-brand-error/20 bg-brand-error/5 px-3.5 py-3 text-[13px] text-brand-error">
                                {loginError}
                            </div>
                        )}

                        <button
                            type="submit"
                            disabled={isPending}
                            className="mb-[18px] flex h-[52px] w-full items-center justify-center rounded-[32px] bg-primary text-[16px] font-medium text-canvas transition-colors hover:bg-cohere-black disabled:cursor-not-allowed disabled:opacity-70"
                        >
                            {isPending ? <Loader2 className="animate-spin" size={20} /> : '로그인'}
                        </button>
                    </form>

                    <div className="mb-[18px] flex items-center gap-3">
                        <div className="h-px flex-1 bg-[#e5e7eb]" />
                        <span className="text-[13px] text-muted">또는</span>
                        <div className="h-px flex-1 bg-[#e5e7eb]" />
                    </div>

                    <button
                        type="button"
                        onClick={() => { /* TODO: Google OAuth 연결 */ }}
                        className="flex h-[52px] w-full items-center justify-center gap-[9px] rounded-[32px] border border-hairline bg-canvas text-[15px] font-medium text-primary transition-colors hover:bg-[#f7f7f5]"
                    >
                        <span className="inline-flex h-[18px] w-[18px] items-center justify-center rounded-full bg-soft-stone font-display text-[12px] font-bold text-primary">
                            G
                        </span>
                        Google로 계속하기
                    </button>

                    <div className="mt-[30px] text-center text-[14px] text-[#75758a]">
                        아직 계정이 없으신가요?{' '}
                        <button
                            type="button"
                            onClick={() => navigate('/auth/signup')}
                            className="font-medium text-action-blue"
                        >
                            회원가입
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}
