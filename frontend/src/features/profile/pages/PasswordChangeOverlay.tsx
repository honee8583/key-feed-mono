import { useRef, useEffect, useState } from 'react';
import { ArrowLeft, Loader2, Check } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import gsap from 'gsap';
import { useGSAP } from '@gsap/react';
import { cn } from '@/utils/cn';
import { useUiStore } from '@/stores/uiStore';
import { useChangePassword } from '../api/userApi';

// tracer 디자인 시스템(로그인/회원가입 화면)과 동일한 입력 필드 스타일
const inputClass =
    'w-full h-[50px] border border-hairline rounded-[4px] px-3.5 text-[16px] text-ink ' +
    'placeholder:text-muted outline-none transition-colors focus:border-form-focus';

const schema = z
    .object({
        currentPassword: z.string().min(1, '현재 비밀번호를 입력해주세요.'),
        newPassword: z
            .string()
            .min(8, '비밀번호는 8자 이상이어야 합니다.')
            .max(20, '비밀번호는 20자 이하여야 합니다.'),
        confirmPassword: z.string().min(1, '새 비밀번호를 한 번 더 입력해주세요.'),
    })
    .refine((v) => v.newPassword === v.confirmPassword, {
        path: ['confirmPassword'],
        message: '새 비밀번호가 일치하지 않습니다.',
    });

type FormValues = z.infer<typeof schema>;

export function PasswordChangeOverlay() {
    const { isPasswordChangeOpen, closePasswordChange, unmountPasswordChange } = useUiStore();
    const overlayRef = useRef<HTMLDivElement>(null);
    const { contextSafe } = useGSAP({ scope: overlayRef });

    const { mutateAsync: changePassword, isPending } = useChangePassword();
    const [apiError, setApiError] = useState<string | null>(null);
    const [done, setDone] = useState(false);

    const {
        register,
        handleSubmit,
        formState: { errors },
    } = useForm<FormValues>({
        resolver: zodResolver(schema),
        defaultValues: { currentPassword: '', newPassword: '', confirmPassword: '' },
    });

    useEffect(() => {
        contextSafe(() => {
            if (isPasswordChangeOpen) {
                gsap.to(overlayRef.current, { x: 0, opacity: 1, duration: 0.4, ease: 'power3.out' });
            } else {
                gsap.to(overlayRef.current, {
                    x: '100%',
                    opacity: 0,
                    duration: 0.3,
                    ease: 'power2.in',
                    onComplete: unmountPasswordChange,
                });
            }
        })();
    }, [isPasswordChangeOpen, unmountPasswordChange, contextSafe]);

    const onSubmit = async (data: FormValues) => {
        setApiError(null);
        try {
            await changePassword(data);
            setDone(true);
            setTimeout(closePasswordChange, 1100);
        } catch (error) {
            const apiErr = error as { response?: { data?: { message?: string } } };
            setApiError(apiErr?.response?.data?.message || '비밀번호 변경에 실패했습니다. 다시 시도해주세요.');
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
                        onClick={closePasswordChange}
                        className="rounded-full border border-[#f2f2f2] bg-white p-2 text-ink shadow-sm transition-transform active:scale-90"
                        aria-label="뒤로"
                    >
                        <ArrowLeft size={20} />
                    </button>
                    <h2 className="font-display text-[20px] font-medium tracking-tightest text-primary">비밀번호 변경</h2>
                </div>

                <div className="flex-1 px-5 pb-20 pt-4">
                    {done ? (
                        <div className="flex flex-col items-center py-24 text-center">
                            <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-deep-green text-white">
                                <Check size={26} strokeWidth={3} />
                            </div>
                            <p className="text-[15px] font-medium text-ink">비밀번호가 변경되었습니다.</p>
                        </div>
                    ) : (
                        <form onSubmit={handleSubmit(onSubmit)} noValidate>
                            <label className="mb-[7px] block text-[13px] text-[#616161]">현재 비밀번호</label>
                            <input
                                type="password"
                                autoComplete="current-password"
                                placeholder="현재 비밀번호"
                                {...register('currentPassword')}
                                className={cn(inputClass, 'mb-2', errors.currentPassword && 'border-brand-error focus:border-brand-error')}
                            />
                            {errors.currentPassword && (
                                <p className="mb-3 text-[13px] text-brand-error">{errors.currentPassword.message}</p>
                            )}

                            <label className="mb-[7px] mt-4 block text-[13px] text-[#616161]">새 비밀번호</label>
                            <input
                                type="password"
                                autoComplete="new-password"
                                placeholder="8~20자"
                                {...register('newPassword')}
                                className={cn(inputClass, 'mb-2', errors.newPassword && 'border-brand-error focus:border-brand-error')}
                            />
                            {errors.newPassword && (
                                <p className="mb-3 text-[13px] text-brand-error">{errors.newPassword.message}</p>
                            )}

                            <label className="mb-[7px] mt-4 block text-[13px] text-[#616161]">새 비밀번호 확인</label>
                            <input
                                type="password"
                                autoComplete="new-password"
                                placeholder="새 비밀번호를 다시 입력"
                                {...register('confirmPassword')}
                                className={cn(inputClass, 'mb-2', errors.confirmPassword && 'border-brand-error focus:border-brand-error')}
                            />
                            {errors.confirmPassword && (
                                <p className="mb-3 text-[13px] text-brand-error">{errors.confirmPassword.message}</p>
                            )}

                            {apiError && (
                                <div className="mb-4 mt-2 rounded-[4px] border border-brand-error/20 bg-brand-error/5 px-3.5 py-3 text-[13px] text-brand-error">
                                    {apiError}
                                </div>
                            )}

                            <button
                                type="submit"
                                disabled={isPending}
                                className="mt-6 flex h-[52px] w-full items-center justify-center rounded-[32px] bg-primary text-[16px] font-medium text-canvas transition-colors hover:bg-cohere-black disabled:cursor-not-allowed disabled:opacity-70"
                            >
                                {isPending ? <Loader2 className="animate-spin" size={20} /> : '변경하기'}
                            </button>
                        </form>
                    )}
                </div>
            </div>
        </div>
    );
}
