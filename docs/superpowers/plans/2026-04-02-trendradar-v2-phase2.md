# TrendRadar v2 Phase 2: 분석 엔진 구축 — 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 채널 급부상 랭킹(5지표 스코어 + S~D 등급), 크로스보더 분석(3뷰), AI 분석(4타입 캐싱), 키워드 배치 집계, v2 API 컨트롤러를 구축한다.

**Architecture:** Phase 1에서 구축한 channel/keyword/ai_analyses 테이블 위에 분석 서비스를 구현. 각 서비스는 독립적으로 테스트 가능. 스케줄러에 배치 집계와 AI 분석 스텝을 추가. v2 API는 `/api/v2/` 경로로 분리.

**Tech Stack:** Spring Boot 3.5.0, Java 21, PostgreSQL 16, Claude API (Haiku/Sonnet), Mockito, AssertJ

**Spec:** `docs/superpowers/specs/2026-04-02-trendradar-v2-design.md` (섹션 4~7)
**Depends on:** Phase 1 완료 (`feature/v2-phase1` 브랜치)

---

## File Map

### 신규 파일

```
backend/src/main/java/com/trendradar/channel/
  service/ChannelRankingService.java       -- 5지표 surge_score 계산 + 등급
  dto/ChannelRankingResponse.java          -- 랭킹 응답 DTO
  dto/ChannelDetailResponse.java           -- 채널 상세 응답 DTO
  controller/ChannelController.java        -- /api/v2/channels/** 엔드포인트

backend/src/main/java/com/trendradar/crossborder/
  service/CrossBorderService.java          -- 3가지 크로스보더 뷰
  dto/OpportunityResponse.java             -- 선점 기회 DTO
  dto/PropagationResponse.java             -- 전파 경로 DTO
  dto/GlobalLocalResponse.java             -- 글로벌 vs 로컬 DTO
  controller/CrossBorderController.java    -- /api/v2/crossborder/** 엔드포인트

backend/src/main/java/com/trendradar/ai/
  domain/AiAnalysis.java                   -- ai_analyses 엔티티
  domain/AnalysisType.java                 -- enum (BRIEFING/VIDEO/CHANNEL/PREDICTION)
  repository/AiAnalysisRepository.java     -- DB 캐시 조회
  service/AiAnalysisService.java           -- 4타입 AI 분석 + 캐싱
  dto/AiAnalysisResponse.java              -- AI 분석 응답 DTO

backend/src/main/java/com/trendradar/keyword/
  controller/KeywordController.java        -- /api/v2/keywords/** 엔드포인트
  dto/KeywordTrendResponse.java            -- 키워드 트렌드 응답 DTO
  dto/KeywordTimelineResponse.java         -- 키워드 타임라인 DTO

backend/src/main/java/com/trendradar/scheduler/
  KeywordAggregationScheduler.java         -- 일/주/월 배치 집계
  AiAnalysisScheduler.java                 -- 브리핑 1h + 예측 6h

backend/src/test/java/com/trendradar/channel/
  service/ChannelRankingServiceTest.java
backend/src/test/java/com/trendradar/crossborder/
  service/CrossBorderServiceTest.java
backend/src/test/java/com/trendradar/ai/
  service/AiAnalysisServiceTest.java
backend/src/test/java/com/trendradar/keyword/
  service/KeywordAggregationTest.java
```

### 수정 파일

```
backend/src/main/java/com/trendradar/youtube/client/ClaudeApiClient.java  -- 모델 선택 파라미터 추가
backend/src/main/java/com/trendradar/keyword/service/KeywordTrendService.java  -- 배치 집계 메서드 추가
```

---

## Task 1: ChannelRankingService (TDD) — 5지표 스코어 + 등급

**Files:**
- Create: `backend/src/test/java/com/trendradar/channel/service/ChannelRankingServiceTest.java`
- Create: `backend/src/main/java/com/trendradar/channel/service/ChannelRankingService.java`
- Create: `backend/src/main/java/com/trendradar/channel/dto/ChannelRankingResponse.java`

