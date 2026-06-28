import { Navigate } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import { OnboardingCarousel } from '../components/OnboardingCarousel';
import { LandingMarketing } from '../components/LandingMarketing';

/**
 * 소개(랜딩) 페이지 — 서비스의 공개 진입점.
 * 모바일: 3단계 온보딩 캐러셀 (tracer.dc.html)
 * 데스크톱: 스크롤형 마케팅 랜딩 (tracer-web.dc.html)
 * 이미 로그인한 사용자는 홈으로 보냅니다.
 */
export function LandingPage() {
    const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

    if (isAuthenticated) {
        return <Navigate to="/home" replace />;
    }

    return (
        <>
            <div className="md:hidden">
                <OnboardingCarousel />
            </div>
            <div className="hidden md:block">
                <LandingMarketing />
            </div>
        </>
    );
}
