# TrendRadar v2 Phase 1: 데이터 기반 강화 — 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 채널 수집, 키워드 집계, AI 캐싱 테이블을 추가하고, Rate Limiting + Admin 보호를 적용하여 v2 분석 엔진의 데이터 토대를 구축한다.

**Architecture:** 기존 Spring Boot + JPA 구조를 유지하면서 `channel/`, `keyword/`, `common/security/` 패키지를 추가한다. 수집 파이프라인에 채널 정보 수집과 키워드 집계 단계를 추가하고, 모든 API에 Rate Limiting을 적용한다.

**Tech Stack:** Spring Boot 3.5.0, Java 21, PostgreSQL 16, Flyway, bucket4j (Rate Limiting), Testcontainers, Mockito

**Spec:** `docs/superpowers/specs/2026-04-02-trendradar-v2-design.md`

---

## File Map

### 신규 파일

```
backend/src/main/resources/db/migration/
  V7__create_channels_and_add_channel_id.sql
  V8__create_keyword_trends.sql
  V9__create_ai_analyses.sql

backend/src/main/java/com/trendradar/channel/
  domain/Channel.java
  domain/ChannelSnapshot.java
  repository/ChannelRepository.java
  repository/ChannelSnapshotRepository.java
  service/ChannelCollectService.java

backend/src/main/java/com/trendradar/keyword/
  domain/KeywordTrend.java
  domain/PeriodType.java
  repository/KeywordTrendRepository.java
  service/KeywordTrendService.java

backend/src/main/java/com/trendradar/common/
  security/AdminApiKeyFilter.java
  filter/RateLimitFilter.java

backend/src/main/java/com/trendradar/youtube/
  dto/YouTubeChannelItem.java
  dto/YouTubeChannelResponse.java

backend/src/test/java/com/trendradar/channel/
  service/ChannelCollectServiceTest.java

backend/src/test/java/com/trendradar/keyword/
  service/KeywordTrendServiceTest.java

backend/src/test/java/com/trendradar/common/
  security/AdminApiKeyFilterTest.java
  filter/RateLimitFilterTest.java
```

### 수정 파일

```
backend/build.gradle                                          — bucket4j 의존성 추가
backend/src/main/resources/application-local.yml              — admin.api.key 설정 추가
backend/src/main/java/com/trendradar/youtube/dto/YouTubeVideoItem.java  — channelId 추가
backend/src/main/java/com/trendradar/trending/domain/TrendingVideo.java — channelId 필드 추가
backend/src/main/java/com/trendradar/trending/service/TrendingCollectService.java — 채널수집+키워드 통합
backend/src/main/java/com/trendradar/youtube/client/YouTubeApiClient.java — fetchChannels() 추가
backend/src/main/java/com/trendradar/scheduler/TrendingScheduler.java — 채널+키워드 단계 추가
```

---

## Task 1: DB 마이그레이션 (V7, V8, V9)

**Files:**
- Create: `backend/src/main/resources/db/migration/V7__create_channels_and_add_channel_id.sql`
- Create: `backend/src/main/resources/db/migration/V8__create_keyword_trends.sql`
- Create: `backend/src/main/resources/db/migration/V9__create_ai_analyses.sql`

- [ ] **Step 1: V7 마이그레이션 작성**

```sql
-- V7__create_channels_and_add_channel_id.sql

-- 채널 마스터 테이블
CREATE TABLE channels (
    channel_id       VARCHAR(30) PRIMARY KEY,
    title            VARCHAR(200) NOT NULL,
    thumbnail_url    VARCHAR(500),
    subscriber_count BIGINT DEFAULT 0,
    video_count      BIGINT DEFAULT 0,
    total_view_count BIGINT DEFAULT 0,
    first_seen_at    TIMESTAMP WITH TIME ZONE,
    updated_at       TIMESTAMP WITH TIME ZONE,
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_channels_updated ON channels(updated_at DESC);

-- 채널 스냅샷 (성장 추이 시계열)
CREATE TABLE channel_snapshots (
    id                   BIGSERIAL PRIMARY KEY,
    channel_id           VARCHAR(30) NOT NULL REFERENCES channels(channel_id),
    subscriber_count     BIGINT DEFAULT 0,
    video_count          BIGINT DEFAULT 0,
    total_view_count     BIGINT DEFAULT 0,
    trending_video_count INT DEFAULT 0,
    snapshot_at          TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_channel_snapshots_channel_time
    ON channel_snapshots(channel_id, snapshot_at DESC);

-- trending_videos에 channel_id 추가
ALTER TABLE trending_videos ADD COLUMN channel_id VARCHAR(30);
CREATE INDEX idx_trending_channel_id ON trending_videos(channel_id);
```

- [ ] **Step 2: V8 마이그레이션 작성**

```sql
-- V8__create_keyword_trends.sql

CREATE TABLE keyword_trends (
    id             BIGSERIAL PRIMARY KEY,
    keyword        VARCHAR(200) NOT NULL,
    country_code   VARCHAR(5) NOT NULL,
    video_count    INT DEFAULT 0,
    total_views    BIGINT DEFAULT 0,
    avg_engagement DOUBLE PRECISION DEFAULT 0,
    period_type    VARCHAR(10) NOT NULL,
    period_start   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_keyword_trends_lookup
    ON keyword_trends(keyword, country_code, period_type);
CREATE UNIQUE INDEX idx_keyword_trends_unique
    ON keyword_trends(keyword, country_code, period_type, period_start);
```

- [ ] **Step 3: V9 마이그레이션 작성**

