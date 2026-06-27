// Korean date helpers for the feed. The feed API returns naive ISO timestamps
// (e.g. "2025-03-01T12:00:00"); we treat them as local time.

const WEEKDAYS_KO = ['일', '월', '화', '수', '목', '금', '토'] as const;

/** "6월 27일 금" — today's date label for the feed eyebrow. */
export function formatTodayLabel(date: Date = new Date()): string {
    return `${date.getMonth() + 1}월 ${date.getDate()}일 ${WEEKDAYS_KO[date.getDay()]}`;
}

/**
 * Relative Korean time for a post's publishedAt.
 * 방금 전 → N분 전 → N시간 전 → 어제 → N일 전 → "M월 D일" for anything older.
 */
export function formatRelativeTime(iso: string, now: Date = new Date()): string {
    const date = new Date(iso);
    if (Number.isNaN(date.getTime())) return '';

    const diffMs = now.getTime() - date.getTime();
    const diffMin = Math.floor(diffMs / 60_000);

    if (diffMin < 1) return '방금 전';
    if (diffMin < 60) return `${diffMin}분 전`;

    const diffHour = Math.floor(diffMin / 60);
    if (diffHour < 24) return `${diffHour}시간 전`;

    const diffDay = Math.floor(diffHour / 24);
    if (diffDay === 1) return '어제';
    if (diffDay < 7) return `${diffDay}일 전`;

    return `${date.getFullYear()}년 ${date.getMonth() + 1}월 ${date.getDate()}일`;
}
