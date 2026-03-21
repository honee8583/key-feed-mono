import type { Post, Notification, TrendingKeyword, RecommendedFeed } from '@/types';

export const INITIAL_POSTS: Post[] = [
    {
        id: 1,
        company: "Toss",
        logo: "https://toss.im/favicon.ico",
        title: "Node.js 서비스의 메모리 누수를 찾는 방법",
        excerpt: "대규모 트래픽을 처리하는 환경에서 발생하는 메모리 누수 문제를 어떻게 진단하고 해결했는지 그 과정을 공유합니다.",
        date: "2시간 전",
        category: "Backend",
        tags: ["Node.js", "Performance"],
        color: "bg-blue-400/50",
        readTime: "5 min",
        content: "이 글에서는 Toss의 거대한 트래픽을 견뎌내는 Node.js 서버들이 어떻게 메모리를 관리하고 있는지 상세히 다룹니다...",
        folder: "나중에 읽을 글",
        thumbnail: "https://images.unsplash.com/photo-1555066931-4365d14bab8c?auto=format&fit=crop&w=400&q=80",
        url: "https://toss.tech/article/nodejs-memory-leak"
    },
    {
        id: 2,
        company: "Kakao",
        logo: "https://www.kakaocorp.com/favicon.ico",
        title: "React Server Components가 가져온 변화",
        excerpt: "RSC를 도입하면서 프론트엔드 아키텍처가 어떻게 변했는지, 그리고 우리가 얻은 이점은 무엇인지 알아봅니다.",
        date: "5시간 전",
        category: "Frontend",
        tags: ["React", "Architecture"],
        color: "bg-amber-400/50",
        readTime: "8 min",
        folder: "프로젝트",
        thumbnail: "https://images.unsplash.com/photo-1633356122544-f134324a6cee?auto=format&fit=crop&w=400&q=80",
        url: "https://tech.kakao.com/2023/07/01/react-server-components/"
    },
    {
        id: 3,
        company: "Naver",
        logo: "https://www.naver.com/favicon.ico",
        title: "AI Search: 검색 엔진의 미래를 설계하다",
        excerpt: "LLM을 활용한 검색 기술 고도화 프로젝트의 비하인드 스토리와 기술적 도전을 소개합니다.",
        date: "어제",
        category: "AI",
        tags: ["LLM", "Search"],
        color: "bg-emerald-400/50",
        readTime: "12 min",
        folder: "나중에 읽을 글",
        thumbnail: "https://images.unsplash.com/photo-1677442136019-21780ecad995?auto=format&fit=crop&w=400&q=80",
        url: "https://d2.naver.com/helloworld/123456"
    },
    {
        id: 4,
        company: "Google",
        logo: "https://www.google.com/favicon.ico",
        title: "WebAssembly 성능 극대화 전략",
        excerpt: "복잡한 연산이 필요한 웹 애플리케이션에서 Wasm을 통해 얻을 수 있는 성능 이득을 확인하세요.",
        date: "2일 전",
        category: "Frontend",
        tags: ["Wasm", "Web"],
        color: "bg-rose-400/50",
        readTime: "6 min",
        folder: "프로젝트",
        thumbnail: "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?auto=format&fit=crop&w=400&q=80",
        url: "https://developers.google.com/webassembly"
    }
];

export const MOCK_NOTIFICATIONS: Notification[] = [
    { id: 1, type: 'post', title: 'Toss Tech', body: 'Node.js 성능 최적화의 모든 것 아티클이 올라왔습니다.', time: '방금 전', unread: true },
    { id: 2, type: 'system', title: '시스템', body: '새로운 폴더 "나중에 읽을 글"이 성공적으로 생성되었습니다.', time: '1시간 전', unread: false },
    { id: 3, type: 'trend', title: '트렌드 키워드', body: '실시간 검색어에 "DeepSeek-V3"가 새롭게 진입했습니다.', time: '3시간 전', unread: true },
    { id: 4, type: 'post', title: 'Naver D2', body: 'FE 개발자를 위한 렌더링 패턴 분석 가이드가 발행되었습니다.', time: '어제', unread: false },
];

export const TRENDING_KEYWORDS: TrendingKeyword[] = [
    { rank: 1, keyword: "React Server Components", gap: "up" },
    { rank: 2, keyword: "DeepSeek-V3", gap: "new" },
    { rank: 3, keyword: "WebAssembly", gap: "up" },
    { rank: 4, keyword: "Svelte 5", gap: "down" },
    { rank: 5, keyword: "Edge Computing", gap: "stable" },
];

export const RECOMMENDED_FEEDS: RecommendedFeed[] = [
    { id: 1, name: "Woowahan Tech", desc: "우아한형제들 기술 블로그", subs: "12k", tags: ["MSA", "Java"] },
    { id: 2, name: "Line Engineering", desc: "글로벌 라인의 엔지니어링 리포트", subs: "8.5k", tags: ["Global", "Infra"] },
];
