import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { Loader2, CheckCircle2, XCircle } from 'lucide-react';
import { registerPaymentMethod } from '../api/paymentApi';
import { startSubscription } from '../api/subscriptionApi';

export function PaymentCallback() {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const [status, setStatus] = useState<'pending' | 'success' | 'error'>('pending');
    const [errorMessage, setErrorMessage] = useState<string | undefined>();

    const authKey = searchParams.get('authKey');
    const isFail = searchParams.get('fail');
    const code = searchParams.get('code');
    const message = searchParams.get('message');
    const action = searchParams.get('action'); // "subscribe" → 카드 등록 후 구독 시작

    useEffect(() => {
        async function process() {
            if (isFail === 'true' || code) {
                setErrorMessage(message ?? undefined);
                setStatus('error');
                return;
            }
            if (!authKey) {
                setStatus('error');
                return;
            }
            try {
                const method = await registerPaymentMethod(authKey);

                if (action === 'subscribe') {
                    await startSubscription(method.methodId);
                }

                setStatus('success');
            } catch (err: any) {
                setErrorMessage(err?.response?.data?.message);
                setStatus('error');
            }
        }
        process();
    }, [authKey, isFail, code, action]);

    const successMessage = action === 'subscribe'
        ? '구독이 시작되었습니다!'
        : '결제 수단이 등록되었습니다.';

    return (
        <div className="min-h-screen bg-slate-50 flex items-center justify-center p-6">
            <div className="bg-white max-w-sm w-full p-8 rounded-[32px] shadow-[0_20px_50px_rgba(0,0,0,0.05)] border border-slate-100 text-center flex flex-col items-center">
                {status === 'pending' && (
                    <>
                        <Loader2 size={48} className="animate-spin text-indigo-500 mb-6" />
                        <h2 className="text-lg font-black text-slate-800 mb-2">
                            {action === 'subscribe' ? '구독 처리 중...' : '카드 등록 중...'}
                        </h2>
                        <p className="text-xs text-slate-500 font-medium">안전하게 처리하고 있습니다.</p>
                    </>
                )}
                {status === 'success' && (
                    <>
                        <div className="w-16 h-16 bg-emerald-100 text-emerald-500 rounded-full flex items-center justify-center mb-6">
                            <CheckCircle2 size={32} strokeWidth={2.5} />
                        </div>
                        <h2 className="text-lg font-black text-slate-800 mb-2">완료!</h2>
                        <p className="text-xs text-slate-500 font-medium mb-8">{successMessage}</p>
                        <button
                            onClick={() => navigate('/profile')}
                            className="w-full bg-slate-900 text-white rounded-2xl py-3.5 text-[13px] font-bold active:scale-95 transition-transform"
                        >
                            마이페이지로 돌아가기
                        </button>
                    </>
                )}
                {status === 'error' && (
                    <>
                        <div className="w-16 h-16 bg-rose-100 text-rose-500 rounded-full flex items-center justify-center mb-6">
                            <XCircle size={32} strokeWidth={2.5} />
                        </div>
                        <h2 className="text-lg font-black text-slate-800 mb-2">실패했습니다</h2>
                        <p className="text-xs text-slate-500 font-medium mb-2">
                            {errorMessage || '처리 중 문제가 발생했습니다.'}
                        </p>
                        <button
                            onClick={() => navigate('/profile')}
                            className="w-full mt-6 bg-slate-100 text-slate-700 rounded-2xl py-3.5 text-[13px] font-bold active:scale-95 transition-transform"
                        >
                            돌아가기
                        </button>
                    </>
                )}
            </div>
        </div>
    );
}
