export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string | null;
}

export interface TrendingVideo {
  videoId: string;
  title: string;
  channelTitle: string;
  countryCode: string;
  categoryId: number | null;
  categoryName: string;
  rankPosition: number;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  publishedAt: string;
  thumbnailUrl: string;
  duration: string;
  collectedAt: string;
  tags: string[];
}

export interface Country {
  code: string;
  nameKo: string;
  nameEn: string;
  region: string;
}

export interface Category {
  id: number;
  name: string;
}

export interface ViewSnapshot {
  videoId: string;
  viewCount: number;
  snapshotAt: string;
}

export interface BriefingResponse {
  country: string;
  countryName: string;
  summary: string;
  topVideos: TrendingVideo[];
  generatedAt: string;
}
