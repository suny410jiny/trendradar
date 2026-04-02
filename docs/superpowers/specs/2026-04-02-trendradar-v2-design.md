# TrendRadar v2 — 설계 문서

> **작성일:** 2026-04-02
> **상태:** 설계 승인 완료
> **접근법:** 점진적 확장 (3 Phase)

---

## 1. 비전

### 컨셉 전환

| | MVP (현재) | v2 (목표) |
|---|---|---|
| 단위 | 영상 TOP 50 나열 | **채널 중심** 급부상 랭킹 |
| 관점 | "뭐가 뜨나" | **"왜 뜨나"** + AI 분석 |
| 데이터 | 트렌딩 리스트 스냅샷 | 키워드 + 성장속도 + 시계열 |
| 깊이 | 나라별 필터 | 크로스보더 전파 + 다차원 분석 |
| 디자인 | 단일 페이지, 기본 UI | Social Blade 스타일 전문가급 |

### 핵심 기능 6가지

1. **채널 급부상 랭킹** — 트렌딩 영상 기반 TOP 100 채널, 등급 시스템 (S/A/B/C/D)
2. **키워드 트렌드 분석** — 나라별/시간대별 키워드 랭킹, 시간/일/주/월/분기/년 집계
3. **AI 종합 분석** — 트렌드 브리핑 + 영상분석 + 채널분석 + 성장예측 (긍정적 톤)
4. **크로스보더 트렌드** — 내 나라 기준 해외 트렌드 + 전파 경로 + 글로벌 vs 로컬
5. **탑 트렌드 타임라인** — 년/분기/월/주 단위 키워드 흐름 히스토리
6. **전문가급 시각화** — Social Blade 스타일 다크 테마, 데이터 밀도 + 직관성

### 수익 모델
- 기본 무료 (누구나 접근)
- 광고 (Google AdSense) — Phase 3에서 슬롯 준비
- 향후: 프리미엄 API/리포트 (트래픽 확보 후)

---

## 2. DB 스키마 변경

### 신규 테이블

#### V7: channels + channel_snapshots + trending_videos 변경

```sql
-- 채널 마스터 테이블
CREATE TABLE channels (
    channel_id      VARCHAR(30) PRIMARY KEY,
    title           VARCHAR(200) NOT NULL,
    thumbnail_url   VARCHAR(500),
    subscriber_count BIGINT DEFAULT 0,
    video_count     BIGINT DEFAULT 0,
    total_view_count BIGINT DEFAULT 0,
    first_seen_at   TIMESTAMP WITH TIME ZONE,
    updated_at      TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_channels_updated ON channels(updated_at DESC);

-- 채널 스냅샷 (성장 추이 시계열)
CREATE TABLE channel_snapshots (
    id                  BIGSERIAL PRIMARY KEY,
    channel_id          VARCHAR(30) NOT NULL REFERENCES channels(channel_id),
    subscriber_count    BIGINT DEFAULT 0,
    video_count         BIGINT DEFAULT 0,
    total_view_count    BIGINT DEFAULT 0,
    trending_video_count INT DEFAULT 0,
    snapshot_at         TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_channel_snapshots_channel_time
    ON channel_snapshots(channel_id, snapshot_at DESC);

-- trending_videos에 channel_id 추가
ALTER TABLE trending_videos ADD COLUMN channel_id VARCHAR(30);
CREATE INDEX idx_trending_channel_id ON trending_videos(channel_id);
```

#### V8: keyword_trends

```sql
CREATE TABLE keyword_trends (
    id              BIGSERIAL PRIMARY KEY,
    keyword         VARCHAR(200) NOT NULL,
    country_code    VARCHAR(5) NOT NULL,
    video_count     INT DEFAULT 0,
    total_views     BIGINT DEFAULT 0,
    avg_engagement  DOUBLE PRECISION DEFAULT 0,
    period_type     VARCHAR(10) NOT NULL,  -- HOUR, DAY, WEEK, MONTH, QUARTER, YEAR
    period_start    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_keyword_trends_lookup
    ON keyword_trends(keyword, country_code, period_type);
CREATE UNIQUE INDEX idx_keyword_trends_unique
    ON keyword_trends(keyword, country_code, period_type, period_start);
```

