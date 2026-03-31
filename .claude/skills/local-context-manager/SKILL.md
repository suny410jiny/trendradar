---
name: local-context-manager
description: TrendRadar 프로젝트 내 .context/ 폴더에 작업 이력과 패턴을 로컬로 관리한다. 메타데이터만 저장하여 프라이버시를 보호한다.
---

# Local Context Manager (TrendRadar)

프로젝트 내 로컬 컨텍스트를 관리한다.

## 저장 위치

```
~/Projects/trendradar/
├── .context/                    ← Git ignore
│   ├── config.json             ← 설정
│   ├── project_structure.json  ← 프로젝트 구조
│   ├── domains/
│   │   ├── trending.json       ← 도메인별 정보
│   │   ├── youtube.json
│   │   └── tag.json
│   ├── patterns/
│   │   ├── field_add.json
│   │   ├── api_endpoint.json
│   │   └── collector_add.json
│   └── history/
│       └── 2026-04/
│           └── 2026-04-01.json
```

---

## TrendRadar 프로젝트 구조

```json
{
  "framework": {
    "backend": "Spring Boot 3.5.0",
    "language": "Java 21",
    "database": "PostgreSQL 16",
    "migration": "Flyway",
    "query": "QueryDSL 5.0"
  },
  "paths": {
    "domain_base": "backend/src/main/java/com/trendradar/",
    "migration": "backend/src/main/resources/db/migration/",
    "test_base": "backend/src/test/java/com/trendradar/",
    "config": "backend/src/main/resources/"
  },
  "conventions": {
    "entity_naming": "PascalCase",
    "table_naming": "snake_case",
    "api_prefix": "/api/v1/",
    "response_wrapper": "ApiResponse<T>",
    "indent": 4
  },
  "domains": ["trending", "youtube", "scheduler", "tag", "common"]
}
```

---

## 저장 규칙 (프라이버시 보호)

### ✅ 저장 가능
```
- 파일 경로 (상대 경로)
- 도메인명, 테이블명
- 작업 유형, 패턴명
- 성공/실패 여부
- 프레임워크 버전
```

### ❌ 저장 금지
```
- 실제 코드 내용
- DB 접속 정보
- API 키 (YouTube, Claude)
- 서버 IP, 도메인
```

---

## API

### 초기화
```
context.init()
- .context/ 폴더 생성
- .gitignore에 .context/ 추가
- Serena로 프로젝트 구조 스캔 후 저장
```

### 도메인 정보
```
context.get_domain(domain_name)  → 도메인 메타데이터 반환
context.save_domain(domain_name, data)  → 저장
context.find_domain(keyword)  → 키워드로 검색
```

### 패턴
```
context.get_pattern(pattern_name)  → 패턴 정보 반환
context.find_similar_pattern(description)  → 유사 패턴 검색
context.update_pattern_usage(pattern_name, success)  → 사용 횟수 업데이트
```

### 이력
```
context.log_task(task_info)  → 작업 이력 기록
context.get_history(domain, days=30)  → 도메인별 최근 이력
context.find_similar_task(description)  → 유사 작업 검색
```
