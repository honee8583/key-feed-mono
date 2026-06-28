import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import gsap from 'gsap';
import { useGSAP } from '@gsap/react';
import { cn } from '@/utils/cn';

const BRAND = 'tracer';

// Slide 0 — 실시간 크롤링/추적
function TrackingVisual() {
    const blogs = [
        { initial: 'T', name: '토스 기술블로그', active: true },
        { initial: '우', name: '우아한형제들', active: false },
        { initial: 'N', name: '네이버 D2', active: false },
    ];
    return (
        <div className="mb-9 rounded-[22px] bg-deep-green p-6 text-white">
            <div className="mb-[18px] font-mono text-[11px] tracking-[0.6px] text-coral-soft">
                TRACKING · 142 BLOGS
            </div>
            <div className="flex flex-col gap-3.5">
                {blogs.map((b) => (
                    <div key={b.name} className="flex items-center gap-2.5">
                        <div className="flex h-[26px] w-[26px] items-center justify-center rounded-[6px] bg-white/[0.12] text-[11px] font-semibold">
                            {b.initial}
                        </div>
                        <span className="flex-1 text-[14px] text-white/90">{b.name}</span>
                        <div
                            className={cn(
                                'h-[7px] w-[7px] rounded-full',
                                b.active ? 'bg-coral' : 'bg-white/25',
                            )}
                        />
                    </div>
                ))}
            </div>
        </div>
    );
}

