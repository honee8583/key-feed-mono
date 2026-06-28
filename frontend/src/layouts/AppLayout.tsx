import { useLocation, useNavigate, useOutlet } from 'react-router-dom';
import { useRef, useEffect } from 'react';
import gsap from 'gsap';
import { useGSAP } from '@gsap/react';
import { Search, Bell } from 'lucide-react';
import { TracerTabBar } from '@/components/ui/TracerTabBar';
import { useUiStore } from '@/stores/uiStore';

import { SearchOverlay } from '@/features/search/components/SearchOverlay';
import { NotificationOverlay } from '@/features/notifications/components/NotificationOverlay';
import { FolderOverlay } from '@/features/saved/components/FolderOverlay';
import { UpgradePlanOverlay } from '@/features/profile/pages/UpgradePlanOverlay';
import { MySourcesOverlay } from '@/features/profile/pages/MySourcesOverlay';
import { PaymentMethodManageOverlay } from '@/features/payment/pages/PaymentMethodManageOverlay';
import { SubscriptionManageOverlay } from '@/features/payment/pages/SubscriptionManageOverlay';
import { PaymentHistoryOverlay } from '@/features/payment/pages/PaymentHistoryOverlay';
import { PasswordChangeOverlay } from '@/features/profile/pages/PasswordChangeOverlay';
import { WithdrawOverlay } from '@/features/profile/pages/WithdrawOverlay';
import { DesktopSidebar } from './DesktopSidebar';
import { useNotifications, useNotificationSubscription } from '@/features/notifications/api/notificationApi';

export function AppLayout() {
    const location = useLocation();
    const navigate = useNavigate();
    const outlet = useOutlet();

    const contentRef = useRef<HTMLDivElement>(null);
    const mainWrapperRef = useRef<HTMLDivElement>(null);
    const progressBarRef = useRef<HTMLDivElement>(null);
    const scrollContainerRef = useRef<HTMLElement>(null);

    useGSAP(() => {
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
        isPasswordChangeMounted,
        closePasswordChange,
        isWithdrawMounted,
        closeWithdraw,
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
        closePasswordChange();
        closeWithdraw();
    }, [location.pathname, closeNotifications, closeSearch, closeFolderManagement, closeUpgradePlan, closeSourcesManagement, closePaymentMethod, closeSubscriptionManage, closePaymentHistory, closePasswordChange, closeWithdraw]);

    return (
        <div className="flex h-[100dvh] justify-center overflow-hidden bg-soft-stone font-pretendard text-ink selection:bg-soft-stone">
            <div className="relative z-10 flex w-full max-w-[1200px] flex-col md:flex-row md:justify-center">
                <DesktopSidebar />
                <div className="relative mx-auto flex h-[100dvh] w-full max-w-[480px] shrink-0 flex-col overflow-hidden border-x border-card-border bg-canvas md:mx-0 md:max-w-[540px] md:shadow-[0_20px_50px_rgba(0,0,0,0.06)] lg:max-w-[600px]">
                    <div ref={mainWrapperRef} className="flex-1 flex flex-col overflow-hidden relative opacity-0">

                        <div
                            ref={progressBarRef}
                            className="fixed top-0 left-0 right-0 h-0.5 bg-primary/15 origin-left z-[60]"
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
                        {isPasswordChangeMounted && <PasswordChangeOverlay />}
                        {isWithdrawMounted && <WithdrawOverlay />}

                        <header className="md:hidden sticky top-0 z-40 border-b border-card-border bg-canvas/95 px-5 pb-3 pt-[calc(env(safe-area-inset-top)+18px)] backdrop-blur-xl">
                            <div className="flex items-center justify-between">
                                <button
                                    onClick={() => navigate('/home')}
                                    className="flex items-center gap-[7px]"
                                    aria-label="tracer 홈"
                                >
                                    <span className="h-[15px] w-[15px] rounded-[4px] bg-deep-green" />
                                    <span className="font-display text-[20px] font-medium tracking-[-0.3px] text-primary">tracer</span>
                                </button>
                                <div className="flex items-center gap-1.5">
                                    <button
                                        onClick={openSearch}
                                        aria-label="검색"
                                        className="flex h-[38px] w-[38px] items-center justify-center rounded-full text-ink transition-colors hover:bg-soft-stone active:bg-soft-stone"
                                    >
                                        <Search size={19} strokeWidth={1.7} />
                                    </button>
                                    <button
                                        onClick={openNotifications}
                                        aria-label="알림"
                                        className="relative flex h-[38px] w-[38px] items-center justify-center rounded-full text-ink transition-colors hover:bg-soft-stone active:bg-soft-stone"
                                    >
                                        <Bell size={19} strokeWidth={1.7} />
                                        {unreadCount > 0 && (
                                            <span className="absolute right-[9px] top-[9px] h-[7px] w-[7px] rounded-full border-[1.5px] border-canvas bg-coral" />
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

                        <TracerTabBar />
                    </div>
                </div>
            </div>
        </div>
    );
}
