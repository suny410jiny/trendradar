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
  // 순위 변동
  previousRank: number | null;
  rankChange: number | null;
  rankChangeType: 'UP' | 'DOWN' | 'SAME' | 'NEW' | null;
  // 타겟 연령대
  targetDemographic: string | null;
  // YouTube 원본 태그
  youtubeTags: string[];
  // Shorts 여부
  isShort: boolean;
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

export type AlgorithmTag =
  | 'SURGE'
  | 'NEW_ENTRY'
  | 'HOT_COMMENT'
  | 'HIGH_ENGAGE'
  | 'LONG_RUN'
  | 'GLOBAL'
  | 'COMEBACK';

export const TAG_CONFIG: Record<AlgorithmTag, { label: string; emoji: string; color: string }> = {
  SURGE:       { label: '급상승',   emoji: '🔥', color: 'bg-red-500/10 text-red-600 border-red-500/20' },
  NEW_ENTRY:   { label: '신규진입', emoji: '🆕', color: 'bg-green-500/10 text-green-600 border-green-500/20' },
  HOT_COMMENT: { label: '화제성',   emoji: '💬', color: 'bg-blue-500/10 text-blue-600 border-blue-500/20' },
  HIGH_ENGAGE: { label: '고참여율', emoji: '❤️', color: 'bg-pink-500/10 text-pink-600 border-pink-500/20' },
  LONG_RUN:    { label: '롱런',     emoji: '📺', color: 'bg-purple-500/10 text-purple-600 border-purple-500/20' },
  GLOBAL:      { label: '글로벌',   emoji: '🌍', color: 'bg-sky-500/10 text-sky-600 border-sky-500/20' },
  COMEBACK:    { label: '역주행',   emoji: '🔄', color: 'bg-yellow-500/10 text-yellow-600 border-yellow-500/20' },
};

export const COUNTRY_FLAGS: Record<string, string> = {
  KR: '🇰🇷', US: '🇺🇸', JP: '🇯🇵', GB: '🇬🇧', DE: '🇩🇪',
};

export interface BriefingResponse {
  country: string;
  countryName: string;
  summary: string;
  topVideos: TrendingVideo[];
  generatedAt: string;
}

// === Stats API 타입 ===

export interface StatsOverview {
  countryCode: string;
  countryName: string;
  totalVideos: number;
  newEntryCount: number;
  surgeCount: number;
  avgEngagementRate: number;
  totalViews: number;
  uniqueVideos24h: number;
  categoryDistribution: CategoryStat[];
  tagDistribution: TagStat[];
  demographicDistribution: DemographicStat[];
  generatedAt: string;
}

export interface CategoryStat {
  categoryId: number;
  categoryName: string;
  videoCount: number;
  totalViews: number;
  avgViews: number;
  percentage: number;
}

export interface TagStat {
  tagType: string;
  tagLabel: string;
  videoCount: number;
  percentage: number;
}

export interface DemographicStat {
  ageGroup: string;
  videoCount: number;
  percentage: number;
  topCategories: string[];
}

export interface TrendingKeyword {
  keyword: string;
  count: number;
  percentage: number;
}