#### V9: ai_analyses

```sql
CREATE TABLE ai_analyses (
    id              BIGSERIAL PRIMARY KEY,
    analysis_type   VARCHAR(30) NOT NULL,   -- BRIEFING, VIDEO, CHANNEL, PREDICTION
    target_id       VARCHAR(30) NOT NULL,   -- country_code or video_id or channel_id
    country_code    VARCHAR(5),
    content         TEXT NOT NULL,           -- AI 분석 결과 (JSON)
    model_used      VARCHAR(50),
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_ai_analyses_lookup
    ON ai_analyses(analysis_type, target_id, created_at DESC);
```

### YouTube API 할당량 영향

| 호출 | 유닛/회 | 횟수/일 | 합계/일 |
|------|:------:|:------:|:------:|
| videos.list (기존) | 1 | 5×24 = 120 | 120 |
| channels.list (신규) | 1 | ~3×24 = 72 | 72 |
| **합계** | | | **192 / 10,000** |

할당량 1.9% 사용. 여유 충분.

---

## 3. 수집 파이프라인 확장

### 현재 파이프라인
```
YouTube videos.list → TrendingVideo 저장 → AlgorithmTag 계산
```

### v2 파이프라인
```
YouTube videos.list → TrendingVideo 저장 (+ channel_id 추가)
    ↓ channelId 추출 (중복 제거)
    → YouTube channels.list (배치 50개씩) → Channel UPSERT
    → ChannelSnapshot 저장
    → AlgorithmTag 계산
    → KeywordTrend 집계
```

### 변경 대상 파일
- `YouTubeVideoItem.Snippet` — `channelId` 필드 추가
- `TrendingVideo` 엔티티 — `channelId` 컬럼 추가
- `TrendingCollectService` — 채널 수집 로직 추가
- `YouTubeApiClient` — `fetchChannels(List<String> channelIds)` 메서드 추가
- `TrendingScheduler` — 키워드 집계 호출 추가

---

## 4. 채널 급부상 랭킹 알고리즘

### Channel Surge Score (5가지 지표)

| 지표 | 비중 | 계산 |
|------|:---:|------|
| 트렌딩 빈도 | 25% | 24시간 내 트렌딩 등장 고유 영상 수 |
| 순위 가중 | 20% | AVG(51 - rank_position) / 50 |
| 조회수 성장률 | 20% | 채널 영상들의 24h 조회수 성장률 평균 |
| 참여율 | 15% | AVG((like + comment) / view) |
| **채널 규모 대비 폭발력** | **20%** | **video_view / subscriber_count (burst_ratio)** |

```
surge_score = 0.25 × trending_freq_norm
            + 0.20 × rank_score
            + 0.20 × growth_rate_norm
            + 0.15 × engagement_norm
            + 0.20 × burst_ratio_norm
```

*_norm = min-max 정규화 (0~1 범위). TOP 100은 surge_score DESC 정렬.

### 채널 등급 시스템

| 등급 | 기준 | 표현 |
|:---:|------|------|
| S | score >= 0.9 | "지금 가장 핫한 채널!" |
| A | score >= 0.75 | "주목할 만한 성장세!" |
| B | score >= 0.55 | "꾸준히 성장하는 채널" |
| C | score >= 0.35 | "잠재력 있는 채널" |
| D | score < 0.35 | "트렌딩에 등장한 채널" |

### 다크호스 감지

구독자 수 대비 조회수가 비정상적으로 높은 채널/영상을 별도 하이라이트:
- `burst_ratio >= 10x` → 다크호스 뱃지 표시
- 채널 상세 페이지에서 해당 채널의 급상승 영상 하이라이트

### 관련 API

```
GET /api/v2/channels/ranking?country=KR&limit=100
GET /api/v2/channels/ranking?country=ALL&limit=100     -- 글로벌 종합
GET /api/v2/channels/{channelId}                       -- 채널 상세 + AI 분석
GET /api/v2/channels/{channelId}/videos                -- 채널의 트렌딩 영상 목록
GET /api/v2/channels/{channelId}/snapshots             -- 채널 성장 추이
```

