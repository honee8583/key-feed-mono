export interface Post {
    id: string | number;
    company: string;
    logo: string;
    title: string;
    excerpt: string;
    date: string;
    category: string;
    tags: string[];
    color: string;
    readTime: string;
    content?: string;
    folder?: string | null;
    thumbnail?: string;
    url?: string;
    originalUrl?: string;
    bookmarkId?: number | null;
}

export interface Notification {
    id: number;
    title: string;
    message: string;
    isRead: boolean;
    createdAt: string;
}

export interface FolderConfig {
    name: string;
    icon: string;
    color: string;
}

export interface TrendingKeyword {
    rank: number;
    keyword: string;
    gap: 'up' | 'down' | 'new' | 'stable';
}

export interface RecommendedFeed {
    id: number;
    name: string;
    desc: string;
    subs: string;
    tags: string[];
}
