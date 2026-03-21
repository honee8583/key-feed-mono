export interface BookmarkContent {
    contentId: string;
    title: string;
    summary: string;
    sourceName: string;
    originalUrl: string;
    thumbnailUrl: string;
    publishedAt: string;
    bookmarkId: number;
}

export interface BookmarkItem {
    bookmarkId: number;
    folderId: number;
    folderName: string;
    createdAt: string;
    content: BookmarkContent;
}

export interface BookmarkResponse {
    content: BookmarkItem[];
    nextCursorId: number | null;
    hasNext: boolean;
}

export interface BookmarkFolder {
    folderId: number;
    name: string;
    icon: string;
    color: string;
}
