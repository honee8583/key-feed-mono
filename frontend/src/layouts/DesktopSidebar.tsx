import { useLocation, useNavigate } from 'react-router-dom';
import { Terminal, Search, Bell, Home, Compass, Bookmark, User } from 'lucide-react';
import { cn } from '@/utils/cn';
import { useUiStore } from '@/stores/uiStore';
import { useNotificationStore } from '@/stores/notificationStore';

interface NavItemProps {
    icon: React.ReactNode;
    label: string;
    isActive?: boolean;
    onClick: () => void;
    badgeCount?: number;
}

function NavItem({ icon, label, isActive, onClick, badgeCount }: NavItemProps) {
    return (
        <button
            onClick={onClick}
            className={cn(
                "w-full flex items-center gap-4 px-4 py-3 rounded-2xl transition-all duration-200 group relative",
                isActive
                    ? "bg-white shadow-sm text-slate-900 font-bold"
                    : "text-slate-500 hover:bg-white/50 hover:text-slate-800 font-medium"
            )}
        >
            <div className={cn(
                "transition-transform duration-200",
                isActive ? "scale-110" : "group-hover:scale-110"
            )}>
                {icon}
            </div>
            <span className="text-[15px]">{label}</span>
            {badgeCount !== undefined && badgeCount > 0 && (
                <span className="absolute right-4 w-5 h-5 bg-rose-500 rounded-full flex items-center justify-center text-[10px] text-white font-bold">
                    {badgeCount > 99 ? '99+' : badgeCount}
                </span>
            )}
        </button>
    );
}

export function DesktopSidebar() {
    const navigate = useNavigate();
    const location = useLocation();
    const activeTab = location.pathname.replace('/', '') || 'home';

    const notifications = useNotificationStore(state => state.notifications);
    const unreadCount = notifications.filter(n => n.unread).length;

    const { openSearch, openNotifications } = useUiStore();

    return (
        <div className="hidden md:flex flex-col w-[260px] h-screen sticky top-0 py-8 px-6 overflow-y-auto no-scrollbar shrink-0 z-50">
            {/* Logo */}
            <div className="flex items-center gap-3 mb-12 px-2 cursor-pointer" onClick={() => navigate('/home')}>
                <div className="w-10 h-10 bg-white shadow-xl shadow-slate-200/50 rounded-2xl flex items-center justify-center text-indigo-600">
                    <Terminal size={20} strokeWidth={2.5} />
                </div>
                <h1 className="text-xl font-black tracking-tighter text-slate-900 uppercase">
                    TechStack
                </h1>
            </div>

            {/* Navigation */}
            <nav className="flex flex-col gap-2 mb-8">
                <NavItem
                    icon={<Home size={22} />}
                    label="Home"
                    isActive={activeTab === 'home' || activeTab === ''}
                    onClick={() => navigate('/home')}
                />
                <NavItem
                    icon={<Compass size={22} />}
                    label="Explore"
                    isActive={activeTab === 'explore'}
                    onClick={() => navigate('/explore')}
                />
                <NavItem
                    icon={<Bookmark size={22} />}
                    label="Saved"
                    isActive={activeTab === 'saved'}
                    onClick={() => navigate('/saved')}
                />
                <NavItem
                    icon={<User size={22} />}
                    label="Profile"
                    isActive={activeTab === 'profile'}
                    onClick={() => navigate('/profile')}
                />
            </nav>

            {/* Actions */}
            <div className="flex flex-col gap-2 mt-auto">
                <NavItem
                    icon={<Search size={22} />}
                    label="Search"
                    onClick={openSearch}
                />
                <NavItem
                    icon={<Bell size={22} />}
                    label="Notifications"
                    onClick={openNotifications}
                    badgeCount={unreadCount}
                />
            </div>
        </div>
    );
}
