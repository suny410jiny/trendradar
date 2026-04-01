package com.trendradar.stats.service;

import com.trendradar.trending.domain.TrendingVideo;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 카테고리 + 영상 특성 기반 타겟 연령대 추론 서비스.
 * YouTube API는 시청자 연령 데이터를 제공하지 않으므로,
 * 카테고리·제목 키워드·채널 특성으로 타겟 연령대를 추정한다.
 */
@Service
public class DemographicInferenceService {

    // 카테고리 → 주요 타겟 연령대 매핑
    private static final Map<Integer, List<String>> CATEGORY_AGE_MAP = Map.of(
            10, List.of("10대", "20대"),          // 음악
            17, List.of("20대", "30대", "40대+"),  // 스포츠
            20, List.of("10대", "20대"),           // 게임
            22, List.of("10대", "20대", "30대"),   // 일반/Vlog
            24, List.of("10대", "20대", "30대"),   // 엔터테인먼트
            25, List.of("30대", "40대+"),          // 뉴스/정치
            26, List.of("10대", "20대"),           // 뷰티/패션
            27, List.of("20대", "30대"),           // 교육
            28, List.of("20대", "30대")            // 과학/기술
    );

    // 제목 키워드 → 연령대 가중치 조정
    private static final Map<String, String> KEYWORD_AGE_HINTS = new LinkedHashMap<>();
    static {
        // 10대 키워드
        KEYWORD_AGE_HINTS.put("챌린지", "10대");
        KEYWORD_AGE_HINTS.put("challenge", "10대");
        KEYWORD_AGE_HINTS.put("tiktok", "10대");
        KEYWORD_AGE_HINTS.put("틱톡", "10대");
        KEYWORD_AGE_HINTS.put("shorts", "10대");
        KEYWORD_AGE_HINTS.put("먹방", "10대");
        KEYWORD_AGE_HINTS.put("mukbang", "10대");
        KEYWORD_AGE_HINTS.put("asmr", "10대");
        KEYWORD_AGE_HINTS.put("fancam", "10대");
        KEYWORD_AGE_HINTS.put("직캠", "10대");

        // 20대 키워드
        KEYWORD_AGE_HINTS.put("vlog", "20대");
        KEYWORD_AGE_HINTS.put("브이로그", "20대");
        KEYWORD_AGE_HINTS.put("리뷰", "20대");
        KEYWORD_AGE_HINTS.put("review", "20대");
        KEYWORD_AGE_HINTS.put("unboxing", "20대");
        KEYWORD_AGE_HINTS.put("언박싱", "20대");
        KEYWORD_AGE_HINTS.put("tutorial", "20대");

        // 30대+ 키워드
        KEYWORD_AGE_HINTS.put("투자", "30대");
        KEYWORD_AGE_HINTS.put("부동산", "30대");
        KEYWORD_AGE_HINTS.put("재테크", "30대");
        KEYWORD_AGE_HINTS.put("육아", "30대");
        KEYWORD_AGE_HINTS.put("뉴스", "40대+");
        KEYWORD_AGE_HINTS.put("정치", "40대+");
        KEYWORD_AGE_HINTS.put("시사", "40대+");
    }

    private static final List<String> AGE_GROUPS = List.of("10대", "20대", "30대", "40대+");
    private static final List<String> DEFAULT_AGES = List.of("20대", "30대");

    /**
     * 단일 영상의 추정 타겟 연령대 반환
     */
    public String inferPrimaryDemographic(TrendingVideo video) {
        Map<String, Double> scores = calculateAgeScores(video);
        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("20대");
    }

    /**
     * 단일 영상의 타겟 연령대 목록 반환 (복수)
     */
    public List<String> inferDemographics(TrendingVideo video) {
        Map<String, Double> scores = calculateAgeScores(video);
        if (scores.isEmpty()) {
            return DEFAULT_AGES;
        }

        double maxScore = scores.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double threshold = maxScore * 0.5;

        return AGE_GROUPS.stream()
                .filter(age -> scores.getOrDefault(age, 0.0) >= threshold)
                .toList();
    }

    /**
     * 영상 목록에서 연령대별 분포 계산
     */
    public Map<String, List<TrendingVideo>> groupByDemographic(List<TrendingVideo> videos) {
        Map<String, List<TrendingVideo>> result = new LinkedHashMap<>();
        for (String age : AGE_GROUPS) {
            result.put(age, new ArrayList<>());
        }

        for (TrendingVideo video : videos) {
            List<String> ages = inferDemographics(video);
            for (String age : ages) {
                result.computeIfAbsent(age, k -> new ArrayList<>()).add(video);
            }
        }

        return result;
    }

    private Map<String, Double> calculateAgeScores(TrendingVideo video) {
        Map<String, Double> scores = new HashMap<>();

        // 1. 카테고리 기반 점수 (가중치 2.0)
        Integer categoryId = video.getCategoryId();
        if (categoryId != null) {
            List<String> categoryAges = CATEGORY_AGE_MAP.getOrDefault(categoryId, DEFAULT_AGES);
            for (String age : categoryAges) {
                scores.merge(age, 2.0, Double::sum);
            }
        }

        // 2. 제목 키워드 기반 점수 (가중치 1.5)
        String title = video.getTitle();
        if (title != null) {
            String lowerTitle = title.toLowerCase();
            for (Map.Entry<String, String> entry : KEYWORD_AGE_HINTS.entrySet()) {
                if (lowerTitle.contains(entry.getKey().toLowerCase())) {
                    scores.merge(entry.getValue(), 1.5, Double::sum);
                }
            }
        }

        // 3. 참여율 기반 보정 (높은 좋아요율 → 젊은 층)
        if (video.getViewCount() != null && video.getViewCount() > 0 && video.getLikeCount() != null) {
            double likeRatio = (double) video.getLikeCount() / video.getViewCount();
            if (likeRatio > 0.05) {  // 좋아요율 5% 이상 → 10~20대 가중
                scores.merge("10대", 0.5, Double::sum);
                scores.merge("20대", 0.5, Double::sum);
            }
        }

        // 4. 댓글 비율 기반 보정 (높은 댓글율 → 젊은 층)
        if (video.getViewCount() != null && video.getViewCount() > 0 && video.getCommentCount() != null) {
            double commentRatio = (double) video.getCommentCount() / video.getViewCount();
            if (commentRatio > 0.005) {
                scores.merge("10대", 0.3, Double::sum);
                scores.merge("20대", 0.3, Double::sum);
            }
        }

        return scores;
    }
}