```sql
-- V9__create_ai_analyses.sql

CREATE TABLE ai_analyses (
    id            BIGSERIAL PRIMARY KEY,
    analysis_type VARCHAR(30) NOT NULL,
    target_id     VARCHAR(30) NOT NULL,
    country_code  VARCHAR(5),
    content       TEXT NOT NULL,
    model_used    VARCHAR(50),
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_ai_analyses_lookup
    ON ai_analyses(analysis_type, target_id, created_at DESC);
```

- [ ] **Step 4: 로컬 DB에서 마이그레이션 검증**

Run: `cd backend && ./gradlew bootRun` (스타트업 후 종료)
Expected: Flyway V7, V8, V9 마이그레이션 성공 로그. 실패 시 SQL 문법 수정.

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/resources/db/migration/V7__create_channels_and_add_channel_id.sql \
        backend/src/main/resources/db/migration/V8__create_keyword_trends.sql \
        backend/src/main/resources/db/migration/V9__create_ai_analyses.sql
git commit -m "feat: V7~V9 Flyway 마이그레이션 추가 (channels, keyword_trends, ai_analyses)"
```

---

## Task 2: Channel 도메인 (Entity + Repository)

**Files:**
- Create: `backend/src/main/java/com/trendradar/channel/domain/Channel.java`
- Create: `backend/src/main/java/com/trendradar/channel/domain/ChannelSnapshot.java`
- Create: `backend/src/main/java/com/trendradar/channel/repository/ChannelRepository.java`
- Create: `backend/src/main/java/com/trendradar/channel/repository/ChannelSnapshotRepository.java`

- [ ] **Step 1: Channel 엔티티 작성**

```java
package com.trendradar.channel.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "channels")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Channel {

    @Id
    @Column(name = "channel_id", length = 30)
    private String channelId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "subscriber_count")
    private Long subscriberCount;

    @Column(name = "video_count")
    private Long videoCount;

    @Column(name = "total_view_count")
    private Long totalViewCount;

    @Column(name = "first_seen_at")
    private OffsetDateTime firstSeenAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        if (this.firstSeenAt == null) {
            this.firstSeenAt = this.createdAt;
        }
    }

    @Builder
    private Channel(String channelId, String title, String thumbnailUrl,
                    Long subscriberCount, Long videoCount, Long totalViewCount,
                    OffsetDateTime firstSeenAt) {
        this.channelId = channelId;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.subscriberCount = subscriberCount;
        this.videoCount = videoCount;
        this.totalViewCount = totalViewCount;
        this.firstSeenAt = firstSeenAt;
    }

    public void updateStats(String title, String thumbnailUrl,
                            Long subscriberCount, Long videoCount, Long totalViewCount) {
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.subscriberCount = subscriberCount;
        this.videoCount = videoCount;
        this.totalViewCount = totalViewCount;
        this.updatedAt = OffsetDateTime.now();
    }
}
```

- [ ] **Step 2: ChannelSnapshot 엔티티 작성**

```java
package com.trendradar.channel.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "channel_snapshots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChannelSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "channel_id", nullable = false, length = 30)
    private String channelId;

    @Column(name = "subscriber_count")
    private Long subscriberCount;

    @Column(name = "video_count")
    private Long videoCount;

    @Column(name = "total_view_count")
    private Long totalViewCount;

    @Column(name = "trending_video_count")
    private Integer trendingVideoCount;

    @Column(name = "snapshot_at", nullable = false)
    private OffsetDateTime snapshotAt;

    @Builder
    private ChannelSnapshot(String channelId, Long subscriberCount,
                            Long videoCount, Long totalViewCount,
                            Integer trendingVideoCount, OffsetDateTime snapshotAt) {
        this.channelId = channelId;
        this.subscriberCount = subscriberCount;
        this.videoCount = videoCount;
        this.totalViewCount = totalViewCount;
        this.trendingVideoCount = trendingVideoCount;
        this.snapshotAt = snapshotAt;
    }
}
```

- [ ] **Step 3: Repository 인터페이스 작성**

```java
// ChannelRepository.java
package com.trendradar.channel.repository;

import com.trendradar.channel.domain.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChannelRepository extends JpaRepository<Channel, String> {

    List<Channel> findByChannelIdIn(List<String> channelIds);
}
```

```java
// ChannelSnapshotRepository.java
package com.trendradar.channel.repository;