---

## 5. 키워드 분석 엔진

### 키워드 추출 소스 (3가지)

| 소스 | 현재 | 설명 |
|------|:---:|------|
| YouTube Tags | ✅ 수집 중 | snippet.tags 배열 |
| 제목 키워드 | 🆕 신규 | 제목에서 단어 추출 (영어권 우선, 불용어 제거) |
| 카테고리 태그 | ✅ 수집 중 | YouTube 카테고리 상위 분류 |

### 키워드 점수

```
keyword_score = frequency_norm × 0.5 + impact_norm × 0.5

frequency = 해당 키워드를 가진 영상 수
impact    = AVG(view_count) × AVG(engagement_rate)
```

### 집계 주기

| 주기 | 트리거 | 저장 |
|------|--------|------|
| 시간별 | 매 수집 시 실시간 | keyword_trends (period_type=HOUR) |
| 일별 | 매일 자정 배치 | keyword_trends (period_type=DAY) |
| 주별 | 매주 월요일 배치 | keyword_trends (period_type=WEEK) |
| 월별 | 매월 1일 배치 | keyword_trends (period_type=MONTH) |
| 분기별 | 매분기 1일 배치 | keyword_trends (period_type=QUARTER) |
| 년별 | 매년 1/1 배치 | keyword_trends (period_type=YEAR) |

### 관련 API

```
GET /api/v2/keywords/trending?country=KR&period=DAY&limit=50
GET /api/v2/keywords/{keyword}/timeline?period=WEEK
GET /api/v2/keywords/{keyword}/videos?country=KR
```

### 크로스체크 사항
- 한국어/일본어 제목 키워드 추출: 형태소 분석 없이 공백 분리만으로는 정확도 낮음 → 초기에는 YouTube tags 위주, 제목은 영어권만
- 키워드 정규화: 소문자 변환 적용. 동의어 매핑은 향후 추가
- 데이터 축적: 주/월/분기/년 분석은 데이터가 쌓여야 의미 있음 → 수집부터 빠르게 시작

---

## 6. 크로스보더 트렌드 분석

### 3가지 분석 뷰

#### 뷰 1: 내 나라 기준 해외 트렌드
"한국에서는 아직 안 뜨는데, 다른 나라에서는 뜨고 있는 것"
- 로직: 다른 나라 keyword_trends에 있지만 내 나라에는 없는 키워드 추출
- 표현: "🇺🇸 미국에서 'AI cooking' 급상승 중 → 🇰🇷 선점 기회!"

#### 뷰 2: 트렌드 전파 경로
"이 키워드가 어느 나라에서 시작해서 어디로 퍼지고 있는가"
- 로직: keyword_trends의 period_start를 나라별 비교, 최초 등장일 기준 시간순 정렬
- 표현: "🇺🇸 3/28 → 🇬🇧 3/29 → 🇩🇪 3/30 → 🇯🇵 3/31 → 🇰🇷 예상 4/1~2"

#### 뷰 3: 글로벌 공통 vs 로컬 전용
"전 세계가 관심 있는 것" vs "이 나라만 관심 있는 것"
- 로직: keyword_trends에서 같은 키워드의 country_code 개수로 분류
- 글로벌: 3개국 이상 동시 등장
- 로컬: 1개국에서만 등장

### 관련 API

```
GET /api/v2/crossborder/opportunities?country=KR       -- 뷰1: 선점 기회
GET /api/v2/crossborder/propagation?keyword=xxx        -- 뷰2: 전파 경로
GET /api/v2/crossborder/global-vs-local?country=KR     -- 뷰3: 글로벌 vs 로컬
```

### 크로스체크 사항
- 같은 영상(video_id)이 여러 나라에 동시 등장하는 경우도 추적 (현재 GLOBAL 태그로 일부 커버)
- 전파 경로 분석은 최소 1~2주치 데이터 필요

---

## 7. AI 분석 시스템

### 4가지 분석 타입

