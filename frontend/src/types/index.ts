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
  SURGE:       { label: '급상승',   emoji: '🔥', color: 'bg-orange-100 text-orange-800 border-orange-200' },
  NEW_ENTRY:   { label: '신규진입', emoji: '🆕', color: 'bg-teal-100 text-teal-800 border-teal-200' },
  HOT_COMMENT: { label: '화제성',   emoji: '💬', color: 'bg-blue-100 text-blue-800 border-blue-200' },
  HIGH_ENGAGE: { label: '고참여율', emoji: '❤️', color: 'bg-pink-100 text-pink-800 border-pink-200' },
  LONG_RUN:    { label: '롱런',     emoji: '📺', color: 'bg-purple-100 text-purple-800 border-purple-200' },
  GLOBAL:      { label: '글로벌',   emoji: '🌍', color: 'bg-sky-100 text-sky-800 border-sky-200' },
  COMEBACK:    { label: '역주행',   emoji: '🔄', color: 'bg-yellow-100 text-yellow-800 border-yellow-200' },
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

// === v2 API 타입 ===

export interface ChannelRanking {
  channelId: string;
  title: string;
  thumbnailUrl: string | null;
  subscriberCount: number;
  totalViewCount: number;
  surgeScore: number;
  grade: 'S' | 'A' | 'B' | 'C' | 'D';
  gradeLabel: string;
  darkhorse: boolean;
  trendingVideoCount: number;
  burstRatio: number;
}

export interface ChannelDetail {
  channelId: string;
  title: string;
  thumbnailUrl: string | null;
  subscriberCount: number;
  videoCount: number;
  totalViewCount: number;
  surgeScore: number;
  grade: string;
  gradeLabel: string;
  darkhorse: boolean;
  firstSeenAt: string;
  aiAnalysis: AiAnalysisResult | null;
}

export interface ChannelSnapshot {
  channelId: string;
  subscriberCount: number;
  videoCount: number;
  totalViewCount: number;
  trendingVideoCount: number;
  snapshotAt: string;
}

export interface AiAnalysisResult {
  analysisType: string;
  targetId: string;
  content: string;
  modelUsed: string;
  createdAt: string;
  fromCache: boolean;
}

export interface KeywordTrend {
  keyword: string;
  videoCount: number;
  totalViews: number;
  avgEngagement: number;
  keywordScore: number;
}

export interface KeywordTimeline {
  keyword: string;
  periodType: string;
  periodStart: string;
  videoCount: number;
  totalViews: number;
}

export interface CrossBorderOpportunity {
  keyword: string;
  trendingCountries: string[];
  targetCountry: string;
  totalVideoCount: number;
  totalViews: number;
}

export interface CrossBorderPropagation {
  keyword: string;
  propagationPath: {
    countryCode: string;
    firstSeenAt: string;
  }[];
}

export interface CrossBorderGlobalLocal {
  globalKeywords: {
    keyword: string;
    countries: string[];
    videoCount: number;
  }[];
  localKeywords: {
    keyword: string;
    countries: string[];
    videoCount: number;
  }[];
}

export const GRADE_CONFIG: Record<string, { color: string; bgColor: string }> = {
  S: { color: 'text-teal-800', bgColor: 'bg-teal-100' },
  A: { color: 'text-emerald-800', bgColor: 'bg-emerald-100' },
  B: { color: 'text-amber-800', bgColor: 'bg-amber-100' },
  C: { color: 'text-slate-600', bgColor: 'bg-slate-100' },
  D: { color: 'text-gray-500', bgColor: 'bg-gray-100' },
};