import com.trendradar.channel.domain.ChannelSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface ChannelSnapshotRepository extends JpaRepository<ChannelSnapshot, Long> {

    List<ChannelSnapshot> findByChannelIdAndSnapshotAtBetweenOrderBySnapshotAtDesc(
            String channelId, OffsetDateTime from, OffsetDateTime to);

    List<ChannelSnapshot> findByChannelIdOrderBySnapshotAtDesc(String channelId);
}
```

- [ ] **Step 4: 컴파일 확인**

Run: `cd backend && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/trendradar/channel/
git commit -m "feat: Channel, ChannelSnapshot 엔티티 및 Repository 추가"
```

---

## Task 3: YouTube API 확장 (channelId + fetchChannels)

**Files:**
- Modify: `backend/src/main/java/com/trendradar/youtube/dto/YouTubeVideoItem.java:21` — channelId 필드 확인 (이미 있음)
- Create: `backend/src/main/java/com/trendradar/youtube/dto/YouTubeChannelItem.java`
- Create: `backend/src/main/java/com/trendradar/youtube/dto/YouTubeChannelResponse.java`
- Modify: `backend/src/main/java/com/trendradar/youtube/client/YouTubeApiClient.java` — fetchChannels() 추가
- Modify: `backend/src/main/java/com/trendradar/trending/domain/TrendingVideo.java` — channelId 필드 추가
- Modify: `backend/src/main/java/com/trendradar/trending/service/TrendingCollectService.java:72` — channelId 매핑

- [ ] **Step 1: YouTubeChannelItem DTO 작성**

```java
package com.trendradar.youtube.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class YouTubeChannelItem {

    private String id;
    private Snippet snippet;
    private Statistics statistics;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Snippet {
        private String title;
        private Thumbnails thumbnails;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Statistics {
        private String subscriberCount;
        private String videoCount;
        private String viewCount;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Thumbnails {
        private Thumbnail high;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Thumbnail {
        private String url;
    }
}
```

- [ ] **Step 2: YouTubeChannelResponse DTO 작성**

```java
package com.trendradar.youtube.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class YouTubeChannelResponse {
    private List<YouTubeChannelItem> items;
}
```

- [ ] **Step 3: YouTubeApiClient에 fetchChannels() 추가**

`YouTubeApiClient.java` 끝에 추가:

```java
public YouTubeChannelResponse fetchChannels(List<String> channelIds) {
    if (channelIds == null || channelIds.isEmpty()) {
        return new YouTubeChannelResponse();
    }

    String ids = String.join(",", channelIds);
    String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .path("/youtube/v3/channels")
            .queryParam("part", "snippet,statistics")
            .queryParam("id", ids)
            .queryParam("key", apiKey)
            .toUriString();

    log.info("Fetching {} channels", channelIds.size());

    return restTemplate.getForObject(url, YouTubeChannelResponse.class);
}
```

- [ ] **Step 4: TrendingVideo에 channelId 필드 추가**

`TrendingVideo.java`에 필드 추가:

```java
@Column(name = "channel_id", length = 30)
private String channelId;
```

Builder 파라미터에도 `String channelId` 추가, Builder 본문에 `this.channelId = channelId;` 추가.

- [ ] **Step 5: TrendingCollectService.toTrendingVideo()에 channelId 매핑 추가**

`TrendingCollectService.java`의 `toTrendingVideo` 메서드 builder에 추가:

```java
.channelId(snippet != null ? snippet.getChannelId() : null)
```

- [ ] **Step 6: 컴파일 확인**

Run: `cd backend && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: 커밋**

```bash
git add backend/src/main/java/com/trendradar/youtube/ \
        backend/src/main/java/com/trendradar/trending/domain/TrendingVideo.java \
        backend/src/main/java/com/trendradar/trending/service/TrendingCollectService.java
git commit -m "feat: YouTube channels API 연동 및 TrendingVideo channelId 추가"
```

---

## Task 4: ChannelCollectService (TDD)

**Files:**
- Create: `backend/src/test/java/com/trendradar/channel/service/ChannelCollectServiceTest.java`
- Create: `backend/src/main/java/com/trendradar/channel/service/ChannelCollectService.java`

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package com.trendradar.channel.service;

import com.trendradar.channel.domain.Channel;
import com.trendradar.channel.domain.ChannelSnapshot;
import com.trendradar.channel.repository.ChannelRepository;
import com.trendradar.channel.repository.ChannelSnapshotRepository;
import com.trendradar.youtube.client.YouTubeApiClient;
import com.trendradar.youtube.dto.YouTubeChannelItem;
import com.trendradar.youtube.dto.YouTubeChannelResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChannelCollectServiceTest {

    @Mock private YouTubeApiClient youTubeApiClient;
    @Mock private ChannelRepository channelRepository;
    @Mock private ChannelSnapshotRepository channelSnapshotRepository;

    @InjectMocks private ChannelCollectService channelCollectService;

    @Test
    void collectChannels_whenNewChannels_createsChannelAndSnapshot() {
        // Given
        List<String> channelIds = List.of("UC_channel1");
        OffsetDateTime collectedAt = OffsetDateTime.now();

        YouTubeChannelResponse response = createMockChannelResponse(
                "UC_channel1", "TestChannel", 10000L, 50L, 500000L);
        given(youTubeApiClient.fetchChannels(channelIds)).willReturn(response);
        given(channelRepository.findById("UC_channel1")).willReturn(Optional.empty());
        given(channelRepository.save(any(Channel.class))).willAnswer(inv -> inv.getArgument(0));
        given(channelSnapshotRepository.save(any(ChannelSnapshot.class))).willAnswer(inv -> inv.getArgument(0));

        // When
        channelCollectService.collectChannels(channelIds, collectedAt);

        // Then
        ArgumentCaptor<Channel> channelCaptor = ArgumentCaptor.forClass(Channel.class);
        verify(channelRepository).save(channelCaptor.capture());
        Channel saved = channelCaptor.getValue();
        assertThat(saved.getChannelId()).isEqualTo("UC_channel1");
        assertThat(saved.getTitle()).isEqualTo("TestChannel");
        assertThat(saved.getSubscriberCount()).isEqualTo(10000L);

        verify(channelSnapshotRepository).save(any(ChannelSnapshot.class));
    }

    @Test
    void collectChannels_whenExistingChannel_updatesStats() {
        // Given
        List<String> channelIds = List.of("UC_existing");
        OffsetDateTime collectedAt = OffsetDateTime.now();

        Channel existing = Channel.builder()
                .channelId("UC_existing")
                .title("OldTitle")
                .subscriberCount(5000L)
                .videoCount(30L)
                .totalViewCount(100000L)
                .build();

        YouTubeChannelResponse response = createMockChannelResponse(
                "UC_existing", "NewTitle", 8000L, 35L, 200000L);
        given(youTubeApiClient.fetchChannels(channelIds)).willReturn(response);
        given(channelRepository.findById("UC_existing")).willReturn(Optional.of(existing));
        given(channelSnapshotRepository.save(any(ChannelSnapshot.class))).willAnswer(inv -> inv.getArgument(0));

        // When
        channelCollectService.collectChannels(channelIds, collectedAt);

        // Then
        assertThat(existing.getTitle()).isEqualTo("NewTitle");
        assertThat(existing.getSubscriberCount()).isEqualTo(8000L);
        verify(channelRepository, never()).save(any());  // 기존 채널은 영속성 컨텍스트로 업데이트
        verify(channelSnapshotRepository).save(any(ChannelSnapshot.class));
    }

    @Test
    void collectChannels_whenEmptyList_doesNothing() {
        // Given
        List<String> channelIds = List.of();
        OffsetDateTime collectedAt = OffsetDateTime.now();

        // When
        channelCollectService.collectChannels(channelIds, collectedAt);

        // Then
        verifyNoInteractions(youTubeApiClient);
        verifyNoInteractions(channelRepository);
    }

    @Test
    void collectChannels_batchesOver50Channels() {
        // Given: 60개 채널 ID → 50 + 10 두 번 호출
        List<String> channelIds = new java.util.ArrayList<>();
        for (int i = 0; i < 60; i++) {
            channelIds.add("UC_ch" + i);
        }
        OffsetDateTime collectedAt = OffsetDateTime.now();

        given(youTubeApiClient.fetchChannels(anyList())).willReturn(new YouTubeChannelResponse());

        // When
        channelCollectService.collectChannels(channelIds, collectedAt);

        // Then: fetchChannels가 2번 호출되어야 함
        verify(youTubeApiClient, times(2)).fetchChannels(anyList());
    }

    // Helper: Mock YouTubeChannelResponse 생성
    private YouTubeChannelResponse createMockChannelResponse(
            String id, String title, long subscribers, long videos, long views) {
        // Reflection 또는 Jackson ObjectMapper 사용하여 생성
        // 테스트에서는 ObjectMapper로 JSON → 객체 변환
        String json = String.format("""
            {"items":[{"id":"%s","snippet":{"title":"%s","thumbnails":{"high":{"url":"https://img.com/thumb.jpg"}}},
            "statistics":{"subscriberCount":"%d","videoCount":"%d","viewCount":"%d"}}]}
            """, id, title, subscribers, videos, views);
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(json, YouTubeChannelResponse.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

- [ ] **Step 2: 테스트 실행 — 실패 확인**

Run: `cd backend && ./gradlew test --tests "com.trendradar.channel.service.ChannelCollectServiceTest"`
Expected: FAIL — `ChannelCollectService` 클래스 없음

- [ ] **Step 3: ChannelCollectService 구현**

```java
package com.trendradar.channel.service;

import com.trendradar.channel.domain.Channel;
import com.trendradar.channel.domain.ChannelSnapshot;
import com.trendradar.channel.repository.ChannelRepository;
import com.trendradar.channel.repository.ChannelSnapshotRepository;
import com.trendradar.youtube.client.YouTubeApiClient;
import com.trendradar.youtube.dto.YouTubeChannelItem;
import com.trendradar.youtube.dto.YouTubeChannelResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelCollectService {

    private static final int BATCH_SIZE = 50;

    private final YouTubeApiClient youTubeApiClient;
    private final ChannelRepository channelRepository;
    private final ChannelSnapshotRepository channelSnapshotRepository;

    @Transactional
    public void collectChannels(List<String> channelIds, OffsetDateTime collectedAt) {
        if (channelIds == null || channelIds.isEmpty()) {
            return;
        }

        // 50개씩 배치 처리 (YouTube API 제한)
        for (int i = 0; i < channelIds.size(); i += BATCH_SIZE) {
            List<String> batch = channelIds.subList(i, Math.min(i + BATCH_SIZE, channelIds.size()));
            processBatch(batch, collectedAt);
        }
    }

    private void processBatch(List<String> channelIds, OffsetDateTime collectedAt) {
        YouTubeChannelResponse response = youTubeApiClient.fetchChannels(channelIds);

        if (response == null || response.getItems() == null) {
            log.warn("No channel data returned for {} channels", channelIds.size());
            return;
        }

        for (YouTubeChannelItem item : response.getItems()) {
            upsertChannel(item);
            saveSnapshot(item, collectedAt);
        }

        log.info("Processed {} channels", response.getItems().size());
    }

    private void upsertChannel(YouTubeChannelItem item) {
        String channelId = item.getId();
        String title = item.getSnippet() != null ? item.getSnippet().getTitle() : "";
        String thumbnailUrl = extractThumbnailUrl(item);
        Long subscribers = parseLong(item.getStatistics() != null ? item.getStatistics().getSubscriberCount() : null);
        Long videoCount = parseLong(item.getStatistics() != null ? item.getStatistics().getVideoCount() : null);
        Long viewCount = parseLong(item.getStatistics() != null ? item.getStatistics().getViewCount() : null);

        Optional<Channel> existing = channelRepository.findById(channelId);
        if (existing.isPresent()) {
            existing.get().updateStats(title, thumbnailUrl, subscribers, videoCount, viewCount);
        } else {
            Channel channel = Channel.builder()
                    .channelId(channelId)
                    .title(title)
                    .thumbnailUrl(thumbnailUrl)
                    .subscriberCount(subscribers)
                    .videoCount(videoCount)
                    .totalViewCount(viewCount)
                    .build();
            channelRepository.save(channel);
        }
    }

    private void saveSnapshot(YouTubeChannelItem item, OffsetDateTime collectedAt) {
        ChannelSnapshot snapshot = ChannelSnapshot.builder()
                .channelId(item.getId())
                .subscriberCount(parseLong(item.getStatistics() != null ? item.getStatistics().getSubscriberCount() : null))
                .videoCount(parseLong(item.getStatistics() != null ? item.getStatistics().getVideoCount() : null))
                .totalViewCount(parseLong(item.getStatistics() != null ? item.getStatistics().getViewCount() : null))
                .trendingVideoCount(0) // 이후 TrendingScheduler에서 계산
                .snapshotAt(collectedAt)
                .build();
        channelSnapshotRepository.save(snapshot);
    }

    private String extractThumbnailUrl(YouTubeChannelItem item) {
        if (item.getSnippet() == null || item.getSnippet().getThumbnails() == null
                || item.getSnippet().getThumbnails().getHigh() == null) {
            return null;
        }
        return item.getSnippet().getThumbnails().getHigh().getUrl();
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) return 0L;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
```

- [ ] **Step 4: 테스트 실행 — 통과 확인**

Run: `cd backend && ./gradlew test --tests "com.trendradar.channel.service.ChannelCollectServiceTest"`
Expected: 4 tests PASSED

- [ ] **Step 5: 커밋**

```bash
git add backend/src/test/java/com/trendradar/channel/ \
        backend/src/main/java/com/trendradar/channel/service/ChannelCollectService.java
git commit -m "feat: ChannelCollectService 구현 (TDD, 배치 50개, UPSERT)"
```

---

## Task 5: KeywordTrend 도메인 + Service (TDD)

**Files:**
- Create: `backend/src/main/java/com/trendradar/keyword/domain/PeriodType.java`
- Create: `backend/src/main/java/com/trendradar/keyword/domain/KeywordTrend.java`
- Create: `backend/src/main/java/com/trendradar/keyword/repository/KeywordTrendRepository.java`
- Create: `backend/src/test/java/com/trendradar/keyword/service/KeywordTrendServiceTest.java`
- Create: `backend/src/main/java/com/trendradar/keyword/service/KeywordTrendService.java`

- [ ] **Step 1: PeriodType enum 작성**

```java
package com.trendradar.keyword.domain;

public enum PeriodType {
    HOUR, DAY, WEEK, MONTH, QUARTER, YEAR
}
```

- [ ] **Step 2: KeywordTrend 엔티티 작성**

```java
package com.trendradar.keyword.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "keyword_trends")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KeywordTrend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String keyword;

    @Column(name = "country_code", nullable = false, length = 5)
    private String countryCode;

    @Column(name = "video_count")
    private Integer videoCount;

    @Column(name = "total_views")
    private Long totalViews;

    @Column(name = "avg_engagement")
    private Double avgEngagement;

    @Column(name = "period_type", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private PeriodType periodType;

    @Column(name = "period_start", nullable = false)
    private OffsetDateTime periodStart;

    @Builder
    private KeywordTrend(String keyword, String countryCode, Integer videoCount,
                         Long totalViews, Double avgEngagement,
                         PeriodType periodType, OffsetDateTime periodStart) {
        this.keyword = keyword;
        this.countryCode = countryCode;
        this.videoCount = videoCount;
        this.totalViews = totalViews;
        this.avgEngagement = avgEngagement;
        this.periodType = periodType;
        this.periodStart = periodStart;
    }

    public void updateStats(Integer videoCount, Long totalViews, Double avgEngagement) {
        this.videoCount = videoCount;
        this.totalViews = totalViews;
        this.avgEngagement = avgEngagement;
    }
}
```

- [ ] **Step 3: KeywordTrendRepository 작성**

```java
package com.trendradar.keyword.repository;

import com.trendradar.keyword.domain.KeywordTrend;
import com.trendradar.keyword.domain.PeriodType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface KeywordTrendRepository extends JpaRepository<KeywordTrend, Long> {

    Optional<KeywordTrend> findByKeywordAndCountryCodeAndPeriodTypeAndPeriodStart(
            String keyword, String countryCode, PeriodType periodType, OffsetDateTime periodStart);

    List<KeywordTrend> findByCountryCodeAndPeriodTypeAndPeriodStartOrderByVideoCountDesc(
            String countryCode, PeriodType periodType, OffsetDateTime periodStart);

    List<KeywordTrend> findByKeywordAndPeriodTypeOrderByPeriodStartDesc(
            String keyword, PeriodType periodType);
}
```

- [ ] **Step 4: 실패하는 테스트 작성**

```java
package com.trendradar.keyword.service;

import com.trendradar.keyword.domain.KeywordTrend;
import com.trendradar.keyword.domain.PeriodType;
import com.trendradar.keyword.repository.KeywordTrendRepository;
import com.trendradar.trending.domain.TrendingVideo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KeywordTrendServiceTest {

    @Mock private KeywordTrendRepository keywordTrendRepository;

    @InjectMocks private KeywordTrendService keywordTrendService;

    @Test
    void aggregateHourlyKeywords_extractsTagsAndSavesFrequency() {
        // Given
        OffsetDateTime collectedAt = OffsetDateTime.of(2026, 4, 2, 10, 0, 0, 0, ZoneOffset.UTC);
        List<TrendingVideo> videos = List.of(
                buildVideo("KR", "먹방,ASMR", 1000000L, 50000L, 2000L),
                buildVideo("KR", "먹방,요리", 500000L, 20000L, 1000L)
        );

        given(keywordTrendRepository.findByKeywordAndCountryCodeAndPeriodTypeAndPeriodStart(
                anyString(), anyString(), any(), any())).willReturn(Optional.empty());
        given(keywordTrendRepository.save(any(KeywordTrend.class))).willAnswer(inv -> inv.getArgument(0));

        // When
        keywordTrendService.aggregateHourlyKeywords(videos, "KR", collectedAt);

        // Then: "먹방"은 2개 영상, "asmr"은 1개, "요리"는 1개 → 3종류 저장
        ArgumentCaptor<KeywordTrend> captor = ArgumentCaptor.forClass(KeywordTrend.class);
        verify(keywordTrendRepository, org.mockito.Mockito.atLeast(3)).save(captor.capture());

        List<KeywordTrend> saved = captor.getAllValues();
        KeywordTrend mukbang = saved.stream()
                .filter(k -> "먹방".equals(k.getKeyword())).findFirst().orElse(null);
        assertThat(mukbang).isNotNull();
        assertThat(mukbang.getVideoCount()).isEqualTo(2);
        assertThat(mukbang.getPeriodType()).isEqualTo(PeriodType.HOUR);
        assertThat(mukbang.getCountryCode()).isEqualTo("KR");
    }

    @Test
    void aggregateHourlyKeywords_whenNoTags_doesNothing() {
        // Given
        OffsetDateTime collectedAt = OffsetDateTime.now();
        List<TrendingVideo> videos = List.of(
                buildVideo("KR", null, 1000L, 100L, 10L)
        );

        // When
        keywordTrendService.aggregateHourlyKeywords(videos, "KR", collectedAt);

        // Then
        verify(keywordTrendRepository, org.mockito.Mockito.never()).save(any());
    }

    private TrendingVideo buildVideo(String country, String tags,
                                     Long views, Long likes, Long comments) {
        return TrendingVideo.builder()
                .videoId("vid_" + System.nanoTime())
                .title("Test Video")
                .channelTitle("TestChannel")
                .countryCode(country)
                .rankPosition(1)
                .viewCount(views)
                .likeCount(likes)
                .commentCount(comments)
                .collectedAt(OffsetDateTime.now())
                .youtubeTags(tags)
                .build();
    }
}
```

- [ ] **Step 5: 테스트 실행 — 실패 확인**

Run: `cd backend && ./gradlew test --tests "com.trendradar.keyword.service.KeywordTrendServiceTest"`
Expected: FAIL — `KeywordTrendService` 없음

- [ ] **Step 6: KeywordTrendService 구현**

```java
package com.trendradar.keyword.service;

import com.trendradar.keyword.domain.KeywordTrend;
import com.trendradar.keyword.domain.PeriodType;
import com.trendradar.keyword.repository.KeywordTrendRepository;
import com.trendradar.trending.domain.TrendingVideo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeywordTrendService {

    private final KeywordTrendRepository keywordTrendRepository;

    @Transactional
    public void aggregateHourlyKeywords(List<TrendingVideo> videos, String countryCode,
                                         OffsetDateTime collectedAt) {
        if (videos == null || videos.isEmpty()) return;

        // 키워드별 영상 그룹화
        Map<String, List<TrendingVideo>> keywordVideos = new HashMap<>();

        for (TrendingVideo video : videos) {
            List<String> tags = video.getYoutubeTagList();
            for (String tag : tags) {
                String normalized = tag.trim().toLowerCase();
                if (!normalized.isBlank()) {
                    keywordVideos.computeIfAbsent(normalized, k -> new ArrayList<>()).add(video);
                }
            }
        }

        if (keywordVideos.isEmpty()) return;

        OffsetDateTime periodStart = collectedAt.truncatedTo(ChronoUnit.HOURS);

        for (Map.Entry<String, List<TrendingVideo>> entry : keywordVideos.entrySet()) {
            String keyword = entry.getKey();
            List<TrendingVideo> kwVideos = entry.getValue();

            int videoCount = kwVideos.size();
            long totalViews = kwVideos.stream()
                    .mapToLong(v -> v.getViewCount() != null ? v.getViewCount() : 0)
                    .sum();
            double avgEngagement = kwVideos.stream()
                    .filter(v -> v.getViewCount() != null && v.getViewCount() > 0 && v.getLikeCount() != null)
                    .mapToDouble(v -> (double) v.getLikeCount() / v.getViewCount())
                    .average()
                    .orElse(0.0);

            Optional<KeywordTrend> existing = keywordTrendRepository
                    .findByKeywordAndCountryCodeAndPeriodTypeAndPeriodStart(
                            keyword, countryCode, PeriodType.HOUR, periodStart);

            if (existing.isPresent()) {
                existing.get().updateStats(videoCount, totalViews, avgEngagement);
            } else {
                KeywordTrend trend = KeywordTrend.builder()
                        .keyword(keyword)
                        .countryCode(countryCode)
                        .videoCount(videoCount)
                        .totalViews(totalViews)
                        .avgEngagement(avgEngagement)
                        .periodType(PeriodType.HOUR)
                        .periodStart(periodStart)
                        .build();
                keywordTrendRepository.save(trend);
            }
        }

        log.info("Aggregated {} keywords for country={} at {}", keywordVideos.size(), countryCode, periodStart);
    }
}
```

- [ ] **Step 7: 테스트 실행 — 통과 확인**

Run: `cd backend && ./gradlew test --tests "com.trendradar.keyword.service.KeywordTrendServiceTest"`
Expected: 2 tests PASSED

- [ ] **Step 8: 커밋**

```bash
git add backend/src/main/java/com/trendradar/keyword/ \
        backend/src/test/java/com/trendradar/keyword/
git commit -m "feat: KeywordTrend 도메인 + KeywordTrendService 구현 (TDD)"
```

---

## Task 6: TrendingScheduler 통합 (채널 수집 + 키워드 집계)

**Files:**
- Modify: `backend/src/main/java/com/trendradar/scheduler/TrendingScheduler.java`
- Modify: `backend/src/main/java/com/trendradar/trending/service/TrendingCollectService.java`

- [ ] **Step 1: TrendingScheduler에 채널 + 키워드 단계 추가**

`TrendingScheduler.java` 전체 교체:

```java
package com.trendradar.scheduler;

import com.trendradar.channel.service.ChannelCollectService;
import com.trendradar.keyword.service.KeywordTrendService;
import com.trendradar.tag.service.AlgorithmTagService;
import com.trendradar.trending.domain.TrendingVideo;
import com.trendradar.trending.service.TrendingCollectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TrendingScheduler {

    private static final List<String> TARGET_COUNTRIES = List.of("KR", "US", "JP", "GB", "DE");

    private final TrendingCollectService trendingCollectService;
    private final AlgorithmTagService algorithmTagService;
    private final ChannelCollectService channelCollectService;
    private final KeywordTrendService keywordTrendService;

    @Scheduled(cron = "0 0 * * * *")
    public void collectAllCountries() {
        log.info("Starting trending collection for {} countries", TARGET_COUNTRIES.size());
        long startTime = System.currentTimeMillis();

        // Step 1: 5개국 비동기 병렬 수집
        List<CompletableFuture<List<TrendingVideo>>> futures = TARGET_COUNTRIES.stream()
                .map(trendingCollectService::collectAsync)
                .toList();

        List<TrendingVideo> allVideos = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            try {
                List<TrendingVideo> result = futures.get(i).join();
                allVideos.addAll(result);
            } catch (Exception e) {
                log.error("Failed to collect trending for country={}", TARGET_COUNTRIES.get(i), e);
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Collected {} videos from {} countries in {}ms",
                allVideos.size(), TARGET_COUNTRIES.size(), elapsed);

        if (allVideos.isEmpty()) return;

        OffsetDateTime collectedAt = OffsetDateTime.now(ZoneOffset.UTC);

        // Step 2: 알고리즘 태그 계산
        try {
            algorithmTagService.calculateTags(allVideos, collectedAt);
            log.info("Algorithm tag calculation completed");
        } catch (Exception e) {
            log.error("Failed to calculate algorithm tags", e);
        }

        // Step 3: 채널 정보 수집
        try {
            List<String> channelIds = allVideos.stream()
                    .map(TrendingVideo::getChannelId)
                    .filter(id -> id != null && !id.isBlank())
                    .distinct()
                    .toList();
            channelCollectService.collectChannels(channelIds, collectedAt);
            log.info("Channel collection completed for {} channels", channelIds.size());
        } catch (Exception e) {
            log.error("Failed to collect channel data", e);
        }

        // Step 4: 키워드 트렌드 집계 (나라별)
        try {
            Map<String, List<TrendingVideo>> byCountry = allVideos.stream()
                    .collect(Collectors.groupingBy(TrendingVideo::getCountryCode));

            for (Map.Entry<String, List<TrendingVideo>> entry : byCountry.entrySet()) {
                keywordTrendService.aggregateHourlyKeywords(entry.getValue(), entry.getKey(), collectedAt);
            }
            log.info("Keyword aggregation completed for {} countries", byCountry.size());
        } catch (Exception e) {
            log.error("Failed to aggregate keywords", e);
        }
    }
}
```

- [ ] **Step 2: 컴파일 확인**

Run: `cd backend && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 기존 TrendingSchedulerTest 실행**

Run: `cd backend && ./gradlew test --tests "com.trendradar.scheduler.TrendingSchedulerTest"`
Expected: 테스트가 새 의존성(@Mock 추가 필요) 때문에 실패할 수 있음. 실패하면 테스트에 `@Mock ChannelCollectService`, `@Mock KeywordTrendService` 추가.

- [ ] **Step 4: 커밋**

```bash
git add backend/src/main/java/com/trendradar/scheduler/TrendingScheduler.java
git commit -m "feat: TrendingScheduler에 채널 수집 + 키워드 집계 단계 추가"
```

---

## Task 7: Rate Limiting (bucket4j)

**Files:**
- Modify: `backend/build.gradle` — bucket4j 의존성 추가
- Create: `backend/src/main/java/com/trendradar/common/config/RateLimitConfig.java`
- Create: `backend/src/main/java/com/trendradar/common/filter/RateLimitFilter.java`
- Create: `backend/src/test/java/com/trendradar/common/filter/RateLimitFilterTest.java`

- [ ] **Step 1: build.gradle에 bucket4j 의존성 추가**

`build.gradle` dependencies 블록에 추가:

```groovy
// Rate Limiting
implementation 'com.bucket4j:bucket4j-core:8.10.1'
```

- [ ] **Step 2: 실패하는 테스트 작성**

```java
package com.trendradar.common.filter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class RateLimitFilterTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void apiRequest_withinLimit_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/countries"))
                .andExpect(status().isOk());
    }

    @Test
    void apiRequest_exceedingLimit_returns429() throws Exception {
        // 61번 연속 호출 → 마지막은 429
        for (int i = 0; i < 60; i++) {
            mockMvc.perform(get("/api/v1/countries")
                    .header("X-Forwarded-For", "10.0.0.99"));
        }
        mockMvc.perform(get("/api/v1/countries")
                        .header("X-Forwarded-For", "10.0.0.99"))
                .andExpect(status().isTooManyRequests());
    }
}
```

- [ ] **Step 3: RateLimitFilter 구현**

```java
package com.trendradar.common.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter implements Filter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();

        if (!path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(httpRequest);
        int limit = path.contains("/briefing") || path.contains("/ai") ? 10 : 60;
        String bucketKey = clientIp + ":" + limit;

        Bucket bucket = buckets.computeIfAbsent(bucketKey,
                k -> Bucket.builder()
                        .addLimit(Bandwidth.simple(limit, Duration.ofMinutes(1)))
                        .build());

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"success\":false,\"message\":\"Too many requests. Please try again later.\"}");
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
```

- [ ] **Step 4: 테스트 실행 — 통과 확인**

Run: `cd backend && ./gradlew test --tests "com.trendradar.common.filter.RateLimitFilterTest"`
Expected: 2 tests PASSED

- [ ] **Step 5: 커밋**

```bash
git add backend/build.gradle \
        backend/src/main/java/com/trendradar/common/filter/RateLimitFilter.java \
        backend/src/test/java/com/trendradar/common/filter/RateLimitFilterTest.java
git commit -m "feat: Rate Limiting 추가 (bucket4j, 일반 60/min, AI 10/min)"
```

---

## Task 8: Admin API Key 인증 필터

**Files:**
- Create: `backend/src/main/java/com/trendradar/common/security/AdminApiKeyFilter.java`
- Create: `backend/src/test/java/com/trendradar/common/security/AdminApiKeyFilterTest.java`
- Modify: `backend/src/main/resources/application-local.yml` — admin.api.key 설정

- [ ] **Step 1: application-local.yml에 admin key 설정 추가**

```yaml
admin:
  api:
    key: ${ADMIN_API_KEY:dev-admin-key-12345}
```

- [ ] **Step 2: 실패하는 테스트 작성**

```java
package com.trendradar.common.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AdminApiKeyFilterTest {

    private final AdminApiKeyFilter filter = new AdminApiKeyFilter("test-admin-key");

    @Test
    void adminApi_withValidKey_passes() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/collect");
        request.addHeader("X-Admin-Key", "test-admin-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // When
        filter.doFilter(request, response, chain);

        // Then
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void adminApi_withInvalidKey_returns403() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/collect");
        request.addHeader("X-Admin-Key", "wrong-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // When
        filter.doFilter(request, response, chain);

        // Then
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void adminApi_withoutKey_returns403() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/collect");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // When
        filter.doFilter(request, response, chain);

        // Then
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void nonAdminApi_withoutKey_passes() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/trending");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // When
        filter.doFilter(request, response, chain);

        // Then
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
```

- [ ] **Step 3: AdminApiKeyFilter 구현**

```java
package com.trendradar.common.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AdminApiKeyFilter implements Filter {

    private final String adminApiKey;

    public AdminApiKeyFilter(@Value("${admin.api.key:}") String adminApiKey) {
        this.adminApiKey = adminApiKey;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();

        if (!path.startsWith("/api/v1/admin")) {
            chain.doFilter(request, response);
            return;
        }

        String providedKey = httpRequest.getHeader("X-Admin-Key");

        if (adminApiKey.isBlank() || !adminApiKey.equals(providedKey)) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(403);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"success\":false,\"message\":\"Forbidden: Invalid admin API key\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
```

- [ ] **Step 4: 테스트 실행 — 통과 확인**

Run: `cd backend && ./gradlew test --tests "com.trendradar.common.security.AdminApiKeyFilterTest"`
Expected: 4 tests PASSED

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/trendradar/common/security/AdminApiKeyFilter.java \
        backend/src/test/java/com/trendradar/common/security/AdminApiKeyFilterTest.java \
        backend/src/main/resources/application-local.yml
git commit -m "feat: Admin API Key 인증 필터 추가 (X-Admin-Key 헤더)"
```

---

## Task 9: 전체 테스트 + 최종 검증

**Files:** 없음 (검증만)

- [ ] **Step 1: 전체 테스트 실행**

Run: `cd backend && ./gradlew test`
Expected: ALL TESTS PASSED. 실패 시 기존 테스트의 새 의존성 문제(@Mock 추가) 수정.

- [ ] **Step 2: 로컬 서버 기동 확인**

Run: `cd backend && ./gradlew bootRun`
Expected:
- Flyway V7~V9 마이그레이션 성공 로그
- 서버 8081 포트 기동
- `/api/v1/trending?country=KR` 정상 응답
- Rate Limiting 동작 확인

- [ ] **Step 3: Admin API 보호 확인**

```bash
# 키 없이 → 403
curl -X POST http://localhost:8081/api/v1/admin/collect

# 키 포함 → 200
curl -X POST http://localhost:8081/api/v1/admin/collect \
  -H "X-Admin-Key: dev-admin-key-12345"
```

- [ ] **Step 4: 수동 수집 트리거 후 채널/키워드 데이터 확인**

```bash
# 수집 트리거
curl -X POST http://localhost:8081/api/v1/admin/collect \
  -H "X-Admin-Key: dev-admin-key-12345"

# DB 확인 (psql 또는 DBeaver)
# SELECT COUNT(*) FROM channels;
# SELECT COUNT(*) FROM channel_snapshots;
# SELECT COUNT(*) FROM keyword_trends;
# SELECT channel_id FROM trending_videos WHERE channel_id IS NOT NULL LIMIT 5;
```

- [ ] **Step 5: Jacoco 커버리지 확인**

Run: `cd backend && ./gradlew jacocoTestReport`
Expected: 80% 이상. 리포트: `backend/build/reports/jacoco/test/html/index.html`

- [ ] **Step 6: 최종 커밋**

```bash
git add -A
git commit -m "test: Phase 1 전체 테스트 통과 및 검증 완료"
```

---

## Summary

| Task | 내용 | 테스트 |
|:---:|------|:---:|
| 1 | DB 마이그레이션 V7~V9 | 기동 검증 |
| 2 | Channel + ChannelSnapshot 도메인 | 컴파일 |
| 3 | YouTube API 확장 (channelId, fetchChannels) | 컴파일 |
| 4 | ChannelCollectService | TDD 4건 |
| 5 | KeywordTrend 도메인 + Service | TDD 2건 |
| 6 | TrendingScheduler 통합 | 기존 테스트 |
| 7 | Rate Limiting (bucket4j) | TDD 2건 |
| 8 | Admin API Key 필터 | TDD 4건 |
| 9 | 전체 검증 | 통합 검증 |

**총 신규 파일:** 16개 | **수정 파일:** 7개 | **신규 테스트:** 12건