| 타입 | 트리거 | 캐시 TTL | 모델 | 입력 데이터 |
|------|--------|:-------:|------|-----------|
| 트렌드 브리핑 | 스케줄러 (1시간) | 1h | Haiku | TOP 10 영상 + 키워드 랭킹 + 크로스보더 |
| 영상별 분석 | 사용자 요청 | 24h | Haiku | 영상 메타데이터 + 태그 + 순위 변동 |
| 채널 분석 | 사용자 요청 | 6h | Sonnet | 채널 스냅샷 + 트렌딩 영상 + surge_score |
| 성장 예측 | 스케줄러 (6시간) | 6h | Sonnet | 키워드 추이 + 크로스보더 + 채널 성장률 |

### 예상 일일 API 호출

| 타입 | 호출/일 |
|------|:------:|
| 브리핑 | 5국 × 24h = 120 |
| 영상 분석 | ~50 (추정, 캐시 활용) |
| 채널 분석 | ~30 (추정, 캐시 활용) |
| 성장 예측 | 5국 × 4 = 20 |
| **합계** | **~220** |

### 비용 절약 전략
- Haiku 사용: 브리핑, 영상분석 (저비용 고속)
- Sonnet 사용: 채널분석, 예측 (정확도 필요한 곳만)
- DB 캐싱: ai_analyses 테이블로 중복 호출 방지
- 배치 프롬프트: 여러 영상을 한 번에 분석 요청

### 프롬프트 톤 가이드

**원칙: 긍정적, 기회 중심, 재미있는 분석**

- ❌ "이 채널은 성장이 부진합니다"
- ✅ "이 채널은 최근 빠르게 성장하고 있어요!"
- ❌ "참여율이 낮아 하락 가능성이 있습니다"
- ✅ "댓글 참여가 활발해서 주목할 만합니다"
- ❌ "경쟁 채널에 비해 열위입니다"
- ✅ "비슷한 키워드로 새로운 기회를 노려볼 수 있어요"

---

## 8. 프론트엔드 페이지 구조

### 페이지 구성 (6페이지)

| 경로 | 페이지 | 핵심 컴포넌트 |
|------|--------|-------------|
| `/` | 대시보드 | KPI 카드 + AI 브리핑 + TOP 5 채널 + TOP 10 키워드 |
| `/trending` | 트렌딩 영상 | TOP 50 영상 리스트 (기존 기능 유지) |
| `/channels` | 채널 랭킹 | TOP 100 채널 + 등급 뱃지 + 스파크라인 + burst_ratio |
| `/keywords` | 키워드 트렌드 | 기간 토글(시간~년) + 라인 차트 + 워드클라우드 + 성장률 뱃지 |
| `/crossborder` | 크로스보더 | 3탭(선점기회/전파경로/글로벌vs로컬) |
| `/video/:id` | 영상 상세 | 영상 메타 + 조회수 차트 + AI 분석 |
| `/channel/:id` | 채널 상세 | 채널 정보 + 성장 차트 + 트렌딩 영상 + AI 분석 |

### 네비게이션
- 상단 고정 네비게이션 바
- 나라 선택은 전역 (모든 페이지에 적용)
- 다크 테마 기본 (Social Blade 스타일)

### 디자인 원칙
- Social Blade 스타일: 데이터 밀도 높지만 정돈된 느낌
- 일반인도 한눈에 파악 가능한 직관적 시각화
- 등급 뱃지, 성장률 뱃지, 다크호스 뱃지 등 시각적 요소 활용

### 광고 슬롯 (Phase 3)
- 상단 배너 (728×90)
- 사이드바 (300×250)
- 리스트 중간 (네이티브)
- AdSlot placeholder 컴포넌트만 배치, 실제 광고 코드는 트래픽 확보 후

---

## 9. 보안

### Rate Limiting
- 구현: bucket4j + Spring Filter
- 일반 API: 60 req/min per IP
- AI 분석 API: 10 req/min per IP
- 초과 시: HTTP 429 Too Many Requests

### Admin API 보호
- 방식: API Key 헤더 인증
- 헤더: `X-Admin-Key: {env.ADMIN_API_KEY}`
- 대상: `POST /api/v1/admin/*`
- 로그인 시스템 불필요 (공개 사이트)

---