// Slide 1 — 맞춤 알림
function AlertsVisual() {
    const notifs = [
        { title: '토스 기술블로그 · 새 글', body: '대규모 트래픽을 견디는 분산 락 설계기' },
        { title: '관심 주제 · AI/ML', body: '오늘 12개의 새 글이 올라왔어요' },
    ];
    return (
        <div className="mb-9 rounded-[22px] bg-pale-green p-[22px]">
            <div className="mb-4 font-mono text-[11px] tracking-[0.6px] text-deep-green">
                NOTIFICATIONS
            </div>
            <div className="flex flex-col gap-3">
                {notifs.map((n) => (
                    <div key={n.title} className="flex items-start gap-2.5 rounded-[12px] bg-white p-3.5">
                        <div className="mt-[5px] h-[7px] w-[7px] rounded-full bg-coral" />
                        <div className="flex-1">
                            <div className="mb-[3px] text-[13px] font-semibold text-primary">{n.title}</div>
                            <div className="text-[12px] leading-[1.4] text-[#75758a]">{n.body}</div>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}

// Slide 2 — AI 추천 피드
function FeedVisual() {
    const posts = [
        { source: '네이버 D2 · 어제', title: 'HyperCLOVA X 추론 비용을 40% 줄인 양자화 전략' },
        { source: 'Cloudflare · 2일 전', title: '엣지에서 동작하는 글로벌 레이트 리미터 만들기' },
    ];
    return (
        <div className="mb-9 rounded-[22px] bg-soft-stone p-5">
            <div className="mb-4 flex flex-wrap gap-[7px]">
                <span className="rounded-[32px] bg-coral px-[11px] py-1 text-[12px] font-medium text-white">
                    AI/ML
                </span>
                <span className="rounded-[32px] border border-coral-soft px-[11px] py-1 text-[12px] font-medium text-coral">
                    백엔드
                </span>
                <span className="rounded-[32px] border border-coral-soft px-[11px] py-1 text-[12px] font-medium text-coral">
                    인프라
                </span>
            </div>
            {posts.map((p, i) => (
                <div key={p.title} className={cn('rounded-[12px] bg-white p-3.5', i === 0 && 'mb-2.5')}>
                    <div className="mb-1.5 font-mono text-[10px] tracking-[0.5px] text-muted">{p.source}</div>
                    <div className="text-[14px] font-semibold leading-[1.3] text-primary">{p.title}</div>
                </div>
            ))}
        </div>
    );
}

const SLIDES = [
    {
        eyebrow: '01 / 추적',
        title: ['흩어진 기술 블로그를', '한 곳에서 추적해요'],
        body: '국내외 142개 엔지니어링 블로그를 매시간 크롤링해, 새 글을 놓치지 않게 모아드려요.',
        Visual: TrackingVisual,
    },
    {
        eyebrow: '02 / 알림',
        title: ['당신에게 딱 맞는', '순간에만 알려드려요'],
        body: '팔로우한 블로그와 관심 주제 기준으로, 정말 필요한 알림만 골라 보내드려요.',
        Visual: AlertsVisual,
    },
    {
        eyebrow: '03 / 추천',
        title: ['관심사로 큐레이션된', '오늘의 추천 피드'],
        body: '읽은 글과 저장한 글을 학습해, 매일 아침 당신만의 피드를 새로 짜드려요.',
        Visual: FeedVisual,
    },
] as const;

export function OnboardingCarousel() {
    const navigate = useNavigate();
    const [slide, setSlide] = useState(0);
    const slideRef = useRef<HTMLDivElement>(null);

    const isLast = slide === SLIDES.length - 1;
    const current = SLIDES[slide];

    useGSAP(
        () => {
            gsap.fromTo(
                slideRef.current,
                { opacity: 0, y: 8 },
                { opacity: 1, y: 0, duration: 0.3, ease: 'power2.out' },
            );
        },
        { dependencies: [slide] },
    );

    const handleNext = () => {
        if (isLast) {
            navigate('/auth/signup');
            return;
        }
        setSlide((s) => s + 1);
    };

    return (
        <div className="flex min-h-screen flex-col bg-canvas font-pretendard text-ink">
            {/* top bar */}
            <div className="flex items-center justify-between px-[22px] pt-[60px]">
                <div className="flex items-center gap-[7px]">
                    <span className="h-[15px] w-[15px] rounded-[4px] bg-deep-green" />
                    <span className="font-display text-[19px] font-medium tracking-[-0.3px] text-primary">
                        {BRAND}
                    </span>
                </div>
                <button
                    type="button"
                    onClick={() => navigate('/auth/login')}
                    className="text-[14px] text-[#75758a]"
                >
                    건너뛰기
                </button>
            </div>

            {/* visual + copy */}
            <div className="flex min-h-0 flex-1 flex-col justify-center px-[22px]">
                <div ref={slideRef}>
                    <current.Visual />
                    <div className="mb-3.5 font-mono text-[11px] tracking-[0.6px] text-muted">
                        {current.eyebrow}
                    </div>
                    <h1 className="mb-3.5 font-display text-[32px] font-medium leading-[1.12] tracking-[-0.6px] text-primary">
                        {current.title[0]}
                        <br />
                        {current.title[1]}
                    </h1>
                    <p className="m-0 max-w-[300px] text-[16px] leading-[1.5] text-[#616161]">
                        {current.body}
                    </p>
                </div>
            </div>

            {/* footer: dots + cta */}
            <div className="px-[22px] pb-[46px]">
                <div className="mb-6 flex gap-[7px]">
                    {SLIDES.map((_, i) => (
                        <div
                            key={i}
                            className={cn(
                                'h-[6px] rounded-full transition-all duration-[250ms]',
                                i === slide ? 'w-[22px] bg-primary' : 'w-[6px] bg-hairline',
                            )}
                        />
                    ))}
                </div>
                <button
                    type="button"
                    onClick={handleNext}
                    className="h-[52px] w-full rounded-[32px] bg-primary text-[16px] font-medium text-white transition-opacity active:opacity-85"
                >
                    {isLast ? '시작하기' : '다음'}
                </button>
                <div className="mt-[18px] text-center text-[14px] text-[#75758a]">
                    이미 계정이 있으신가요?{' '}
                    <button
                        type="button"
                        onClick={() => navigate('/auth/login')}
                        className="font-medium text-action-blue"
                    >
                        로그인
                    </button>
                </div>
            </div>
        </div>
    );
}
