import { createBrowserRouter } from 'react-router-dom';
import { AppLayout } from '@/layouts/AppLayout';
import { AuthLayout } from '@/layouts/AuthLayout';
import { AuthGuard } from '@/components/ui/AuthGuard';

import { LandingPage } from '@/features/landing/pages/LandingPage';

import { SignupPage } from '@/features/auth/pages/SignupPage';
import { VerifyPage } from '@/features/auth/pages/VerifyPage';
import { WelcomePage } from '@/features/auth/pages/WelcomePage';
import { LoginPage } from '@/features/auth/pages/LoginPage';
import { ForgotPasswordPage } from '@/features/auth/pages/ForgotPasswordPage';

import { HomeTab } from '@/features/feed/pages/HomeTab';
import { ExploreTab } from '@/features/explore/pages/ExploreTab';
import { SavedTab } from '@/features/saved/pages/SavedTab';
import { ProfileTab } from '@/features/profile/pages/ProfileTab';
import { PaymentCallback } from '@/features/payment/pages/PaymentCallback';

export const router = createBrowserRouter([
    {
        // 공개 소개(랜딩) 페이지. 모바일은 온보딩 캐러셀, 데스크톱은 마케팅 랜딩.
        path: '/',
        element: <LandingPage />,
    },
    {
        // Login, signup and the verify step use their own full-bleed responsive
        // layout (Cohere design system), so they sit outside the glass AuthLayout shell.
        path: '/auth/login',
        element: <LoginPage />,
    },
    {
        path: '/auth/signup',
        element: <SignupPage />,
    },
    {
        path: '/auth/verify',
        element: <VerifyPage />,
    },
    {
        path: '/auth/forgot-password',
        element: <ForgotPasswordPage />,
    },
    {
        path: '/auth',
        element: <AuthLayout />,
        children: [
            { path: 'welcome', element: <WelcomePage /> },
        ]
    },
    {
        path: '/',
        element: (
            <AuthGuard>
                <AppLayout />
            </AuthGuard>
        ),
        children: [
            { path: 'home', element: <HomeTab /> },
            { path: 'explore', element: <ExploreTab /> },
            { path: 'saved', element: <SavedTab /> },
            { path: 'profile', element: <ProfileTab /> },
            { path: 'payment/callback', element: <PaymentCallback /> },
        ]
    }
]);
