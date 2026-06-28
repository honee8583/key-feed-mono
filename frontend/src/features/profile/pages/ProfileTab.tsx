import { useNavigate } from 'react-router-dom';
import { ChevronRight, Crown } from 'lucide-react';

import { useAuthStore } from '@/stores/authStore';
import { useUiStore } from '@/stores/uiStore';
import { useMySubscription } from '@/features/payment/api/subscriptionApi';
import { useMySources } from '../api/sourceApi';

// 마이페이지 행(設定 항목) — 라벨 + (선택) 카운트 + 셰브론
function SettingRow({
    label,
    value,
    onClick,
    last = false,
    danger = false,
}: {
    label: string;
    value?: string;
    onClick?: () => void;
    last?: boolean;
    danger?: boolean;
}) {
    return (
        <button
            onClick={onClick}
            className={`flex w-full items-center px-4 py-[15px] text-left transition-colors active:bg-black/[0.02] ${
                last ? '' : 'border-b border-[#f2f2f2]'
            }`}
        >
            <span className={`flex-1 text-[16px] ${danger ? 'text-brand-error' : 'text-ink'}`}>{label}</span>
            {value && <span className="mr-2 text-[13px] text-muted">{value}</span>}
            <ChevronRight size={16} className="text-[#c4c4cc]" />
        </button>
    );
}

// 그룹 라벨 — tracer 모노 캡션
function GroupLabel({ children }: { children: React.ReactNode }) {
    return (
        <div className="mb-[10px] pl-0.5 font-mono text-[11px] uppercase tracking-mono-label text-muted">
            {children}
        </div>
    );
}

export function ProfileTab() {
    const navigate = useNavigate();
    const user = useAuthStore((state) => state.user);
    const logout = useAuthStore((state) => state.logout);

    const openUpgradePlan = useUiStore((state) => state.openUpgradePlan);
    const openSourcesManagement = useUiStore((state) => state.openSourcesManagement);
    const openPaymentMethod = useUiStore((state) => state.openPaymentMethod);
    const openPasswordChange = useUiStore((state) => state.openPasswordChange);
    const openWithdraw = useUiStore((state) => state.openWithdraw);

    const { data: subscription } = useMySubscription();
    const isSubscribed =
        subscription?.status === 'ACTIVE' ||
        subscription?.status === 'CANCELED' ||
        subscription?.status === 'PAUSED';

    const { data: mySources } = useMySources();
    const sourceCount = mySources?.length;

    const handleLogout = () => {
        logout();
        navigate('/auth/login');
    };

    const name = user?.name ?? '사용자';
    const email = user?.email ?? '';
    const initial = name.trim().charAt(0) || 'U';

    return (
        <div className="min-h-full bg-[#fafafa] font-pretendard">
            {/* 헤더 */}
            <div className="px-5 pb-2 pt-6">
                <h1 className="font-display text-[30px] font-medium tracking-tightest text-primary">마이페이지</h1>
            </div>

            <div className="px-5 pb-28 pt-4">
                {/* 프로필 */}
                <div className="mb-7 flex items-center gap-4">
                    <div className="flex h-[60px] w-[60px] shrink-0 items-center justify-center rounded-full bg-soft-stone font-display text-[22px] font-semibold text-deep-green">
                        {initial}
                    </div>
                    <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-2">
                            <span className="truncate text-[19px] font-semibold text-ink">{name}</span>
                            {isSubscribed && (
                                <span className="inline-flex shrink-0 items-center gap-1 rounded-full bg-deep-green px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide text-white">
                                    <Crown size={9} strokeWidth={3} /> PRO
                                </span>
                            )}
                        </div>
                        <div className="truncate text-[14px] text-[#75758a]">{email}</div>
                    </div>
                </div>

                {/* PRO 업그레이드 (미구독자) */}
                {!isSubscribed && (
                    <button
                        onClick={openUpgradePlan}
                        className="group relative mb-7 flex w-full items-center justify-between overflow-hidden rounded-[22px] bg-deep-green px-5 py-[18px] text-left transition-transform active:scale-[0.985]"
                    >
                        <div className="relative z-10">
                            <div className="mb-1.5 flex items-center gap-1.5 text-coral-soft">
                                <Crown size={13} strokeWidth={2.5} />
                                <span className="font-mono text-[11px] uppercase tracking-mono-label">Premium</span>
                            </div>
                            <div className="font-display text-[18px] font-medium tracking-tightest text-white">
                                PRO로 업그레이드
                            </div>
                            <p className="mt-0.5 text-[12px] text-white/70">무제한 북마크 &amp; 프리미엄 기능</p>
                        </div>
                        <ChevronRight
                            size={22}
                            strokeWidth={2}
                            className="relative z-10 text-white/80 transition-transform group-hover:translate-x-1"
                        />
                    </button>
                )}

                {/* 콘텐츠 */}
                <GroupLabel>콘텐츠</GroupLabel>
                <div className="mb-6 overflow-hidden rounded-2xl border border-[#f2f2f2] bg-white">
                    <SettingRow
                        label="팔로잉 블로그"
                        value={sourceCount !== undefined ? `${sourceCount}개` : undefined}
                        onClick={openSourcesManagement}
                    />
                    <SettingRow label="저장한 글" onClick={() => navigate('/saved')} last />
                </div>

                {/* 계정 */}
                <GroupLabel>계정</GroupLabel>
                <div className="mb-7 overflow-hidden rounded-2xl border border-[#f2f2f2] bg-white">
                    <SettingRow label="비밀번호 변경" onClick={openPasswordChange} />
                    <SettingRow label="결제 및 구독" onClick={openPaymentMethod} last />
                </div>

                {/* 로그아웃 / 회원탈퇴 */}
                <button
                    onClick={handleLogout}
                    className="h-[50px] w-full rounded-[32px] border border-[#e5e7eb] bg-white text-[15px] font-medium text-brand-error transition-colors hover:bg-[#fff5f5]"
                >
                    로그아웃
                </button>
                <div className="mt-5 text-center">
                    <button
                        onClick={openWithdraw}
                        className="text-[13px] text-muted underline-offset-2 transition-colors hover:text-brand-error hover:underline"
                    >
                        회원 탈퇴
                    </button>
                </div>
            </div>
        </div>
    );
}