### ChannelRankingResponse DTO

```java
package com.trendradar.channel.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChannelRankingResponse {
    private final String channelId;
    private final String title;
    private final String thumbnailUrl;
    private final Long subscriberCount;
    private final Long totalViewCount;
    private final double surgeScore;
    private final String grade;          // S, A, B, C, D
    private final String gradeLabel;     // "지금 가장 핫한 채널!" 등
    private final boolean darkhorse;     // burst_ratio >= 10x
    private final int trendingVideoCount;
    private final double burstRatio;
}
```

### ChannelRankingService 핵심 로직

```java
// 5가지 지표 계산
// 1. trendingFrequency: 24h 내 해당 채널의 고유 영상 수
// 2. rankScore: AVG(51 - rank_position) / 50
// 3. growthRate: 채널 영상들의 24h 조회수 성장률 평균
// 4. engagement: AVG((like + comment) / view)
// 5. burstRatio: AVG(video_view / subscriber_count)

// surge_score = 0.25*freq + 0.20*rank + 0.20*growth + 0.15*engage + 0.20*burst
// min-max 정규화 후 가중합

// 등급: S(>=0.9), A(>=0.75), B(>=0.55), C(>=0.35), D(<0.35)
// 다크호스: burstRatio >= 10.0
```

### 테스트 케이스 (4건)

1. `calculateRanking_withMultipleChannels_returnsSortedByScore` — 여러 채널 → surge_score DESC 정렬
2. `calculateRanking_assignsCorrectGrades` — S/A/B/C/D 등급 정확성
3. `calculateRanking_detectsDarkhorse` — burst_ratio >= 10x → darkhorse=true
4. `calculateRanking_withNoData_returnsEmptyList` — 데이터 없을 때 빈 리스트

### 필요한 Repository 쿼리 (TrendingVideoRepository에 추가)

```java
// 24시간 내 채널별 트렌딩 영상 목록
@Query("SELECT tv FROM TrendingVideo tv WHERE tv.channelId IN :channelIds " +
       "AND tv.collectedAt BETWEEN :from AND :to")
List<TrendingVideo> findByChannelIdInAndCollectedAtBetween(
        @Param("channelIds") List<String> channelIds,
        @Param("from") OffsetDateTime from,
        @Param("to") OffsetDateTime to);

// 최근 수집에서 고유 채널 ID 목록
@Query(value = "SELECT DISTINCT tv.channel_id FROM trending_videos tv " +
       "WHERE tv.country_code = :countryCode AND tv.collected_at BETWEEN :from AND :to " +
       "AND tv.channel_id IS NOT NULL",
       nativeQuery = true)
List<String> findDistinctChannelIdsByCountryCode(
        @Param("countryCode") String countryCode,
        @Param("from") OffsetDateTime from,
        @Param("to") OffsetDateTime to);
```

- [ ] Step 1: ChannelRankingResponse DTO 작성
- [ ] Step 2: TrendingVideoRepository에 쿼리 2개 추가
- [ ] Step 3: 실패하는 테스트 4건 작성
- [ ] Step 4: 테스트 실행 — 실패 확인
- [ ] Step 5: ChannelRankingService 구현 (5지표 + 정규화 + 등급 + 다크호스)
- [ ] Step 6: 테스트 실행 — 통과 확인
- [ ] Step 7: 커밋

---

## Task 2: CrossBorderService (TDD) — 3가지 크로스보더 뷰

**Files:**
- Create: `backend/src/main/java/com/trendradar/crossborder/dto/OpportunityResponse.java`
- Create: `backend/src/main/java/com/trendradar/crossborder/dto/PropagationResponse.java`
- Create: `backend/src/main/java/com/trendradar/crossborder/dto/GlobalLocalResponse.java`
- Create: `backend/src/test/java/com/trendradar/crossborder/service/CrossBorderServiceTest.java`
- Create: `backend/src/main/java/com/trendradar/crossborder/service/CrossBorderService.java`

