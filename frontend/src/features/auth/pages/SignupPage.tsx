import { useNavigate } from 'react-router-dom';
import { Mail, Lock, User as UserIcon } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { AuthWrapper } from '@/components/ui/AuthWrapper';
import { useJoin } from '../hooks/useJoin';
import { useAuthStore } from '@/stores/authStore';

const signupSchema = z.object({
    email: z.string().email('유효한 이메일 주소를 입력해주세요'),
    password: z.string().min(8, '비밀번호는 8자 이상이어야 합니다'),
    name: z.string().min(2, '이름은 2자 이상이어야 합니다'),
});

type SignupFormValues = z.infer<typeof signupSchema>;

export function SignupPage() {
    const navigate = useNavigate();
    const setPendingEmail = useAuthStore((state) => state.setPendingEmail);
    const { mutate: join, isPending, error } = useJoin();

    const {
        register,
        handleSubmit,
        formState: { errors },
    } = useForm<SignupFormValues>({
        resolver: zodResolver(signupSchema),
    });

    const onSubmit = (data: SignupFormValues) => {
        join(data, {
            onSuccess: () => {
                setPendingEmail(data.email);
                navigate('/auth/verify');
            },
        });
    };

    const apiError = error as { response?: { status?: number, data?: { message?: string } } };
    const isConflict = apiError?.response?.status === 409;
    const errorMessage = apiError?.response?.data?.message || error?.message;

    return (
        <AuthWrapper
            title="Create Account"
            subtitle="TechStack의 새로운 소식을 가장 먼저 확인하세요."
        >
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
                <div className="space-y-1">
                    <div className="relative group">
                        <Mail className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
                        <input
                            {...register('email')}
                            type="email"
                            placeholder="이메일 주소"
                            className="w-full bg-white/40 border border-white/60 rounded-2xl py-4 pl-12 pr-4 text-sm font-bold focus:ring-4 focus:ring-slate-100 outline-none transition-all"
                        />
                    </div>
                    {errors.email && <p className="text-xs text-rose-500 font-bold px-2">{errors.email.message}</p>}
                </div>

                <div className="space-y-1">
                    <div className="relative group">
                        <UserIcon className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
                        <input
                            {...register('name')}
                            type="text"
                            placeholder="이름 (닉네임)"
                            className="w-full bg-white/40 border border-white/60 rounded-2xl py-4 pl-12 pr-4 text-sm font-bold focus:ring-4 focus:ring-slate-100 outline-none transition-all"
                        />
                    </div>
                    {errors.name && <p className="text-xs text-rose-500 font-bold px-2">{errors.name.message}</p>}
                </div>

                <div className="space-y-1">
                    <div className="relative group">
                        <Lock className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
                        <input
                            {...register('password')}
                            type="password"
                            placeholder="비밀번호"
                            className="w-full bg-white/40 border border-white/60 rounded-2xl py-4 pl-12 pr-4 text-sm font-bold focus:ring-4 focus:ring-slate-100 outline-none transition-all"
                        />
                    </div>
                    {errors.password && <p className="text-xs text-rose-500 font-bold px-2">{errors.password.message}</p>}
                </div>

                {error && (
                    <div className="px-2">
                        <p className="text-xs text-rose-500 font-bold">
                            {isConflict ? '이미 존재하는 사용자입니다. 다른 이메일을 사용하거나 로그인해주세요.' : errorMessage || '회원가입에 실패했습니다.'}
                        </p>
                    </div>
                )}

                <button
                    type="submit"
                    disabled={isPending}
                    className="w-full bg-slate-900 text-white py-4 rounded-2xl font-black text-sm uppercase shadow-xl mt-4 active:scale-95 transition-transform disabled:opacity-50 disabled:active:scale-100"
                >
                    {isPending ? '처리 중...' : '인증 코드 전송하기'}
                </button>

                <div className="text-center pt-4">
                    <button
                        type="button"
                        onClick={() => navigate('/auth/login')}
                        className="text-xs font-bold text-slate-400 hover:text-slate-600 transition-colors uppercase tracking-widest"
                    >
                        이미 계정이 있으신가요? 로그인
                    </button>
                </div>
            </form>
        </AuthWrapper>
    );
}
