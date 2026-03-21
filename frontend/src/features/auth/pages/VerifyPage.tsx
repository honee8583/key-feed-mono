import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthWrapper } from '@/components/ui/AuthWrapper';
import { useAuthStore } from '@/stores/authStore';
import { useConfirmVerification } from '../hooks/useConfirmVerification';
import { useRequestVerification } from '../hooks/useRequestVerification';

export function VerifyPage() {
    const navigate = useNavigate();
    const pendingEmail = useAuthStore((state) => state.pendingEmail);
    const [code, setCode] = useState(['', '', '', '', '', '']);
    const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

    const { mutate: confirm, isPending: isConfirming, error: confirmError } = useConfirmVerification();
    const { mutate: requestResend, isPending: isResending, isSuccess: isResent } = useRequestVerification();

    useEffect(() => {
        if (!pendingEmail) {
            navigate('/auth/signup', { replace: true });
        }
    }, [pendingEmail, navigate]);

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
        <AuthWrapper title="Verify Email" subtitle={pendingEmail ? `${pendingEmail}로 발송된\n6자리 인증 코드를 입력해주세요.` : "인증 코드를 입력해주세요."}>
            <div className="space-y-6">
                <div className="flex justify-between gap-2">
                    {[0, 1, 2, 3, 4, 5].map((index) => (
                        <input
                            key={index}
                            ref={(el) => { inputRefs.current[index] = el; }}
                            type="text"
                            maxLength={1}
                            value={code[index]}
                            onChange={(e) => handleInput(index, e.target.value)}
                            onKeyDown={(e) => handleKeyDown(index, e)}
                            className="w-12 h-14 bg-white/40 border border-white/60 rounded-xl text-center text-xl font-black text-slate-800 focus:ring-4 focus:ring-slate-100 outline-none transition-all uppercase"
                        />
                    ))}
                </div>

                {confirmError && (
                    <div className="text-center px-2">
                        <p className="text-xs text-rose-500 font-bold">{errorMessage || '유효하지 않은 인증 코드입니다.'}</p>
                    </div>
                )}
                {isResent && !confirmError && (
                    <div className="text-center px-2">
                        <p className="text-xs text-indigo-500 font-bold">인증 코드가 재전송되었습니다.</p>
                    </div>
                )}

                <button
                    onClick={handleConfirm}
                    disabled={!isCodeComplete || isConfirming}
                    className="w-full bg-slate-900 text-white py-4 rounded-2xl font-black text-sm uppercase shadow-xl active:scale-95 transition-transform disabled:opacity-50 disabled:active:scale-100"
                >
                    {isConfirming ? '확인 중...' : '인증 완료'}
                </button>
                <div className="text-center">
                    <button
                        onClick={handleResend}
                        disabled={isResending}
                        className="text-xs font-bold text-indigo-500 uppercase tracking-widest disabled:opacity-50"
                    >
                        {isResending ? '발송 중...' : '코드 다시 보내기'}
                    </button>
                </div>
            </div>
        </AuthWrapper>
    );
}
