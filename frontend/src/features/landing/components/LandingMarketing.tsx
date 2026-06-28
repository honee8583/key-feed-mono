import { useNavigate } from 'react-router-dom';

const BRAND = 'tracer';

const FEATURES = [
    {
        no: '01',
        title: '실시간 크롤링',
        body: '142개 엔지니어링 블로그를 5분마다 수집해, 새 글이 올라오는 즉시 모아드려요.',
    },
    {
        no: '02',
        title: '맞춤 알림',
        body: '팔로우한 블로그와 관심 주제 기준으로, 정말 필요한 알림만 적절한 순간에 보내드려요.',
    },
    {
        no: '03',
        title: 'AI 추천 피드',
        body: '읽은 글과 저장한 글을 학습해, 매일 아침 당신만을 위한 피드를 새로 구성합니다.',
    },
];

const RECENT = [
    { source: '토스 기술블로그', title: '대규모 트래픽을 견디는 분산 락 설계기', cat: '백엔드', time: '방금 전' },
    { source: '네이버 D2', title: 'HyperCLOVA X 추론 비용을 40% 줄인 양자화 전략', cat: 'AI/ML', time: '1시간 전' },
    { source: 'Cloudflare', title: '엣지에서 동작하는 글로벌 레이트 리미터 만들기', cat: '인프라', time: '2일 전' },
    { source: '카카오', title: '모노레포 빌드 시간을 1/5로 줄이기', cat: '빌드', time: '3일 전' },
    { source: '우아한형제들', title: '실시간 추천을 위한 피처 스토어 구축기', cat: '데이터', time: '4일 전' },
    { source: 'AWS 한국 블로그', title: 'LLM 서빙 비용을 90% 절감한 방법', cat: 'AI/ML', time: '5일 전' },
];

function Wordmark({ size = 21 }: { size?: number }) {
    return (
        <div className="flex items-center gap-2">
            <span className="h-4 w-4 rounded-[5px] bg-deep-green" />
            <span
                className="font-display font-medium tracking-[-0.4px] text-primary"
                style={{ fontSize: size }}
            >
                {BRAND}
            </span>
        </div>
    );
}

