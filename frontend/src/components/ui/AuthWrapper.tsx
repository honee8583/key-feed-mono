import type { ReactNode } from 'react';
import { useRef } from 'react';
import gsap from 'gsap';
import { useGSAP } from '@gsap/react';
import { Terminal } from 'lucide-react';

interface AuthWrapperProps {
    children: ReactNode;
    title: string;
    subtitle: string;
}

export function AuthWrapper({ children, title, subtitle }: AuthWrapperProps) {
    const containerRef = useRef<HTMLDivElement>(null);

    useGSAP(() => {
        gsap.fromTo(
            containerRef.current,
            { opacity: 0, x: 20 },
            { opacity: 1, x: 0, duration: 0.4, ease: 'power2.out' }
        );
    }, { scope: containerRef, dependencies: [] });

    return (
        <div
            ref={containerRef}
            className="flex-1 flex flex-col px-8 pt-20"
        >
            <div className="mb-10">
                <div className="w-12 h-12 bg-white/60 backdrop-blur-md rounded-2xl flex items-center justify-center text-slate-800 shadow-sm border border-white/50 mb-6">
                    <Terminal size={24} strokeWidth={2.5} />
                </div>
                <h2 className="text-2xl font-black text-slate-900 leading-tight mb-2 uppercase tracking-tighter">
                    {title}
                </h2>
                <p className="text-sm text-slate-500 font-medium leading-relaxed">
                    {subtitle}
                </p>
            </div>
            {children}
        </div>
    );
}
