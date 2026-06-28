import { useLocation, useNavigate } from 'react-router-dom';

const ACTIVE = '#17171c';
const INACTIVE = '#93939f';

type TabKey = 'home' | 'explore' | 'saved' | 'profile';

interface Tab {
    key: TabKey;
    path: string;
    label: string;
    icon: React.ReactNode;
}

// tracer 디자인 시스템의 하단 탭 아이콘 (23×23, stroke 1.6)
const TABS: Tab[] = [
    {
        key: 'home',
        path: '/home',
        label: '홈',
        icon: (
            <svg width="23" height="23" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6">
                <path d="M4 11l8-6.5 8 6.5v8a1 1 0 0 1-1 1h-4.5v-6h-5v6H5a1 1 0 0 1-1-1z" strokeLinejoin="round" />
            </svg>
        ),
    },
    {
        key: 'explore',
        path: '/explore',
        label: '추천',
        icon: (
            <svg width="23" height="23" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6">
                <circle cx="12" cy="12" r="8.5" />
                <path d="M15.5 8.5l-2 5.5-5 2 2-5.5z" fill="currentColor" stroke="none" />
            </svg>
        ),
    },
    {
        key: 'saved',
        path: '/saved',
        label: '북마크',
        icon: (
            <svg width="23" height="23" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6">
                <path d="M6 4h12v16l-6-4-6 4z" strokeLinejoin="round" />
            </svg>
        ),
    },
    {
        key: 'profile',
        path: '/profile',
        label: 'MY',
        icon: (
            <svg width="23" height="23" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6">
                <circle cx="12" cy="8" r="3.4" />
                <path d="M5.5 19.5a6.5 6.5 0 0 1 13 0" strokeLinecap="round" />
            </svg>
        ),
    },
];

export function TracerTabBar() {
    const navigate = useNavigate();
    const location = useLocation();
    const activeTab = location.pathname.replace('/', '') || 'home';

    return (
        <nav className="md:hidden absolute bottom-0 left-0 right-0 z-[60] flex border-t border-[#f2f2f2] bg-white/[0.92] pb-7 pt-2 backdrop-blur-[14px]">
            {TABS.map((tab) => {
                const isActive = activeTab === tab.key || (tab.key === 'home' && activeTab === '');
                return (
                    <button
                        key={tab.key}
                        onClick={() => navigate(tab.path)}
                        className="flex flex-1 flex-col items-center gap-1 transition-transform active:scale-95"
                        style={{ color: isActive ? ACTIVE : INACTIVE }}
                    >
                        {tab.icon}
                        <span className="font-mono text-[10px] tracking-[0.3px]">{tab.label}</span>
                    </button>
                );
            })}
        </nav>
    );
}