## 10. Phase별 구현 계획

### Phase 1: 데이터 기반 강화 (2~3주)

| 순서 | 작업 | 파일/대상 |
|:---:|------|----------|
| 1 | Flyway V7: channels + channel_snapshots + trending_videos 변경 | db/migration/V7__*.sql |
| 2 | Flyway V8: keyword_trends | db/migration/V8__*.sql |
| 3 | Flyway V9: ai_analyses | db/migration/V9__*.sql |
| 4 | Channel 도메인 (Entity, Repository, Service) | channel/ 패키지 |
| 5 | YouTubeApiClient.fetchChannels() 추가 | youtube/client/ |
| 6 | YouTubeVideoItem.Snippet에 channelId 추가 | youtube/dto/ |
| 7 | TrendingCollectService 확장 (채널 수집 + 키워드 집계) | trending/service/ |
| 8 | KeywordTrend 도메인 (Entity, Repository, Service) | keyword/ 패키지 |
| 9 | Rate Limiting (bucket4j) | common/config/ |
| 10 | Admin API Key 인증 필터 | common/security/ |

### Phase 2: 분석 엔진 구축 (2~3주)

| 순서 | 작업 | 파일/대상 |
|:---:|------|----------|
| 1 | ChannelSurgeScoreService (5지표 종합 스코어) | channel/service/ |
| 2 | ChannelGradeService (등급 시스템 S~D) | channel/service/ |
| 3 | CrossBorderAnalysisService (3가지 뷰) | crossborder/ 패키지 |
| 4 | AIAnalysisService (4가지 분석 타입) | ai/ 패키지 |
| 5 | KeywordAggregationScheduler (일/주/월 배치) | scheduler/ |
| 6 | AI 분석 스케줄러 (브리핑 1시간, 예측 6시간) | scheduler/ |
| 7 | v2 API 컨트롤러 (channels, keywords, crossborder) | */controller/ |

### Phase 3: UI 리디자인 (2~3주)

| 순서 | 작업 | 파일/대상 |
|:---:|------|----------|
| 1 | 네비게이션 바 + 라우팅 (6페이지) | components/layout/, App.tsx |
| 2 | 대시보드 리디자인 | pages/DashboardPage.tsx |
| 3 | 채널 랭킹 페이지 | pages/ChannelsPage.tsx |
| 4 | 키워드 트렌드 페이지 | pages/KeywordsPage.tsx |
| 5 | 크로스보더 페이지 | pages/CrossBorderPage.tsx |
| 6 | 영상/채널 상세 페이지 | pages/VideoDetailPage.tsx, ChannelDetailPage.tsx |
| 7 | 다크 테마 + 전문가급 스타일링 | index.css, tailwind 설정 |
| 8 | 광고 슬롯 placeholder | components/common/AdSlot.tsx |
| 9 | 새 API hooks + stores | hooks/, stores/ |

---

## 11. 리스크 및 완화 전략

| 리스크 | 확률 | 영향 | 완화 |
|--------|:---:|:---:|------|
| YouTube API 할당량 초과 | 낮음 | 높음 | 192/10,000 유닛 (1.9%), 모니터링 로그 |
| Claude API 비용 증가 | 중간 | 중간 | DB 캐싱 + Haiku/Sonnet 분리 |
| 한국어 키워드 추출 정확도 | 높음 | 낮음 | 초기에는 YouTube tags 위주, 제목은 영어권만 |
| 데이터 축적 부족 | 확실 | 중간 | Phase 1에서 즉시 수집 시작, 빈 상태 UI 대응 |
| 프론트 리디자인 복잡도 | 중간 | 중간 | 기존 컴포넌트 재사용, shadcn/ui 활용 |

---

## 12. 성능 기준

| 작업 | 목표 |
|------|------|
| 채널 랭킹 조회 (100개) | < 500ms |
| 키워드 트렌드 조회 (50개) | < 300ms |
| 크로스보더 분석 | < 1000ms |
| AI 분석 (캐시 히트) | < 100ms |
| AI 분석 (캐시 미스) | < 5000ms |
| 전체 수집 사이클 (5개국) | < 15s |
