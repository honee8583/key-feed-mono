import { Folder, Star, Heart, Briefcase, Clock, Zap, Smile, Code, Cpu, Book } from 'lucide-react';

export const ICON_MAP = {
    Folder,
    Star,
    Heart,
    Briefcase,
    Clock,
    Zap,
    Smile,
    Code,
    Cpu,
    Book
} as const;

export type IconName = keyof typeof ICON_MAP;

export const AVAILABLE_ICONS = Object.keys(ICON_MAP) as IconName[];

export const AVAILABLE_COLORS = [
    { name: 'blue', bg: 'bg-blue-500', text: 'text-blue-500', light: 'bg-blue-50', border: 'border-blue-100' },
    { name: 'purple', bg: 'bg-purple-500', text: 'text-purple-500', light: 'bg-purple-50', border: 'border-purple-100' },
    { name: 'rose', bg: 'bg-rose-500', text: 'text-rose-500', light: 'bg-rose-50', border: 'border-rose-100' },
    { name: 'amber', bg: 'bg-amber-500', text: 'text-amber-500', light: 'bg-amber-50', border: 'border-amber-100' },
    { name: 'emerald', bg: 'bg-emerald-500', text: 'text-emerald-500', light: 'bg-emerald-50', border: 'border-emerald-100' },
    { name: 'indigo', bg: 'bg-indigo-500', text: 'text-indigo-500', light: 'bg-indigo-50', border: 'border-indigo-100' },
    { name: 'orange', bg: 'bg-orange-500', text: 'text-orange-500', light: 'bg-orange-50', border: 'border-orange-100' },
    { name: 'cyan', bg: 'bg-cyan-500', text: 'text-cyan-500', light: 'bg-cyan-50', border: 'border-cyan-100' },
] as const;

export type ColorName = typeof AVAILABLE_COLORS[number]['name'];
