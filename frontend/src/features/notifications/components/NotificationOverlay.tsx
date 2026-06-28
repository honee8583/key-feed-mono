import { useRef, useEffect, useState, useMemo } from 'react';
import gsap from 'gsap';
import { useGSAP } from '@gsap/react';
import { X } from 'lucide-react';
import { cn } from '@/utils/cn';
import { formatRelativeTime } from '@/utils/time';
import { useUiStore } from '@/stores/uiStore';
import { useNotifications } from '../api/notificationApi';
import { useIntersectionObserver } from '@/hooks/useIntersectionObserver';
import type { Notification } from '@/types';

type Segment = 'all' | 'unread';

/**
 * 알림 생성 시각(ms)을 구한다. SSE로 실시간 수신한 알림은 createdAt이
 * 비어 있을 수 있는데(백엔드 푸시 페이로드에 미포함), 이 경우 NaN으로 처리해
 * "방금 도착 = 지금"으로 간주한다. `new Date(null)`은 1970이 되므로 falsy 검사가 필요하다.
 */
function parseCreatedAt(createdAt: string | null | undefined): number {
    if (!createdAt) return NaN;
    return new Date(createdAt).getTime();
}

/** createdAt이 없거나 유효하지 않으면(=방금 수신) "방금 전"으로 표시한다. */
function timeLabel(createdAt: string): string {
    return Number.isNaN(parseCreatedAt(createdAt)) ? '방금 전' : formatRelativeTime(createdAt);
}

/** 알림 생성 시각을 기준으로 오늘 / 이번 주 / 그 이전으로 묶는다. */
function groupByPeriod(items: Notification[], now: Date) {
    const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
    const startOfWeek = startOfToday - 6 * 24 * 60 * 60 * 1000; // 오늘 포함 최근 7일

    const today: Notification[] = [];
    const week: Notification[] = [];
    const earlier: Notification[] = [];

    for (const n of items) {
        const t = parseCreatedAt(n.createdAt);
        if (Number.isNaN(t) || t >= startOfToday) today.push(n); // 무효/미수신 시각은 오늘로
        else if (t >= startOfWeek) week.push(n);
        else earlier.push(n);
    }

    return [
        { label: '오늘', items: today },
        { label: '이번 주', items: week },
        { label: '그 이전', items: earlier },
    ].filter((g) => g.items.length > 0);
}

