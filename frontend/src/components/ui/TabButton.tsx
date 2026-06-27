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
                'flex flex-1 flex-col items-center gap-1 py-1 transition-colors',
                active ? 'text-primary' : 'text-muted'
            )}
        >
            <span className="transition-transform active:scale-110">{icon}</span>
            <span className="font-mono text-[10px] tracking-[0.3px]">{label}</span>
        </button>
    );
}
