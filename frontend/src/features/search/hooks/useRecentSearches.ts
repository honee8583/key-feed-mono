import { useCallback, useEffect, useState } from 'react';

const STORAGE_KEY = 'tracer.recentSearches';
const MAX_RECENT = 10;

function read(): string[] {
    try {
        const raw = localStorage.getItem(STORAGE_KEY);
        if (!raw) return [];
        const parsed = JSON.parse(raw);
        return Array.isArray(parsed) ? parsed.filter((v): v is string => typeof v === 'string') : [];
    } catch {
        return [];
    }
}

/**
 * 최근 검색어를 localStorage에 보관한다. 백엔드 API가 없는 클라이언트 전용 기능.
 */
export function useRecentSearches() {
    const [recent, setRecent] = useState<string[]>(() => read());

    // 다른 탭/오버레이에서 변경된 값을 반영
    useEffect(() => {
        const onStorage = (e: StorageEvent) => {
            if (e.key === STORAGE_KEY) setRecent(read());
        };
        window.addEventListener('storage', onStorage);
        return () => window.removeEventListener('storage', onStorage);
    }, []);

    const persist = useCallback((next: string[]) => {
        setRecent(next);
        try {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
        } catch {
            // 저장 실패는 조용히 무시 (사생활 모드 등)
        }
    }, []);

    const addRecent = useCallback((keyword: string) => {
        const trimmed = keyword.trim();
        if (!trimmed) return;
        setRecent((prev) => {
            const next = [trimmed, ...prev.filter((k) => k !== trimmed)].slice(0, MAX_RECENT);
            try {
                localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
            } catch {
                // ignore
            }
            return next;
        });
    }, []);

    const clearRecent = useCallback(() => persist([]), [persist]);

    return { recent, addRecent, clearRecent };
}