export function NotificationOverlay() {
    const { isNotificationsOpen, closeNotifications, unmountNotifications } = useUiStore();
    const [segment, setSegment] = useState<Segment>('all');

    const { data, fetchNextPage, hasNextPage, isFetchingNextPage, isLoading, isError } = useNotifications();
    const notifications = useMemo(() => data?.pages.flatMap((page) => page.content) ?? [], [data]);

    const visible = useMemo(
        () => (segment === 'unread' ? notifications.filter((n) => !n.isRead) : notifications),
        [notifications, segment]
    );
    const groups = useMemo(() => groupByPeriod(visible, new Date()), [visible]);
    const hasUnread = useMemo(() => notifications.some((n) => !n.isRead), [notifications]);

    const { targetRef } = useIntersectionObserver({
        onIntersect: fetchNextPage,
        enabled: hasNextPage && !isFetchingNextPage,
    });

    const overlayRef = useRef<HTMLDivElement>(null);
    const { contextSafe } = useGSAP({ scope: overlayRef });

    useEffect(() => {
        contextSafe(() => {
            if (isNotificationsOpen) {
                gsap.to(overlayRef.current, { y: 0, opacity: 1, duration: 0.4, ease: 'power3.out' });
            } else {
                gsap.to(overlayRef.current, {
                    y: '100%',
                    opacity: 0,
                    duration: 0.3,
                    ease: 'power2.in',
                    onComplete: unmountNotifications,
                });
            }
        })();
    }, [isNotificationsOpen, unmountNotifications, contextSafe]);

    return (
        <div
            ref={overlayRef}
            className="absolute inset-0 z-[100] flex translate-y-full flex-col bg-canvas opacity-0"
        >
            {/* Header */}
            <div className="px-5 pb-3 pt-[calc(env(safe-area-inset-top)+34px)]">
                <div className="mb-3 flex items-center justify-between">
                    <h1 className="font-display text-[30px] font-medium tracking-tightest text-primary">알림</h1>
                    <button
                        onClick={closeNotifications}
                        aria-label="닫기"
                        className="flex h-[38px] w-[38px] items-center justify-center rounded-full text-ink transition-colors hover:bg-soft-stone active:bg-soft-stone"
                    >
                        <X size={20} strokeWidth={1.8} />
                    </button>
                </div>

                {/* Segment filter */}
                <div className="flex gap-2">
                    {([
                        { key: 'all', label: '전체' },
                        { key: 'unread', label: '안 읽음' },
                    ] as const).map((s) => (
                        <button
                            key={s.key}
                            onClick={() => setSegment(s.key)}
                            className={cn(
                                'rounded-full border px-4 py-[7px] text-[13px] font-medium transition-colors',
                                segment === s.key
                                    ? 'border-primary bg-primary text-white'
                                    : 'border-hairline bg-canvas text-[#616161] hover:bg-soft-stone'
                            )}
                        >
                            {s.label}
                            {s.key === 'unread' && hasUnread && (
                                <span
                                    className={cn(
                                        'ml-1.5 inline-block h-1.5 w-1.5 rounded-full align-middle',
                                        segment === 'unread' ? 'bg-coral-soft' : 'bg-coral'
                                    )}
                                />
                            )}
                        </button>
                    ))}
                </div>
            </div>

            {/* Body */}
            <div className="no-scrollbar flex-1 overflow-y-auto px-5 pb-24 pt-2">
                {isLoading ? (
                    <div className="flex justify-center py-16">
                        <div className="h-7 w-7 animate-spin rounded-full border-2 border-hairline border-t-primary" />
                    </div>
                ) : isError ? (
                    <div className="px-6 py-20 text-center text-muted">
                        <div className="mb-2 font-mono text-[12px] tracking-[0.5px]">LOAD FAILED</div>
                        <div className="text-[15px]">알림을 불러오지 못했어요.</div>
                    </div>
                ) : groups.length > 0 ? (
                    <>
                        {groups.map((group) => (
                            <div key={group.label}>
                                <div className="px-0.5 pb-1 pt-3 font-mono text-[11px] tracking-[0.6px] text-muted">
                                    {group.label}
                                </div>
                                {group.items.map((n) => (
                                    <div
                                        key={n.id}
                                        className="flex items-start gap-[11px] border-b border-[#f2f2f2] py-[15px]"
                                    >
                                        <div className="flex w-2 shrink-0 justify-center pt-[5px]">
                                            {!n.isRead && <span className="h-2 w-2 rounded-full bg-coral" />}
                                        </div>
                                        <div className="min-w-0 flex-1">
                                            <div className="mb-1 flex items-center justify-between gap-2">
                                                <span className="truncate font-mono text-[11px] tracking-[0.4px] text-[#75758a]">
                                                    {n.title}
                                                </span>
                                                <span className="shrink-0 text-[12px] text-muted">
                                                    {timeLabel(n.createdAt)}
                                                </span>
                                            </div>
                                            <p
                                                className={cn(
                                                    'text-[15px] leading-[1.45]',
                                                    n.isRead ? 'text-muted' : 'text-ink'
                                                )}
                                            >
                                                {n.message}
                                            </p>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        ))}

                        {hasNextPage && (
                            <div ref={targetRef} className="flex justify-center py-4">
                                {isFetchingNextPage && (
                                    <div className="h-5 w-5 animate-spin rounded-full border-2 border-hairline border-t-primary" />
                                )}
                            </div>
                        )}
                    </>
                ) : (
                    <div className="px-6 py-20 text-center text-muted">
                        <div className="font-mono text-[12px] tracking-[0.5px]">ALL CAUGHT UP</div>
                        <div className="mt-2 text-[15px]">
                            {segment === 'unread' ? '읽지 않은 알림이 없어요.' : '알림이 없어요.'}
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}
