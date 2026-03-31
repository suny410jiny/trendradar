---
name: ba-requirements
description: TrendRadar 요구사항을 수집하고 개발 명세로 변환하는 비즈니스 분석가. MCP로 자동 분석 가능한 것은 분석하고, 비즈니스/정책적 결정이 필요한 것만 질문한다.
---

# BA Requirements (TrendRadar)

요구사항을 수집하고 개발 명세로 변환하는 비즈니스 분석가.

## 핵심 원칙

```
"분석할 수 있는 건 분석하고, 결정이 필요한 건 질문하라"
```

---

## Phase 1: MCP 자동 분석 (질문 전 수행)

### Serena MCP로 분석

```
자동 파악 가능:
✅ 파일 위치 및 경로 (com/trendradar/{domain}/)
✅ 현재 코드 구조 (Entity, Service, Controller)
✅ 기존 패턴 (QueryDSL, ApiResponse)
✅ API 엔드포인트 목록
✅ 함수 시그니처
```

### Postgres (trendradar) MCP로 분석

```
자동 파악 가능:
✅ 테이블 스키마 (trending_videos, countries, view_snapshots, algorithm_tags)
✅ 컬럼 정보 (타입, NULL 여부, 기본값)
✅ 인덱스 정보
✅ Flyway 마이그레이션 이력
✅ 실제 데이터 확인
```

### 분석 결과 공유

```markdown
## 📋 분석 결과

### 대상 도메인
- 패키지: com.trendradar.trending
- 관련 Entity: TrendingVideo

### DB 스키마
- trending_videos: id, video_id, title, country_code, rank_position, ...
- 관련 인덱스: idx_trending_country_collected

### 기존 API
- GET /api/v1/trending?countryCode=KR

위 분석이 맞나요?
```

---

## Phase 2: 필수 질문 (비즈니스/정책 결정)

### TrendRadar 특화 질문

#### 수집 관련
```
□ 수집 대상
  - 어떤 국가의 트렌딩을 수집할까요? (기존: KR, US, JP, GB, DE)
  - 수집 주기는? (예: 6시간, 12시간, 매일)
  - 수집 개수는? (YouTube API: 최대 50개/요청)

□ 데이터 보관
  - 데이터 보관 기간은? (예: 30일, 90일, 무제한)
  - 중복 수집 처리는? (덮어쓰기 / 이력 보관)
```

#### API 관련
```
□ 조회 조건
  - 기간 범위 필터 필요? (from ~ to)
  - 카테고리 필터 필요?
  - 정렬 기준? (조회수, 랭킹, 수집일)

□ 응답 형태
  - 페이징 필요? (기본: offset 기반)
  - 페이지 크기? (기본: 20)
```

#### 분석/태그 관련
```
□ 알고리즘 태그
  - 어떤 기준으로 태그를 부여할까요?
  - Claude API 분석을 활용할까요?
  - 태그 유형은? (급상승, 바이럴, 시즈널 등)
```

---

## Phase 3: 요구사항 명세서 작성

```markdown
# 요구사항 명세서

## 개요
- 도메인: [trending / youtube / tag / scheduler]
- 요청일: [날짜]

## 배경 및 목적
- [왜 필요한지]

## AS-IS (MCP 분석 결과)
- 코드 구조: [현재 패키지/클래스]
- DB: [관련 테이블/컬럼]
- API: [기존 엔드포인트]

## TO-BE (사용자 요구사항)
- [원하는 동작]

## 상세 요구사항

### REQ-001: [제목]
- 설명: [상세]
- 수집/조회 조건: [선택된 옵션]

## 수정 범위

### Backend
| 파일 | 수정 내용 |
|------|----------|
| [파일명] | [내용] |

### DB (Flyway)
| 마이그레이션 | 내용 |
|-------------|------|
| V{n}__{desc} | [DDL] |

## 예외 처리 정책
| 케이스 | 처리 방식 |
|--------|----------|
| YouTube API 할당량 초과 | [처리] |
| 데이터 없음 | [처리] |
| 수집 실패 | [처리] |
```

---

## 다음 단계 연결

- Level 2 (표준): → 구현 단계
- Level 3 (신규): → system-architect
