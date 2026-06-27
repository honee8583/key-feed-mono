import { useLocation, useNavigate } from 'react-router-dom';
import { Search, Bell, Home, Compass, Bookmark, User } from 'lucide-react';
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
                'group relative flex w-full items-center gap-3 rounded-full px-4 py-2.5 transition-colors',
                isActive
                    ? 'bg-soft-stone text-primary'
                    : 'text-muted hover:bg-soft-stone/60 hover:text-ink'
            )}
        >
            {icon}
            <span className={cn('text-[15px]', isActive && 'font-medium')}>{label}</span>
            {badgeCount !== undefined && badgeCount > 0 && (
                <span className="absolute right-4 flex h-5 min-w-5 items-center justify-center rounded-full bg-coral px-1 text-[10px] font-medium text-white">
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

    const notifications = useNotificationStore((state) => state.notifications);
    const unreadCount = notifications.filter((n) => !n.isRead).length;

    const { openSearch, openNotifications } = useUiStore();

    return (
        <div className="sticky top-0 z-50 hidden h-screen w-[260px] shrink-0 flex-col overflow-y-auto px-6 py-8 no-scrollbar md:flex">
            {/* Logo */}
            <button
                onClick={() => navigate('/home')}
                className="mb-12 flex items-center gap-2.5 px-3"
                aria-label="tracer 홈"
            >
                <span className="h-[19px] w-[19px] rounded-[5px] bg-deep-green" />
                <span className="font-display text-[22px] font-medium tracking-[-0.4px] text-primary">tracer</span>
            </button>

            {/* Navigation */}
            <nav className="mb-8 flex flex-col gap-1.5">
                <NavItem
                    icon={<Home size={22} strokeWidth={1.7} />}
                    label="홈"
                    isActive={activeTab === 'home' || activeTab === ''}
                    onClick={() => navigate('/home')}
                />
                <NavItem
                    icon={<Compass size={22} strokeWidth={1.7} />}
                    label="탐색"
                    isActive={activeTab === 'explore'}
                    onClick={() => navigate('/explore')}
                />
                <NavItem
                    icon={<Bookmark size={22} strokeWidth={1.7} />}
                    label="저장"
                    isActive={activeTab === 'saved'}
                    onClick={() => navigate('/saved')}
                />
                <NavItem
                    icon={<User size={22} strokeWidth={1.7} />}
                    label="MY"
                    isActive={activeTab === 'profile'}
                    onClick={() => navigate('/profile')}
                />
            </nav>

            {/* Actions */}
            <div className="mt-auto flex flex-col gap-1.5">
                <NavItem
                    icon={<Search size={22} strokeWidth={1.7} />}
                    label="검색"
                    onClick={openSearch}
                />
                <NavItem
                    icon={<Bell size={22} strokeWidth={1.7} />}
                    label="알림"
                    onClick={openNotifications}
                    badgeCount={unreadCount}
                />
            </div>
        </div>
    );
}
