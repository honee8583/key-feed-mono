import { createBrowserRouter, Navigate } from 'react-router-dom';
import { AppLayout } from '@/layouts/AppLayout';
import { AuthLayout } from '@/layouts/AuthLayout';
import { AuthGuard } from '@/components/ui/AuthGuard';

import { SignupPage } from '@/features/auth/pages/SignupPage';
import { VerifyPage } from '@/features/auth/pages/VerifyPage';
import { WelcomePage } from '@/features/auth/pages/WelcomePage';
import { LoginPage } from '@/features/auth/pages/LoginPage';

import { HomeTab } from '@/features/feed/pages/HomeTab';
import { ExploreTab } from '@/features/explore/pages/ExploreTab';
import { SavedTab } from '@/features/saved/pages/SavedTab';
import { ProfileTab } from '@/features/profile/pages/ProfileTab';

export const router = createBrowserRouter([
    {
        path: '/',
        element: <Navigate to="/auth/login" replace />,
    },
    {
        path: '/auth',
        element: <AuthLayout />,
        children: [
            { path: 'signup', element: <SignupPage /> },
            { path: 'verify', element: <VerifyPage /> },
            { path: 'welcome', element: <WelcomePage /> },
            { path: 'login', element: <LoginPage /> },
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
        ]
    }
]);
