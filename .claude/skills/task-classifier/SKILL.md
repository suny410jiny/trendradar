---
name: task-classifier
description: TrendRadar 작업 요청을 자동으로 분류하고 패턴을 매칭한다. 이전 작업 이력과 패턴 라이브러리를 조회하여 유사한 작업을 찾고, 템플릿 기반 자동 생성이 가능한지 판단한다.
---

# Task Classifier (TrendRadar)

작업 요청 자동 분류 및 패턴 매칭을 담당한다.

## 연동

- **local-context-manager**: 이력 조회, 패턴 라이브러리 조회

---

## 분류 프로세스

### Step 1: 도메인 식별

```
요청: "트렌딩 비디오에 duration 필드 추가해줘"

도메인 식별:
- 키워드: "트렌딩 비디오"
- 매칭: trending 도메인
- 파일: com/trendradar/trending/
```

### Step 2: 패턴 매칭

```
요청 분석:
- 작업 유형: "필드 추가"
- 대상: "duration"

패턴 검색:
- "field_add" 패턴 발견
- 관련: Entity + DTO + Migration + Repository
```

### Step 3: 제안 생성

```json
{
  "domain": "trending",
  "pattern_matched": true,
  "pattern_name": "field_add",
  "suggestion": {
    "type": "template_generate",
    "files": [
      "TrendingVideo.java (Entity 필드 추가)",
      "V{next}__add_duration.sql (Migration)",
      "TrendingVideoResponse.java (DTO 추가)"
    ],
    "next_agent": "code-generator"
  }
}
```

---

## TrendRadar 도메인 키워드 매핑

```
"트렌딩", "인기", "trending"           → trending/*
"유튜브", "YouTube", "API", "수집"     → youtube/*
"스케줄러", "배치", "cron"             → scheduler/*
"태그", "알고리즘", "분류"             → tag/*
"국가", "country", "지역"             → (countries 테이블)
"스냅샷", "조회수", "view"            → (view_snapshots 테이블)
"공통", "설정", "config"              → common/*
```

## TrendRadar 패턴 라이브러리

| 패턴 | 설명 | 관련 파일 |
|------|------|----------|
| field_add | Entity 필드 추가 | Entity, DTO, Migration |
| api_endpoint | 새 API 엔드포인트 | Controller, Service, DTO |
| domain_crud | 도메인 CRUD 생성 | 전체 패키지 |
| migration_add | DB 마이그레이션 추가 | Migration SQL |
| collector_add | 수집기 추가 | Client, DTO, Scheduler |
| index_add | 인덱스 추가 | Migration SQL |
| query_custom | QueryDSL 쿼리 추가 | RepositoryCustom, Impl |

---

## 다음 단계 연결

- 패턴 매칭 성공 → code-generator
- 패턴 없음 → ba-requirements
- 유사 이력 있음 → code-generator (복제 모드)
