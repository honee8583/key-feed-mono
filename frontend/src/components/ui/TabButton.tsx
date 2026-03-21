import type { ReactNode } from 'react';
import { cn } from '@/utils/cn';

interface TabButtonProps {
    active: boolean;
    icon: ReactNode;
    label: string;
    onClick: () => void;
}

export function TabButton({ active, icon, label, onClick }: TabButtonProps) {
    return (
        <button
            onClick={onClick}
            className={cn(
                "flex flex-col items-center gap-1 transition-all relative",
                active ? "text-slate-900" : "text-slate-400"
            )}
        >
            <div
                className="relative z-10 active:-translate-y-1 active:scale-[1.15] transition-transform"
            >
                {icon}
            </div>

            <span
                className={cn(
                    "text-[7px] font-black uppercase tracking-widest",
                    active ? "text-slate-900" : "text-slate-400"
                )}
            >
                {label}
            </span>

            {active && (
                <div
                    className="absolute -top-1 -left-2 -right-2 -bottom-1 bg-white/70 blur-xl rounded-full -z-10 animate-in fade-in duration-300"
                />
            )}
        </button>
    );
}
