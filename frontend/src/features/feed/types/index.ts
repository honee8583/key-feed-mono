export interface FeedItem {
    contentId: string;
    title: string;
    summary: string;
    sourceName: string;
    originalUrl: string;
    thumbnailUrl: string;
    publishedAt: string;
    bookmarkId: number | null;
}

export interface FeedResponse {
    content: FeedItem[];
    hasNext: boolean;
    nextCursorId: number | null;
}