### DTOs

```java
// OpportunityResponse — 선점 기회
@Getter @Builder
public class OpportunityResponse {
    private final String keyword;
    private final List<String> trendingCountries;  // 해당 키워드가 뜨는 나라들
    private final String targetCountry;            // 아직 안 뜨는 내 나라
    private final int totalVideoCount;             // 다른 나라 합산 영상 수
    private final long totalViews;
}

// PropagationResponse — 전파 경로
@Getter @Builder
public class PropagationResponse {
    private final String keyword;
    private final List<PropagationStep> propagationPath;

    @Getter @Builder
    public static class PropagationStep {
        private final String countryCode;
        private final OffsetDateTime firstSeenAt;
    }
}

// GlobalLocalResponse — 글로벌 vs 로컬
@Getter @Builder
public class GlobalLocalResponse {
    private final List<KeywordInfo> globalKeywords;  // 3개국+ 등장
    private final List<KeywordInfo> localKeywords;   // 1개국만

    @Getter @Builder
    public static class KeywordInfo {
        private final String keyword;
        private final List<String> countries;
        private final int videoCount;
    }
}
```

### CrossBorderService 핵심 로직

```java
// 뷰 1: findOpportunities(countryCode)
//   keyword_trends에서 최근 DAY 기준, 다른 나라에는 있지만 내 나라에 없는 키워드
//   KeywordTrendRepository에 추가 쿼리 필요

// 뷰 2: findPropagation(keyword)
//   keyword_trends에서 해당 키워드의 나라별 최초 등장일 → 시간순 정렬

// 뷰 3: findGlobalVsLocal(countryCode)
//   keyword_trends에서 같은 키워드의 country_code 수로 글로벌/로컬 분류
```

### 필요한 Repository 쿼리 (KeywordTrendRepository에 추가)

```java
// 특정 기간의 고유 키워드 목록 (나라별)
List<KeywordTrend> findByCountryCodeAndPeriodTypeAndPeriodStartAfterOrderByVideoCountDesc(
        String countryCode, PeriodType periodType, OffsetDateTime after);

// 특정 키워드의 나라별 최초 등장일
@Query("SELECT kt FROM KeywordTrend kt WHERE kt.keyword = :keyword " +
       "AND kt.periodType = :periodType ORDER BY kt.periodStart ASC")
List<KeywordTrend> findFirstAppearancesByKeyword(
        @Param("keyword") String keyword, @Param("periodType") PeriodType periodType);
```

### 테스트 케이스 (3건)

1. `findOpportunities_returnsKeywordsNotInMyCountry` — 뷰 1 검증
2. `findPropagation_returnsCountriesSortedByFirstSeen` — 뷰 2 검증
3. `findGlobalVsLocal_classifiesCorrectly` — 뷰 3 검증

- [ ] Step 1: DTOs 3개 작성
- [ ] Step 2: KeywordTrendRepository에 쿼리 추가
- [ ] Step 3: 실패하는 테스트 3건 작성
- [ ] Step 4: 테스트 실행 — 실패 확인
- [ ] Step 5: CrossBorderService 구현 (3뷰)
- [ ] Step 6: 테스트 실행 — 통과 확인
- [ ] Step 7: 커밋

---

## Task 3: AI Analysis 도메인 + Service (TDD) — 4타입 캐싱

**Files:**
- Create: `backend/src/main/java/com/trendradar/ai/domain/AnalysisType.java`
- Create: `backend/src/main/java/com/trendradar/ai/domain/AiAnalysis.java`
- Create: `backend/src/main/java/com/trendradar/ai/repository/AiAnalysisRepository.java`
- Create: `backend/src/main/java/com/trendradar/ai/dto/AiAnalysisResponse.java`
- Create: `backend/src/test/java/com/trendradar/ai/service/AiAnalysisServiceTest.java`
- Create: `backend/src/main/java/com/trendradar/ai/service/AiAnalysisService.java`
- Modify: `backend/src/main/java/com/trendradar/youtube/client/ClaudeApiClient.java`