export function LandingMarketing() {
    const navigate = useNavigate();
    const goLogin = () => navigate('/auth/login');
    const goSignup = () => navigate('/auth/signup');

    return (
        <div className="min-h-screen bg-canvas font-pretendard text-ink">
            {/* ════ NAV ════ */}
            <div className="border-b border-[#f2f2f2]">
                <div className="mx-auto flex h-[68px] max-w-[1080px] items-center justify-between px-[clamp(20px,5vw,40px)]">
                    <button type="button" onClick={() => navigate('/')} aria-label={BRAND}>
                        <Wordmark />
                    </button>
                    <div className="flex items-center gap-8">
                        <button
                            type="button"
                            onClick={goLogin}
                            className="text-[15px] text-[#616161] transition-colors hover:text-primary"
                        >
                            로그인
                        </button>
                        <button
                            type="button"
                            onClick={goSignup}
                            className="border-b border-primary pb-px text-[15px] text-primary"
                        >
                            시작하기
                        </button>
                    </div>
                </div>
            </div>

            {/* ════ HERO ════ */}
            <div className="mx-auto max-w-[880px] px-[clamp(20px,5vw,40px)] pb-[clamp(48px,8vw,88px)] pt-[clamp(64px,12vw,140px)] text-center">
                <div className="mb-[26px] font-mono text-[13px] uppercase tracking-[0.8px] text-[#75758a]">
                    기술 블로그 인텔리전스
                </div>
                <h1 className="mx-0 mb-7 font-display text-[clamp(40px,7.5vw,84px)] font-medium leading-[1.02] tracking-tightest text-primary [text-wrap:balance]">
                    흩어진 기술 블로그를,
                    <br />
                    당신의 피드 하나로.
                </h1>
                <p className="mx-auto mb-[38px] max-w-[540px] text-[clamp(16px,2vw,19px)] leading-[1.6] text-[#616161]">
                    국내외 142개 엔지니어링 블로그를 실시간으로 추적하고, 관심사에 맞춰 큐레이션된
                    피드와 알림을 받아보세요.
                </p>
                <button
                    type="button"
                    onClick={goSignup}
                    className="border-b border-primary pb-[3px] text-[17px] text-primary transition-colors hover:text-[#75758a]"
                >
                    무료로 시작하기 →
                </button>
            </div>

            {/* ════ PRODUCT PREVIEW ════ */}
            <div className="mx-auto max-w-[780px] px-[clamp(20px,5vw,40px)] pb-[clamp(56px,9vw,104px)]">
                <div className="overflow-hidden rounded-[22px] border border-[#e5e7eb] bg-white">
                    <div className="flex items-center gap-2 border-b border-[#e5e7eb] bg-soft-stone px-4 py-3">
                        <div className="flex gap-1.5">
                            <div className="h-[11px] w-[11px] rounded-full bg-hairline" />
                            <div className="h-[11px] w-[11px] rounded-full bg-hairline" />
                            <div className="h-[11px] w-[11px] rounded-full bg-hairline" />
                        </div>
                        <div className="flex flex-1 justify-center">
                            <div className="rounded-[6px] bg-white px-4 py-1 font-mono text-[11px] text-[#75758a]">
                                app.tracer.io/feed
                            </div>
                        </div>
                    </div>
                    <div className="p-[clamp(20px,4vw,32px)]">
                        <div className="mb-4 font-mono text-[11px] tracking-[0.6px] text-muted">
                            오늘의 피드 · 8개의 새 글
                        </div>
                        <div className="mb-[18px] overflow-hidden rounded-[16px] border border-[#f2f2f2]">
                            <div className="bg-deep-green px-[22px] py-5">
                                <div className="mb-[30px] flex items-center justify-between">
                                    <span className="font-mono text-[11px] tracking-[0.5px] text-coral-soft">
                                        토스 기술블로그
                                    </span>
                                    <span className="rounded-[32px] border border-white/30 px-2.5 py-[3px] text-[11px] text-white">
                                        백엔드
                                    </span>
                                </div>
                                <div className="font-display text-[clamp(18px,2.4vw,22px)] font-medium leading-[1.2] text-white">
                                    대규모 트래픽을 견디는 분산 락 설계기
                                </div>
                            </div>
                        </div>
                        <div className="border-b border-[#f2f2f2] py-[15px]">
                            <div className="mb-1.5 font-mono text-[11px] text-[#75758a]">네이버 D2 · 어제</div>
                            <div className="text-[16px] font-semibold text-primary">
                                HyperCLOVA X 추론 비용을 40% 줄인 양자화 전략
                            </div>
                        </div>
                        <div className="py-[15px]">
                            <div className="mb-1.5 font-mono text-[11px] text-[#75758a]">Cloudflare · 2일 전</div>
                            <div className="text-[16px] font-semibold text-primary">
                                엣지에서 동작하는 글로벌 레이트 리미터 만들기
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* ════ WHAT IT DOES ════ */}
            <div className="mx-auto max-w-[1080px] px-[clamp(20px,5vw,40px)] pb-[clamp(56px,9vw,104px)]">
                <div className="grid grid-cols-1 gap-[clamp(28px,4vw,56px)] sm:grid-cols-3">
                    {FEATURES.map((f) => (
                        <div key={f.no} className="border-t border-primary pt-[22px]">
                            <div className="mb-3.5 font-mono text-[12px] text-[#75758a]">{f.no}</div>
                            <h3 className="mb-2.5 text-[21px] font-semibold tracking-[-0.2px] text-primary">
                                {f.title}
                            </h3>
                            <p className="m-0 text-[15px] leading-[1.6] text-[#616161]">{f.body}</p>
                        </div>
                    ))}
                </div>
            </div>

            {/* ════ RECENT LIST ════ */}
            <div className="mx-auto max-w-[1080px] px-[clamp(20px,5vw,40px)] pb-[clamp(56px,9vw,104px)]">
                <div className="mb-[clamp(20px,3vw,32px)] flex flex-wrap items-baseline justify-between gap-3">
                    <h2 className="m-0 font-display text-[clamp(24px,3.4vw,38px)] font-medium tracking-[-0.02em] text-primary">
                        지금 이 순간의 피드
                    </h2>
                    <button
                        type="button"
                        onClick={goSignup}
                        className="whitespace-nowrap text-[15px] text-action-blue"
                    >
                        전체 보기 →
                    </button>
                </div>
                <div>
                    {RECENT.map((r) => (
                        <div
                            key={r.title}
                            className="flex flex-wrap items-center gap-[clamp(12px,3vw,28px)] border-b border-[#e5e7eb] py-[clamp(16px,2.2vw,22px)] transition-opacity hover:opacity-60"
                        >
                            <div className="min-w-[200px] flex-1 basis-[260px]">
                                <div className="mb-1.5 font-mono text-[11px] tracking-[0.4px] text-[#75758a]">
                                    {r.source}
                                </div>
                                <div className="text-[clamp(16px,1.9vw,20px)] font-semibold leading-[1.3] tracking-[-0.2px] text-primary">
                                    {r.title}
                                </div>
                            </div>
                            <span className="whitespace-nowrap rounded-[30px] border border-coral-soft px-3 py-[5px] text-[12px] text-coral">
                                {r.cat}
                            </span>
                            <span className="w-[76px] whitespace-nowrap text-right text-[14px] text-muted">
                                {r.time}
                            </span>
                        </div>
                    ))}
                </div>
            </div>

            {/* ════ FOOTER ════ */}
            <div className="border-t border-[#f2f2f2]">
                <div className="mx-auto flex max-w-[1080px] flex-wrap items-center justify-between gap-5 px-[clamp(20px,5vw,40px)] py-[clamp(40px,5vw,56px)]">
                    <div className="flex items-center gap-2">
                        <Wordmark size={18} />
                        <span className="ml-2.5 text-[13px] text-muted">© 2026</span>
                    </div>
                </div>
            </div>
        </div>
    );
}
