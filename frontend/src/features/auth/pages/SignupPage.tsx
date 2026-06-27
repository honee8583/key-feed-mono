import { useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Check, Loader2 } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import gsap from 'gsap';
import { useGSAP } from '@gsap/react';
import { cn } from '@/utils/cn';
import { useJoin } from '../hooks/useJoin';
import { useAuthStore } from '@/stores/authStore';

// Brand wordmark shown top-left. (Design mock uses "tracer" — swap to your brand.)
const BRAND = 'tracer';

const signupSchema = z.object({
    name: z.string().min(2, '이름은 2자 이상이어야 합니다.'),
    email: z.string().min(1, '이메일을 입력해주세요.').email('올바른 이메일 형식이 아닙니다.'),
    password: z.string().min(8, '비밀번호는 8자 이상이어야 합니다.'),
    agree: z.literal(true, { message: '약관에 동의해주세요.' }),
});

type SignupFormValues = z.infer<typeof signupSchema>;

// Exact field styling from the Claude Design auth screens.
const inputClass =
    'w-full h-[50px] border border-hairline rounded-[4px] px-3.5 text-[16px] text-ink ' +
    'placeholder:text-muted outline-none transition-colors focus:border-form-focus';

export function SignupPage() {
    const navigate = useNavigate();
    const setPendingEmail = useAuthStore((state) => state.setPendingEmail);
    const { mutate: join, isPending, error } = useJoin();

    const screenRef = useRef<HTMLDivElement>(null);

    const {
        register,
        handleSubmit,
        watch,
        formState: { errors },
    } = useForm<SignupFormValues>({
        resolver: zodResolver(signupSchema),
        defaultValues: { name: '', email: '', password: '', agree: false as unknown as true },
    });

    const agreed = watch('agree');

    // Matches the design's `scrFade` screen entrance.
    useGSAP(() => {
        gsap.fromTo(
            screenRef.current,
            { opacity: 0, y: 8 },
            { opacity: 1, y: 0, duration: 0.3, ease: 'power2.out' }
        );
    }, []);

    const onSubmit = (data: SignupFormValues) => {
        join(
            { name: data.name, email: data.email, password: data.password },
            {
                onSuccess: () => {
                    setPendingEmail(data.email);
                    navigate('/auth/verify');
                },
            }
        );
    };

    const apiError = error as { response?: { status?: number; data?: { message?: string } } };
    const isConflict = apiError?.response?.status === 409;
    const errorMessage = apiError?.response?.data?.message || error?.message;

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

            {/* Centered signup card */}
            <div className="flex flex-1 items-center justify-center px-5 pb-20 pt-4">
                <div className="w-full max-w-[400px]">
                    <div className="mb-3 font-mono text-[12px] uppercase tracking-mono-label text-coral">
                        SIGN UP · 1 / 2
                    </div>
                    <h1 className="mb-3 font-display text-[32px] font-medium leading-[1.05] tracking-tightest text-primary sm:text-[34px]">
                        계정 만들기
                    </h1>
                    <p className="mb-8 text-[16px] text-[#616161]">
                        이메일로 인증코드를 보내 본인 확인을 해요.
                    </p>

                    <form onSubmit={handleSubmit(onSubmit)} noValidate>
                        <label htmlFor="name" className="mb-[7px] block text-[13px] text-[#616161]">
                            이름
                        </label>
                        <input
                            id="name"
                            type="text"
                            autoComplete="name"
                            placeholder="김도현"
                            {...register('name')}
                            className={cn(inputClass, 'mb-[18px]', errors.name && 'border-brand-error focus:border-brand-error')}
                        />
                        {errors.name && (
                            <p className="-mt-3 mb-3 text-[13px] text-brand-error">{errors.name.message}</p>
                        )}

                        <label htmlFor="email" className="mb-[7px] block text-[13px] text-[#616161]">
                            이메일
                        </label>
                        <input
                            id="email"
                            type="email"
                            autoComplete="email"
                            placeholder="you@example.com"
                            {...register('email')}
                            className={cn(inputClass, 'mb-[18px]', errors.email && 'border-brand-error focus:border-brand-error')}
                        />
                        {errors.email && (
                            <p className="-mt-3 mb-3 text-[13px] text-brand-error">{errors.email.message}</p>
                        )}

                        <label htmlFor="password" className="mb-[7px] block text-[13px] text-[#616161]">
                            비밀번호
                        </label>
                        <input
                            id="password"
                            type="password"
                            autoComplete="new-password"
                            placeholder="8자 이상"
                            {...register('password')}
                            className={cn(inputClass, 'mb-[22px]', errors.password && 'border-brand-error focus:border-brand-error')}
                        />
                        {errors.password && (
                            <p className="-mt-4 mb-3 text-[13px] text-brand-error">{errors.password.message}</p>
                        )}

                        {/* Required terms agreement */}
                        <label htmlFor="agree" className="mb-2 flex cursor-pointer items-start gap-2.5">
                            <input id="agree" type="checkbox" className="sr-only" {...register('agree')} />
                            <span
                                aria-hidden
                                className={cn(
                                    'mt-px flex h-5 w-5 flex-shrink-0 items-center justify-center rounded-[5px] border transition-colors',
                                    agreed ? 'border-deep-green bg-deep-green' : 'border-hairline bg-canvas'
                                )}
                            >
                                {agreed && <Check size={11} strokeWidth={3} className="text-canvas" />}
                            </span>
                            <span className="text-[13px] leading-[1.45] text-[#616161]">
                                (필수) 서비스 이용약관 및 개인정보처리방침에 동의합니다.
                            </span>
                        </label>
                        {errors.agree && (
                            <p className="mb-2 text-[13px] text-brand-error">{errors.agree.message}</p>
                        )}

                        {error && (
                            <div className="mb-[18px] mt-2 rounded-[4px] border border-brand-error/20 bg-brand-error/5 px-3.5 py-3 text-[13px] text-brand-error">
                                {isConflict
                                    ? '이미 존재하는 사용자입니다. 다른 이메일을 사용하거나 로그인해주세요.'
                                    : errorMessage || '회원가입에 실패했습니다.'}
                            </div>
                        )}

                        <button
                            type="submit"
                            disabled={isPending}
                            className="mt-6 flex h-[52px] w-full items-center justify-center rounded-[32px] bg-primary text-[16px] font-medium text-canvas transition-colors hover:bg-cohere-black disabled:cursor-not-allowed disabled:opacity-70"
                        >
                            {isPending ? <Loader2 className="animate-spin" size={20} /> : '인증코드 받기'}
                        </button>
                    </form>

                    <div className="mt-[30px] text-center text-[14px] text-[#75758a]">
                        이미 계정이 있으신가요?{' '}
                        <button
                            type="button"
                            onClick={() => navigate('/auth/login')}
                            className="font-medium text-action-blue"
                        >
                            로그인
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}
