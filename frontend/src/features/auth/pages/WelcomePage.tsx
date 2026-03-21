import { useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import gsap from 'gsap';
import { useGSAP } from '@gsap/react';
import { PartyPopper, ChevronRight } from 'lucide-react';

export function WelcomePage() {
    const navigate = useNavigate();
    const containerRef = useRef<HTMLDivElement>(null);

    useGSAP(() => {
        gsap.fromTo(containerRef.current,
            { opacity: 0, scale: 0.95 },
            { opacity: 1, scale: 1, duration: 0.4, ease: "power3.out" }
        );
    }, []);

    return (
        <div
            ref={containerRef}
            className="flex-1 flex flex-col items-center justify-center px-8 text-center opacity-0"
        >
            <div className="w-24 h-24 bg-white/60 backdrop-blur-md rounded-[40px] border border-white/60 shadow-xl flex items-center justify-center text-indigo-500 mb-8">
                <PartyPopper size={48} />
            </div>
            <h2 className="text-3xl font-black text-slate-900 leading-tight mb-4 uppercase tracking-tighter">Welcome!</h2>
            <p className="text-sm text-slate-500 font-medium leading-relaxed mb-10">
                회원가입이 성공적으로 완료되었습니다.<br />이제 당신만의 기술 인사이트를 쌓아보세요.
            </p>
            <button
                onClick={() => navigate('/auth/login')}
                className="w-full bg-slate-900 text-white py-4 rounded-2xl font-black text-sm uppercase shadow-xl flex items-center justify-center gap-2 active:scale-95 transition-transform"
            >
                시작하기 <ChevronRight size={18} />
            </button>
        </div>
    );
}
