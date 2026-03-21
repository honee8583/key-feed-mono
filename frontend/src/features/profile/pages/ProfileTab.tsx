import { useNavigate } from 'react-router-dom';
import { Mail, Bell, ChevronRight, LogOut, UserX } from 'lucide-react';

import { useAuthStore } from '@/stores/authStore';

export function ProfileTab() {
    const navigate = useNavigate();
    const logout = useAuthStore((state) => state.logout);

    const handleLogout = () => {
        logout();
        navigate('/auth/login');
    };

    return (
        <div className="px-5 pt-2 pb-24">
            <div className="bg-white/40 backdrop-blur-xl rounded-[28px] border border-white/60 p-5 shadow-sm mb-6 flex flex-col items-center text-center">
                <div className="w-16 h-16 bg-gradient-to-tr from-slate-200 to-slate-100 rounded-2xl border-4 border-white shadow-md flex items-center justify-center mb-3 overflow-hidden">
                    <img
                        src="https://api.dicebear.com/7.x/avataaars/svg?seed=Felix"
                        alt="avatar"
                        className="w-full h-full object-cover"
                    />
                </div>
                <h4 className="text-base font-black text-slate-800 mb-0.5">홍길동</h4>
                <p className="text-[11px] text-slate-500 font-medium mb-3 flex items-center gap-1 opacity-70">
                    <Mail size={10} /> developer.hong@example.com
                </p>
                <div className="flex gap-1.5">
                    <div className="px-3 py-1 bg-white/60 rounded-full border border-white/80 text-[8px] font-black text-slate-600 uppercase">
                        Frontend
                    </div>
                    <div className="px-3 py-1 bg-white/60 rounded-full border border-white/80 text-[8px] font-black text-slate-600 uppercase">
                        React
                    </div>
                </div>
            </div>

            <div className="space-y-2">
                <h5 className="text-[9px] font-black text-slate-400 uppercase tracking-widest px-2 mb-1">
                    Preferences
                </h5>

                <button
                    className="w-full bg-white/40 border border-white/60 rounded-xl p-3.5 flex items-center justify-between active:scale-95 transition-transform"
                >
                    <div className="flex items-center gap-3">
                        <div className="w-8 h-8 bg-white/50 rounded-lg flex items-center justify-center text-indigo-500 shadow-sm border border-white/50">
                            <Bell size={16} />
                        </div>
                        <span className="text-xs font-bold text-slate-700">알림 설정</span>
                    </div>
                    <ChevronRight size={14} className="text-slate-300" />
                </button>

                <h5 className="text-[9px] font-black text-slate-400 uppercase tracking-widest px-2 mt-4 mb-1">
                    Danger Zone
                </h5>

                <button
                    onClick={handleLogout}
                    className="w-full bg-white/40 border border-white/60 rounded-xl p-3.5 flex items-center gap-3 active:scale-95 transition-transform"
                >
                    <div className="w-8 h-8 bg-white/50 rounded-lg flex items-center justify-center text-slate-500 shadow-sm border border-white/50">
                        <LogOut size={16} />
                    </div>
                    <span className="text-xs font-bold text-slate-700">로그아웃</span>
                </button>

                <button
                    onClick={() => navigate('/auth/signup')}
                    className="w-full bg-rose-50/20 border border-rose-100/30 rounded-xl p-3.5 flex items-center gap-3 active:scale-95 transition-transform"
                >
                    <div className="w-8 h-8 bg-rose-50/50 rounded-lg flex items-center justify-center text-rose-500 shadow-sm border border-rose-100/30">
                        <UserX size={16} />
                    </div>
                    <span className="text-xs font-bold text-rose-600">회원탈퇴</span>
                </button>
            </div>
        </div>
    );
}