### AnalysisType enum

```java
package com.trendradar.ai.domain;
public enum AnalysisType {
    BRIEFING, VIDEO, CHANNEL, PREDICTION
}
```

### AiAnalysis 엔티티

```java
// ai_analyses 테이블 매핑
// 필드: id, analysisType(enum STRING), targetId, countryCode, content(TEXT), modelUsed, createdAt, expiresAt
// 캐시 확인: expiresAt > now()인 가장 최근 레코드
```

### AiAnalysisRepository

```java
// 캐시 히트: 만료 안 된 가장 최근 분석 조회
Optional<AiAnalysis> findTopByAnalysisTypeAndTargetIdAndExpiresAtAfterOrderByCreatedAtDesc(
        AnalysisType analysisType, String targetId, OffsetDateTime now);
```

### ClaudeApiClient 수정 — 모델 선택 파라미터 추가

```java
// 기존 generateBriefing(String prompt) 유지
// 새 메서드 추가:
public String generate(String prompt, String model, int maxTokens) {
    // model: "claude-haiku-4-5-20251001" 또는 "claude-sonnet-4-20250514"
    // 기존 generateBriefing 로직과 동일, model 파라미터만 변경
}
```

### AiAnalysisService 핵심 로직

```java
// 4가지 분석 메서드:
// 1. generateBriefing(countryCode) — Haiku, TTL 1h
//    입력: TOP 10 영상 + 키워드 TOP 10
//    프롬프트: 긍정적 톤, 3줄 요약

// 2. analyzeVideo(videoId) — Haiku, TTL 24h
//    입력: 영상 메타데이터 + 태그 + 순위
//    프롬프트: "이 영상이 뜬 이유" 분석

// 3. analyzeChannel(channelId) — Sonnet, TTL 6h
//    입력: 채널 정보 + surge_score + 등급
//    프롬프트: 채널 성장 포인트 + 한줄평

// 4. generatePrediction(countryCode) — Sonnet, TTL 6h
//    입력: 키워드 추이 + 크로스보더 데이터
//    프롬프트: 주목 키워드/채널 예측

// 공통: DB 캐시 확인 → 히트면 반환, 미스면 Claude API 호출 → 결과 저장
```

### 테스트 케이스 (4건)

1. `generateBriefing_whenCacheHit_returnsFromDb` — 캐시 히트 → API 호출 안 함
2. `generateBriefing_whenCacheMiss_callsClaudeAndSaves` — 캐시 미스 → API 호출 + DB 저장
3. `analyzeVideo_usesHaikuModel` — Haiku 모델 사용 확인
4. `analyzeChannel_usesSonnetModel` — Sonnet 모델 사용 확인

- [ ] Step 1: AnalysisType enum, AiAnalysis 엔티티, Repository 작성
- [ ] Step 2: AiAnalysisResponse DTO 작성
- [ ] Step 3: ClaudeApiClient에 generate(prompt, model, maxTokens) 추가
- [ ] Step 4: 실패하는 테스트 4건 작성
- [ ] Step 5: 테스트 실행 — 실패 확인
- [ ] Step 6: AiAnalysisService 구현 (4타입 + 캐싱 + 프롬프트)
- [ ] Step 7: 테스트 실행 — 통과 확인
- [ ] Step 8: 커밋

---

## Task 4: KeywordAggregationScheduler — 일/주/월 배치

**Files:**
- Modify: `backend/src/main/java/com/trendradar/keyword/service/KeywordTrendService.java` — 배치 집계 메서드 추가
- Create: `backend/src/main/java/com/trendradar/scheduler/KeywordAggregationScheduler.java`
- Create: `backend/src/test/java/com/trendradar/keyword/service/KeywordAggregationTest.java`

### KeywordTrendService 추가 메서드

