import { useLocation, useNavigate, useOutlet } from 'react-router-dom';
import { useRef, useEffect } from 'react';
import gsap from 'gsap';
import { useGSAP } from '@gsap/react';
import { Terminal, Search, Bell, Home, Compass, Bookmark, User } from 'lucide-react';
import { TabButton } from '@/components/ui/TabButton';
import { useUiStore } from '@/stores/uiStore';

import { SearchOverlay } from '@/features/search/components/SearchOverlay';
import { NotificationOverlay } from '@/features/notifications/components/NotificationOverlay';
import { FolderOverlay } from '@/features/saved/components/FolderOverlay';
import { UpgradePlanOverlay } from '@/features/profile/pages/UpgradePlanOverlay';
import { MySourcesOverlay } from '@/features/profile/pages/MySourcesOverlay';
import { PaymentMethodManageOverlay } from '@/features/payment/pages/PaymentMethodManageOverlay';
import { SubscriptionManageOverlay } from '@/features/payment/pages/SubscriptionManageOverlay';
import { PaymentHistoryOverlay } from '@/features/payment/pages/PaymentHistoryOverlay';
import { DesktopSidebar } from './DesktopSidebar';
import { useNotifications, useNotificationSubscription } from '@/features/notifications/api/notificationApi';

export function AppLayout() {
    const navigate = useNavigate();
    const location = useLocation();
    const outlet = useOutlet();
    const activeTab = location.pathname.replace('/', '') || 'home';

    const blob1Ref = useRef<HTMLDivElement>(null);
    const blob2Ref = useRef<HTMLDivElement>(null);
    const contentRef = useRef<HTMLDivElement>(null);
    const mainWrapperRef = useRef<HTMLDivElement>(null);
    const progressBarRef = useRef<HTMLDivElement>(null);
    const scrollContainerRef = useRef<HTMLElement>(null);

    useGSAP(() => {
        gsap.to(blob1Ref.current, { scale: 1.2, x: 50, y: 30, duration: 7.5, repeat: -1, yoyo: true, ease: "sine.inOut" });
        gsap.to(blob2Ref.current, { scale: 1.3, x: -60, y: -40, duration: 9, repeat: -1, yoyo: true, ease: "sine.inOut" });
        gsap.fromTo(mainWrapperRef.current, { opacity: 0 }, { opacity: 1, duration: 0.4, ease: 'power2.out' });
    }, []);

    useGSAP(() => {
        gsap.fromTo(contentRef.current,
            { opacity: 0, y: 10 },
            { opacity: 1, y: 0, duration: 0.2, ease: 'power2.out' }
        );
    }, [location.pathname]);

    useEffect(() => {
        const handleScroll = () => {
            if (!scrollContainerRef.current || !progressBarRef.current) return;
            const { scrollTop, scrollHeight, clientHeight } = scrollContainerRef.current;
            const maxScroll = scrollHeight - clientHeight;
            const progress = maxScroll > 0 ? scrollTop / maxScroll : 0;
            gsap.to(progressBarRef.current, { scaleX: progress, duration: 0.1, ease: 'none' });
        };
        const container = scrollContainerRef.current;
        if (container) {
            container.addEventListener('scroll', handleScroll);
            handleScroll();
        }
        return () => container?.removeEventListener('scroll', handleScroll);
    }, []);

    // Subscribe to SSE notifications
    useNotificationSubscription();

    const { data: notificationData } = useNotifications();
    const notifications = notificationData?.pages.flatMap(p => p.content) || [];
    const unreadCount = notifications.filter(n => !n.isRead).length;

    const {
        isSearchMounted,
        isNotificationsMounted,
        isFolderMounted,
        isUpgradeMounted,
        openSearch,
        openNotifications,
        closeSearch,
        closeNotifications,
        closeFolderManagement,
        closeUpgradePlan,
        isSourcesMounted,
        closeSourcesManagement,
        isPaymentMethodMounted,
        closePaymentMethod,
        isSubscriptionMounted,
        closeSubscriptionManage,
        isPaymentHistoryMounted,
        closePaymentHistory,
    } = useUiStore();

    // 탭 이동(라우트 변경) 시 오버레이 창 닫기
    useEffect(() => {
        closeNotifications();
        closeSearch();
        closeFolderManagement();
        closeUpgradePlan();
        closeSourcesManagement();
        closePaymentMethod();
        closeSubscriptionManage();
        closePaymentHistory();
    }, [location.pathname, closeNotifications, closeSearch, closeFolderManagement, closeUpgradePlan, closeSourcesManagement, closePaymentMethod, closeSubscriptionManage, closePaymentHistory]);

    return (
        <div className="h-[100dvh] overflow-hidden bg-slate-200 flex justify-center font-sans selection:bg-slate-300">
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

            <div className="w-full max-w-[1200px] flex flex-col md:flex-row md:justify-center relative z-10 w-full">
                <DesktopSidebar />
                <div className="w-full max-w-[480px] md:max-w-[540px] lg:max-w-[600px] bg-white/30 backdrop-blur-[60px] h-[100dvh] shadow-[0_20px_50px_rgba(0,0,0,0.1)] flex flex-col relative overflow-hidden border-x border-white/40 shrink-0 mx-auto md:mx-0">
                    <div ref={mainWrapperRef} className="flex-1 flex flex-col overflow-hidden relative opacity-0">

                        <div
                            ref={progressBarRef}
                            className="fixed top-0 left-0 right-0 h-1 bg-slate-800/20 origin-left z-[60]"
                            style={{ transform: 'scaleX(0)', maxWidth: 480, margin: '0 auto' }}
                        />

                        {isSearchMounted && <SearchOverlay />}
                        {isNotificationsMounted && <NotificationOverlay />}
                        {isFolderMounted && <FolderOverlay />}
                        {isUpgradeMounted && <UpgradePlanOverlay />}
                        {isSourcesMounted && <MySourcesOverlay />}
                        {isPaymentMethodMounted && <PaymentMethodManageOverlay />}
                        {isSubscriptionMounted && <SubscriptionManageOverlay />}
                        {isPaymentHistoryMounted && <PaymentHistoryOverlay />}

                        <header className="md:hidden sticky top-0 bg-white/40 backdrop-blur-3xl z-40 px-6 pt-5 pb-2 border-b border-white/30">
                            <div className="flex items-center justify-between mb-1">
                                <div className="flex items-center gap-2">
                                    <div className="w-7 h-7 bg-white/60 backdrop-blur-md rounded-lg flex items-center justify-center text-slate-800 shadow-sm border border-white/50">
                                        <Terminal size={14} strokeWidth={2.5} />
                                    </div>
                                    <h1 className="text-base font-black tracking-tighter text-slate-900 uppercase">TechStack</h1>
                                </div>
                                <div className="flex items-center gap-1.5">
                                    <button
                                        onClick={openSearch}
                                        className="p-2 bg-white/40 backdrop-blur-md rounded-full text-slate-600 border border-white/50 shadow-sm active:scale-90 transition-transform"
                                    >
                                        <Search size={16} />
                                    </button>
                                    <button
                                        onClick={openNotifications}
                                        className="relative p-2 bg-white/40 backdrop-blur-md rounded-full text-slate-600 border border-white/50 shadow-sm active:scale-90 transition-transform"
                                    >
                                        <Bell size={16} />
                                        {unreadCount > 0 && (
                                            <span className="absolute top-1.5 right-1.5 w-1.5 h-1.5 bg-rose-500 rounded-full border-2 border-white"></span>
                                        )}
                                    </button>
                                </div>
                            </div>
                        </header>

                        <main ref={scrollContainerRef} className="flex-1 overflow-y-auto no-scrollbar relative">
                            <div ref={contentRef} className="h-full">
                                {outlet}
                            </div>
                        </main>

                        <nav className="md:hidden absolute bottom-0 w-full bg-white/30 backdrop-blur-[40px] border-t border-white/20 px-8 pt-3 pb-4 flex justify-between items-center z-[60]">
                            <TabButton active={activeTab === 'home' || activeTab === ''} onClick={() => navigate('/home')} icon={<Home size={18} />} label="Home" />
                            <TabButton active={activeTab === 'explore'} onClick={() => navigate('/explore')} icon={<Compass size={18} />} label="Explore" />
                            <TabButton active={activeTab === 'saved'} onClick={() => navigate('/saved')} icon={<Bookmark size={18} />} label="Saved" />
                            <TabButton active={activeTab === 'profile'} onClick={() => navigate('/profile')} icon={<User size={18} />} label="Me" />
                        </nav>
                    </div>
                </div>
            </div>
        </div>
    );
}
