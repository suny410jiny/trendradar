---
name: bt-obsidian
description: Obsidian Vault에 TrendRadar 프로젝트 문서를 생성하는 종합 스킬. 노트, Canvas, Base 파일을 템플릿 기반으로 생성한다.
---

# BT-Obsidian (TrendRadar)

Obsidian Vault에 프로젝트 문서를 생성하는 종합 스킬.

## 기본 설정

```
Vault 경로: /Users/specialtrailstory/Library/Mobile Documents/iCloud~md~obsidian/Documents/Trace-Story
프로젝트 폴더: trendradar-project
```

## 사용법

### 기본 노트 생성

```bash
/bt-obsidian [폴더] [제목]
```

| 예시 | 결과 |
|------|------|
| `/bt-obsidian backend 트렌딩수집API` | `backend/트렌딩수집API.md` |
| `/bt-obsidian scheduler YouTube수집배치` | `scheduler/YouTube수집배치.md` |
| `/bt-obsidian . 프로젝트개요` | `프로젝트개요.md` (루트) |

### 템플릿 사용

```bash
/bt-obsidian [폴더] [제목] --template [템플릿명]
```

| 템플릿 | 용도 | 포함 섹션 |
|--------|------|-----------|
| `api` | API 명세 | 엔드포인트, 요청/응답, 에러코드 |
| `service` | 서비스 분석 | 메서드, 의존성, 쿼리, 트랜잭션 |
| `meeting` | 회의록 | 참석자, 안건, 결정사항, 액션아이템 |
| `trouble` | 트러블슈팅 | 증상, 원인, 해결방법, 예방책 |
| `feature` | 기능 명세 | 요구사항, 설계, 구현계획, 테스트 |
| `plan-feature` | 신규 개발 계획 | 목표, 설계, 구현계획, 완료조건 |
| `plan-improve` | 수정/개선 계획 | 현재상태, 변경사항, 영향범위 |
| `plan-bugfix` | 버그 수정 계획 | 증상, 원인분석, 해결계획 |
| `domain` | 도메인 분석 | Entity, Repository, Service, API |
| `collector` | 수집기 분석 | YouTube API, 스케줄러, 데이터 흐름 |

### Canvas 생성

```bash
/bt-obsidian canvas [제목]
```

### Base 파일 생성

```bash
/bt-obsidian base [제목]
```

---

## TrendRadar 전용 템플릿

### domain (도메인 분석)

```markdown
# {도메인명}

> TrendRadar 도메인 분석 문서

## 개요
- **패키지**: com.trendradar.{domain}
- **상태**: 분석중 | 완료

## Entity
- 테이블: {table_name}
- 주요 필드:

| 필드 | 타입 | 설명 |
|------|------|------|
| | | |

## Repository
- JPA: 기본 CRUD
- QueryDSL: 동적 쿼리

## Service
- 주요 메서드:

| 메서드 | 설명 | 트랜잭션 |
|--------|------|----------|
| | | readOnly |

## API
| Method | Path | 설명 |
|--------|------|------|
| | /api/v1/{domain} | |

## 관련 문서
- [[]]
```

### collector (수집기 분석)

```markdown
# {수집기명}

> YouTube 데이터 수집기 분석

## 개요
- **YouTube API**: {endpoint}
- **수집 주기**: {interval}
- **대상 국가**: KR, US, JP, GB, DE

## 데이터 흐름
```
YouTube API → Client → Service → Repository → DB
```

## 스케줄러
- Cron: {expression}
- 비동기: @Async("trendingCollectExecutor")

## 저장 테이블
- {table_name}

## 에러 처리
- API 할당량 초과:
- 네트워크 오류:
- 파싱 실패:

## 관련 문서
- [[]]
```

---

## 실행 프로세스

```
1. 명령어 파싱 (폴더, 제목, 옵션)
2. 경로: Vault/trendradar-project/{폴더}/{제목}.md
3. 템플릿 선택 및 변수 치환
4. 파일 생성
5. 경로 출력 + wikilink 제공
```
