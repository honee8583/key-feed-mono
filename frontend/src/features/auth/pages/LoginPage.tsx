import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Mail, Lock, Loader2 } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { AuthWrapper } from '@/components/ui/AuthWrapper';
import { useAuthStore } from '@/stores/authStore';
import { useLogin } from '../hooks/useLogin';
import type { LoginRequest } from '../types/auth.types';

const loginSchema = z.object({
    email: z.string().min(1, '이메일을 입력해주세요.').email('올바른 이메일 형식이 아닙니다.'),
    password: z.string().min(1, '비밀번호를 입력해주세요.'),
});

type LoginFormValues = z.infer<typeof loginSchema>;

export function LoginPage() {
    const navigate = useNavigate();
    const setPendingEmail = useAuthStore((state) => state.setPendingEmail);
    const { mutateAsync: loginMutation, isPending } = useLogin();
    const [loginError, setLoginError] = useState<string | null>(null);

    const {
        register,
        handleSubmit,
        formState: { errors },
    } = useForm<LoginFormValues>({
        resolver: zodResolver(loginSchema),
        defaultValues: {
            email: '',
            password: '',
        },
    });

    const onSubmit = async (data: LoginFormValues) => {
        setLoginError(null);
        try {
            await loginMutation(data as LoginRequest);
            navigate('/home');
        } catch (error) {
            console.error('Login failed:', error);
            // In a real app, parse axios error response message
            const apiError = error as { response?: { status?: number, data?: { message?: string } } };
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
        <AuthWrapper title="Login" subtitle="반갑습니다! 로그인 후 서비스를 이용해주세요.">
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
                <div>
                    <div className="relative group">
                        <Mail className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
                        <input
                            type="email"
                            placeholder="이메일 주소"
                            {...register('email')}
                            className={`w-full bg-white/40 border ${errors.email ? 'border-red-400' : 'border-white/60'} rounded-2xl py-4 pl-12 pr-4 text-sm font-bold focus:ring-4 focus:ring-slate-100 outline-none transition-all placeholder:font-medium`}
                        />
                    </div>
                    {errors.email && <p className="text-red-500 text-xs mt-1.5 ml-2 font-bold">{errors.email.message}</p>}
                </div>

                <div>
                    <div className="relative group">
                        <Lock className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
                        <input
                            type="password"
                            placeholder="비밀번호"
                            {...register('password')}
                            className={`w-full bg-white/40 border ${errors.password ? 'border-red-400' : 'border-white/60'} rounded-2xl py-4 pl-12 pr-4 text-sm font-bold focus:ring-4 focus:ring-slate-100 outline-none transition-all placeholder:font-medium`}
                        />
                    </div>
                    {errors.password && <p className="text-red-500 text-xs mt-1.5 ml-2 font-bold">{errors.password.message}</p>}
                </div>

                {loginError && (
                    <div className="bg-red-50 text-red-500 text-xs font-bold p-3 rounded-xl border border-red-100 text-center">
                        {loginError}
                    </div>
                )}

                <button
                    type="submit"
                    disabled={isPending}
                    className="w-full bg-slate-900 text-white py-4 rounded-2xl font-black text-sm uppercase shadow-xl mt-4 active:scale-95 transition-transform flex items-center justify-center disabled:opacity-70 disabled:active:scale-100"
                >
                    {isPending ? <Loader2 className="animate-spin" size={20} /> : '로그인'}
                </button>

                <div className="text-center pt-4">
                    <button
                        type="button"
                        onClick={() => navigate('/auth/signup')}
                        className="text-xs font-bold text-slate-400 hover:text-slate-600 transition-colors uppercase tracking-widest"
                    >
                        아직 회원이 아니신가요? 가입하기
                    </button>
                </div>
            </form>
        </AuthWrapper>
    );
}
