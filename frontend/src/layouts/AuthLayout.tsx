import { useLocation, useOutlet } from 'react-router-dom';
import { useRef } from 'react';
import gsap from 'gsap';
import { useGSAP } from '@gsap/react';

export function AuthLayout() {
    const outlet = useOutlet();
    const location = useLocation();

    const blob1Ref = useRef<HTMLDivElement>(null);
    const blob2Ref = useRef<HTMLDivElement>(null);
    const contentRef = useRef<HTMLDivElement>(null);

    useGSAP(() => {
        // Infinite background blob 1
        gsap.to(blob1Ref.current, {
            scale: 1.2,
            x: 50,
            y: 30,
            duration: 7.5,
            repeat: -1,
            yoyo: true,
            ease: "sine.inOut"
        });

        // Infinite background blob 2
        gsap.to(blob2Ref.current, {
            scale: 1.3,
            x: -60,
            y: -40,
            duration: 9,
            repeat: -1,
            yoyo: true,
            ease: "sine.inOut"
        });
    }, []);

    useGSAP(() => {
        // Page entry transition
        gsap.fromTo(contentRef.current,
            { opacity: 0, x: 20 },
            { opacity: 1, x: 0, duration: 0.3, ease: 'power2.out' }
        );
    }, [location.pathname]);

    return (
        <div className="min-h-screen bg-slate-200 flex justify-center md:items-center font-sans selection:bg-slate-300 p-0 md:p-6">
            <div className="fixed inset-0 overflow-hidden pointer-events-none">
                <div
                    ref={blob1Ref}
                    className="absolute top-[-15%] left-[-15%] w-[70%] h-[70%] bg-blue-200/20 blur-[120px] rounded-full"
                />
                <div
                    ref={blob2Ref}
                    className="absolute bottom-[-15%] right-[-15%] w-[70%] h-[70%] bg-slate-100/30 blur-[120px] rounded-full"
                />
            </div>

            <div className="w-full max-w-[480px] bg-white/30 backdrop-blur-[60px] min-h-screen md:min-h-[660px] md:h-auto md:rounded-[3rem] shadow-[0_20px_50px_rgba(0,0,0,0.1)] flex flex-col relative overflow-hidden border-x md:border border-white/40">
                <div ref={contentRef} className="flex-1 flex flex-col">
                    {outlet}
                </div>
            </div>
        </div>
    );
}