```java
// aggregateDailyKeywords(String countryCode, LocalDate date)
//   HOUR 레코드를 DAY로 합산 (같은 keyword+country → videoCount 합산, views 합산, engagement 평균)
// aggregateWeeklyKeywords(String countryCode, LocalDate weekStart)
//   DAY 레코드를 WEEK로 합산
// aggregateMonthlyKeywords(String countryCode, YearMonth month)
//   DAY 레코드를 MONTH로 합산
```

### KeywordAggregationScheduler

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class KeywordAggregationScheduler {
    private static final List<String> TARGET_COUNTRIES = List.of("KR", "US", "JP", "GB", "DE");
    private final KeywordTrendService keywordTrendService;

    @Scheduled(cron = "0 5 0 * * *")  // 매일 00:05
    public void aggregateDaily() { /* 어제 날짜 기준 DAY 집계 */ }

    @Scheduled(cron = "0 10 0 * * MON")  // 매주 월요일 00:10
    public void aggregateWeekly() { /* 지난주 기준 WEEK 집계 */ }

    @Scheduled(cron = "0 15 0 1 * *")  // 매월 1일 00:15
    public void aggregateMonthly() { /* 지난달 기준 MONTH 집계 */ }
}
```

### 테스트 케이스 (2건)

1. `aggregateDailyKeywords_sumsHourlyRecords` — HOUR → DAY 합산 정확성
2. `aggregateWeeklyKeywords_sumsDailyRecords` — DAY → WEEK 합산 정확성

- [ ] Step 1: 실패하는 테스트 2건 작성
- [ ] Step 2: 테스트 실행 — 실패 확인
- [ ] Step 3: KeywordTrendService에 배치 집계 메서드 3개 추가
- [ ] Step 4: KeywordAggregationScheduler 작성
- [ ] Step 5: 테스트 실행 — 통과 확인
- [ ] Step 6: 커밋

---

## Task 5: AI 분석 스케줄러

**Files:**
- Create: `backend/src/main/java/com/trendradar/scheduler/AiAnalysisScheduler.java`

### AiAnalysisScheduler

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class AiAnalysisScheduler {
    private static final List<String> TARGET_COUNTRIES = List.of("KR", "US", "JP", "GB", "DE");
    private final AiAnalysisService aiAnalysisService;

    @Scheduled(cron = "0 30 * * * *")  // 매 시간 30분 (수집 후 30분)
    public void generateBriefings() {
        for (String country : TARGET_COUNTRIES) {
            try {
                aiAnalysisService.generateBriefing(country);
                log.info("Generated briefing for country={}", country);
            } catch (Exception e) {
                log.error("Failed to generate briefing for country={}", country, e);
            }
        }
    }

    @Scheduled(cron = "0 45 */6 * * *")  // 6시간마다 45분
    public void generatePredictions() {
        for (String country : TARGET_COUNTRIES) {
            try {
                aiAnalysisService.generatePrediction(country);
                log.info("Generated prediction for country={}", country);
            } catch (Exception e) {
                log.error("Failed to generate prediction for country={}", country, e);
            }
        }
    }
}
```

- [ ] Step 1: AiAnalysisScheduler 작성
- [ ] Step 2: 컴파일 확인
- [ ] Step 3: 커밋

---

## Task 6: v2 API 컨트롤러 (channels, keywords, crossborder)

**Files:**
- Create: `backend/src/main/java/com/trendradar/channel/controller/ChannelController.java`
- Create: `backend/src/main/java/com/trendradar/channel/dto/ChannelDetailResponse.java`
- Create: `backend/src/main/java/com/trendradar/keyword/controller/KeywordController.java`
- Create: `backend/src/main/java/com/trendradar/keyword/dto/KeywordTrendResponse.java`
- Create: `backend/src/main/java/com/trendradar/keyword/dto/KeywordTimelineResponse.java`
- Create: `backend/src/main/java/com/trendradar/crossborder/controller/CrossBorderController.java`

### ChannelController

