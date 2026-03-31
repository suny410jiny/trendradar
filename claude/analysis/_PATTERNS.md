# TrendRadar 패턴 인덱스

> 작업 유형에 따라 필요한 패턴만 참조
> 마지막 업데이트: 2026-04-01

---

## 📂 패턴 구조

```
patterns/
├── backend-entity.md         ← Entity, Builder, 정적 팩토리
├── backend-repository.md     ← JPA + QueryDSL, Batch Query
├── backend-service.md        ← Service, 트랜잭션, 비동기
├── backend-controller.md     ← REST API, ApiResponse, 유효성 검증
├── backend-test.md           ← TDD, Testcontainers, WireMock
└── youtube-client.md         ← YouTube API 호출, 파싱, 에러 처리
```

---

## 🎯 키워드 기반 패턴 매핑

### Backend

| 키워드 | 로드할 파일 |
|--------|------------|
| Entity, 도메인, Builder, 생성 | `patterns/backend-entity.md` |
| 쿼리, N+1, Batch, QueryDSL, Repository | `patterns/backend-repository.md` |
| Service, 트랜잭션, 비동기, @Async | `patterns/backend-service.md` |
| Controller, API, 엔드포인트, ApiResponse | `patterns/backend-controller.md` |
| 테스트, TDD, Mock, Testcontainers, WireMock | `patterns/backend-test.md` |
| YouTube, 수집, API 호출, 파싱 | `patterns/youtube-client.md` |

### Frontend (추후 생성)

| 키워드 | 로드할 파일 |
|--------|------------|
| 컴포넌트, React, shadcn | `patterns/frontend-component.md` |
| API, TanStack Query, Axios | `patterns/frontend-api.md` |
| 차트, Recharts, Tremor | `patterns/frontend-chart.md` |
| Store, Zustand | `patterns/frontend-store.md` |

---

## 패턴 작성 원칙

1. 실제 TrendRadar 코드에서 검증된 패턴만 등록
2. 안티패턴도 함께 기록 (❌ BAD → ✅ GOOD)
3. 패턴 사용 시 성공/실패 이력 기록 (local-context-manager 연동)
