import { useState, useRef } from 'react';
import gsap from 'gsap';
import { useGSAP } from '@gsap/react';
import { Check } from 'lucide-react';
import { ICON_MAP, AVAILABLE_COLORS, AVAILABLE_ICONS, type IconName, type ColorName } from '@/utils/constants';

interface FolderActionModalProps {
    type: 'create' | 'edit';
    initialName?: string;
    initialIcon?: IconName;
    initialColor?: ColorName;
    onConfirm: (name: string, icon: IconName, color: ColorName) => void;
    onClose: () => void;
}

export function FolderActionModal({
    type,
    initialName = '',
    initialIcon = 'Folder',
    initialColor = 'blue',
    onConfirm,
    onClose
}: FolderActionModalProps) {

    const [name, setName] = useState(initialName);
    const [icon, setIcon] = useState<IconName>(initialIcon);
    const [color, setColor] = useState<ColorName>(initialColor);
    const modalRef = useRef<HTMLDivElement>(null);
    const backdropRef = useRef<HTMLDivElement>(null);

    useGSAP(() => {
        gsap.fromTo(backdropRef.current, { opacity: 0 }, { opacity: 1, duration: 0.2 });
        gsap.fromTo(modalRef.current, { scale: 0.9, y: 20 }, { scale: 1, y: 0, duration: 0.3, ease: 'power2.out' });
    }, []);

    const activeColorConfig = AVAILABLE_COLORS.find(c => c.name === color) || AVAILABLE_COLORS[0];
    const ActiveIcon = ICON_MAP[icon] || ICON_MAP.Folder;

    return (
        <div
            ref={backdropRef}
            className="fixed inset-0 z-[110] bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-6"
            onClick={onClose}
        >
            <div
                ref={modalRef}
                className="w-full max-w-[340px] bg-white rounded-[40px] p-8 shadow-2xl flex flex-col"
                onClick={(e) => e.stopPropagation()}
            >
                <h4 className="text-xl font-black text-slate-900 mb-2 uppercase tracking-tighter text-center">
                    {type === 'edit' ? '수정' : '새 폴더'}
                </h4>
                <p className="text-[10px] text-slate-400 font-bold text-center mb-6 uppercase tracking-widest">
                    Setup your folder identity
                </p>

                <div className="flex justify-center mb-8">
                    <div className={`w-16 h-16 ${activeColorConfig.light} rounded-3xl flex items-center justify-center ${activeColorConfig.text} border-2 ${activeColorConfig.border} shadow-lg shadow-${activeColorConfig.name}-100`}>
                        <ActiveIcon size={28} />
                    </div>
                </div>

                <div className="space-y-6 overflow-y-auto max-h-[400px] no-scrollbar">
                    <div>
                        <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1 mb-2 block">Name</label>
                        <input
                            autoFocus
                            type="text"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            placeholder="폴더 이름을 입력하세요"
                            className="w-full bg-slate-50 border border-slate-100 rounded-2xl py-3 px-4 text-sm font-bold focus:ring-4 focus:ring-slate-100 outline-none transition-all"
                        />
                    </div>

                    <div>
                        <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1 mb-2 block">Icon</label>
                        <div className="grid grid-cols-5 gap-2">
                            {AVAILABLE_ICONS.map(iconName => {
                                const IconComp = ICON_MAP[iconName];
                                return (
                                    <button
                                        key={iconName}
                                        onClick={() => setIcon(iconName)}
                                        className={`aspect-square flex items-center justify-center rounded-xl border transition-all ${icon === iconName ? 'bg-slate-900 text-white border-slate-900 shadow-md' : 'bg-slate-50 text-slate-400 border-slate-100 hover:bg-white'}`}
                                    >
                                        <IconComp size={16} />
                                    </button>
                                )
                            })}
                        </div>
                    </div>

                    <div>
                        <label className="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1 mb-2 block">Color Theme</label>
                        <div className="grid grid-cols-4 gap-3">
                            {AVAILABLE_COLORS.map(c => (
                                <button
                                    key={c.name}
                                    onClick={() => setColor(c.name as ColorName)}
                                    className={`h-10 rounded-xl ${c.bg} border-2 flex items-center justify-center transition-all ${color === c.name ? 'border-slate-900 scale-105 shadow-md shadow-slate-100' : 'border-transparent'}`}
                                >
                                    {color === c.name && <Check size={16} className="text-white" />}
                                </button>
                            ))}
                        </div>
                    </div>
                </div>

                <div className="flex gap-2 mt-8">
                    <button
                        onClick={onClose}
                        className="flex-1 py-4 bg-slate-50 text-slate-400 rounded-[20px] text-[11px] font-black uppercase tracking-widest hover:bg-slate-100 active:scale-95 transition-transform"
                    >
                        취소
                    </button>
                    <button
                        onClick={() => onConfirm(name, icon, color)}
                        className={`flex-1 py-4 ${activeColorConfig.bg} text-white rounded-[20px] text-[11px] font-black uppercase tracking-widest shadow-xl shadow-${activeColorConfig.name}-100 active:scale-95 transition-transform`}
                    >
                        확인
                    </button>
                </div>
            </div>
        </div>
    );
}
