import { useEffect, useCallback, useRef } from 'react';

interface UseIntersectionObserverProps {
    onIntersect: () => void;
    enabled?: boolean;
    rootMargin?: string;
    threshold?: number;
}

export function useIntersectionObserver({
    onIntersect,
    enabled = true,
    rootMargin = '0px',
    threshold = 1.0,
}: UseIntersectionObserverProps) {
    const targetRef = useRef<HTMLDivElement | null>(null);

    const handleObserver = useCallback(
        (entries: IntersectionObserverEntry[]) => {
            const [target] = entries;
            if (target.isIntersecting && enabled) {
                onIntersect();
            }
        },
        [onIntersect, enabled]
    );

    useEffect(() => {
        const element = targetRef.current;
        if (!element) return;

        const observer = new IntersectionObserver(handleObserver, {
            rootMargin,
            threshold,
        });

        observer.observe(element);

        return () => observer.unobserve(element);
    }, [handleObserver, rootMargin, threshold]);

    return { targetRef };
}