```java
@RestController
@RequestMapping("/api/v2/channels")
@RequiredArgsConstructor
public class ChannelController {

    @GetMapping("/ranking")
    // GET /api/v2/channels/ranking?country=KR&limit=100
    public ApiResponse<List<ChannelRankingResponse>> getRanking(
            @RequestParam String country,
            @RequestParam(defaultValue = "100") int limit) { ... }

    @GetMapping("/{channelId}")
    // GET /api/v2/channels/{channelId}
    public ApiResponse<ChannelDetailResponse> getDetail(
            @PathVariable String channelId) { ... }

    @GetMapping("/{channelId}/videos")
    public ApiResponse<List<TrendingVideoResponse>> getChannelVideos(
            @PathVariable String channelId) { ... }

    @GetMapping("/{channelId}/snapshots")
    public ApiResponse<List<ChannelSnapshot>> getSnapshots(
            @PathVariable String channelId) { ... }
}
```

### KeywordController

```java
@RestController
@RequestMapping("/api/v2/keywords")
@RequiredArgsConstructor
public class KeywordController {

    @GetMapping("/trending")
    // GET /api/v2/keywords/trending?country=KR&period=DAY&limit=50
    public ApiResponse<List<KeywordTrendResponse>> getTrending(
            @RequestParam String country,
            @RequestParam(defaultValue = "DAY") String period,
            @RequestParam(defaultValue = "50") int limit) { ... }

    @GetMapping("/{keyword}/timeline")
    // GET /api/v2/keywords/{keyword}/timeline?period=WEEK
    public ApiResponse<List<KeywordTimelineResponse>> getTimeline(
            @PathVariable String keyword,
            @RequestParam(defaultValue = "WEEK") String period) { ... }
}
```

### CrossBorderController

```java
@RestController
@RequestMapping("/api/v2/crossborder")
@RequiredArgsConstructor
public class CrossBorderController {

    @GetMapping("/opportunities")
    public ApiResponse<List<OpportunityResponse>> getOpportunities(
            @RequestParam String country) { ... }

    @GetMapping("/propagation")
    public ApiResponse<PropagationResponse> getPropagation(
            @RequestParam String keyword) { ... }

    @GetMapping("/global-vs-local")
    public ApiResponse<GlobalLocalResponse> getGlobalVsLocal(
            @RequestParam String country) { ... }
}
```

- [ ] Step 1: ChannelDetailResponse, KeywordTrendResponse, KeywordTimelineResponse DTO 작성
- [ ] Step 2: ChannelController 작성
- [ ] Step 3: KeywordController 작성
- [ ] Step 4: CrossBorderController 작성
- [ ] Step 5: 컴파일 확인
- [ ] Step 6: 커밋

---

## Task 7: 전체 테스트 + 검증

- [ ] Step 1: 전체 테스트 실행 `./gradlew test`
- [ ] Step 2: 실패 테스트 수정 (기존 테스트 의존성 업데이트)
- [ ] Step 3: Jacoco 커버리지 80%+ 확인
- [ ] Step 4: API 엔드포인트 수동 테스트 (curl)
- [ ] Step 5: 최종 커밋

---

## Summary

| Task | 내용 | 테스트 |
|:---:|------|:---:|
| 1 | ChannelRankingService (5지표 + 등급 + 다크호스) | TDD 4건 |
| 2 | CrossBorderService (3가지 뷰) | TDD 3건 |
| 3 | AiAnalysisService (4타입 + DB 캐싱) | TDD 4건 |
| 4 | KeywordAggregationScheduler (일/주/월 배치) | TDD 2건 |
| 5 | AiAnalysisScheduler (브리핑 1h + 예측 6h) | 컴파일 |
| 6 | v2 API 컨트롤러 3개 (channels, keywords, crossborder) | 컴파일 |
| 7 | 전체 검증 | 통합 |

**총 신규 파일:** ~22개 | **수정 파일:** 3개 | **신규 테스트:** 13건
