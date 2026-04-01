# TrendRadar - CLAUDE.md

이 파일은 Claude Code가 TrendRadar 프로젝트에서 작업할 때 참조하는 가이드입니다.

## 00. Claude 운영 규칙

### 언어 정책
1. **항상 한글 사용** - 모든 응답과 문서를 한글로 작성
2. **내부 처리는 영어** - thinking, 에이전트 프롬프트, Task 호출 시 영어 사용 (토큰 효율)

### 작업 전 계획 수립 (필수)
3. **수정/개발/리팩토링 작업 전 반드시:**
   - 대상: 수정 작업, 신규 도메인 개발, 리팩토링
   - 절차:
     1. `sequential-thinking` MCP로 깊이 있는 분석 수행
     2. 영향 범위, 수정 파일 목록, 예상 위험 요소 정리
     3. 단계별 실행 계획을 마크다운 표로 제시
     4. **사용자 승인 후에만 코드 수정 시작**
   - 단순 조회/확인 작업은 예외

### 배포 및 브라우저
4. **운영 배포는 사용자가 수행** - Claude는 배포 준비까지만, 실제 운영 배포는 사용자가 직접 수행
5. **브라우저 디버깅** - `agent-browser` CLI 사용. 사용자 명시적 요청 시에만 사용

### 트러블슈팅 자동 검색
6. **아래 상황 발생 시 `claude/analysis/troubleshooting/` 폴더 자동 검색:**
   - 같은 파일을 3번 이상 수정해도 문제 해결 안 될 때
   - 사용자가 "안 돼", "왜 안 되지", "이상해", "또 실패" 등의 표현 사용 시
   → 관련 키워드로 트러블슈팅 문서 검색 후 해결책 참조

### 복잡도 기반 처리
7. **단계별 사고 명시** - 복잡한 작업 시 분석→계획→실행→검증 과정을 명시적으로 표시
8. **Agent Teams 제안** - 복잡한 작업 감지 시:
   - 해당: 다수 파일 병렬 수정, Backend+Frontend 동시 개발
   → "Agent Teams으로 병렬 처리할까요?" 질문
   → 사용자 승인 후 팀 구성 및 실행
   → 단순 작업(Level 0~1)에는 제안하지 않음

### 키워드 감지
9. **아래 키워드 감지 시 해당 스킬 제안:**

   | 키워드 | 제안 스킬 |
   |--------|----------|
   | "보안", "취약점", "XSS" | ba-security |
   | "리팩토링", "정리", "최적화" | code-reviewer |
   | "전체 점검", "감사" | ba-security static |
   | "분석해줘", "analyze" | quick-analyzer → 적절한 스킬 체인 |
   | "느려", "성능", "로딩" | ba-requirements (성능 분석) |

   → 사용자 승인 후 실행

### 작업완료 키워드 감지
10. **"작업완료", "작업 완료", "완료 처리" 키워드 감지 시:**
    → 현재 작업 대상 확인
    → claude/analysis/domains/ 분석 문서 업데이트
    → 상태 완료로 변경
    → Git diff 기반 변경 내역 요약

---

## 01. 프로젝트 개요

**서비스명:** TrendRadar
**슬로건:** 세상의 트렌드를 레이더처럼 감지하다
**목적:** YouTube 데이터 기반 나라별·카테고리별 실시간 트렌드 분석 인텔리전스 플랫폼

### 핵심 기능
- 나라별 YouTube 트렌딩 TOP 50 실시간 수집 (1시간 주기)
- 알고리즘 태그 자동 부여 (급상승 / 신규진입 / 화제성 / 고참여율 / 롱런 / 글로벌 / 역주행)
- 나라별 · 카테고리별 필터링
- 조회수 추이 차트 시각화
- AI 기반 트렌드 브리핑 (Claude API)

### 타겟 사용자
- 크리에이터: 알고리즘 패턴 분석
- 마케터/브랜드: 트렌드 타이밍 포착
- 투자자: 시장 선행 신호
- 정치/여론 분석가: 여론 흐름 파악
- 언론/리서처: 이슈 발굴

---

## 02. 기술 스택

### 백엔드
- **Framework:** Spring Boot 3.5.0
- **Language:** Java 21 (Amazon Corretto)
- **ORM:** Spring Data JPA + QueryDSL 5.0
- **Database:** PostgreSQL 16
- **Migration:** Flyway
- **Scheduler:** Spring Scheduler (@Scheduled + @Async)
- **Build:** Gradle
- **SQL 로깅:** p6spy

### 프론트엔드
- **Framework:** React 18
- **UI:** shadcn/ui + Tailwind CSS
- **Charts:** Tremor + Recharts
- **State:** Zustand
- **HTTP:** TanStack Query + Axios
- **Build:** Vite

### 인프라
- **Server:** Vultr Cloud Compute (Seoul, 2vCPU/4GB/120GB)
  - IP: 158.247.241.196
  - SSH: `ssh root@158.247.241.196` (SSH 키 인증, 패스워드 불필요)
  - 도메인: trend-rada.com, api.trend-rada.com (SSL 적용)
- **Container:** Docker 29.3.1 + Docker Compose v5.1.1
- **Web Server:** Nginx (리버스 프록시, SSL Let's Encrypt)
- **Frontend Deploy:** Vercel (무료)

### 외부 API
- **YouTube Data API v3:** 트렌딩 데이터 수집 (10,000 유닛/일 무료)
- **Claude API:** 트렌드 요약 브리핑

---

## 03. TDD 원칙

### 기본 사이클 (Red-Green-Refactor)
```
1. Red      → 실패하는 테스트 먼저 작성
2. Green    → 테스트를 통과하는 최소한의 코드 작성
3. Refactor → 코드 품질 개선 (테스트는 계속 통과해야 함)
```

### 필수 규칙
- 테스트 없이 프로덕션 코드 작성 금지
- 하나의 테스트는 하나의 동작만 검증
- 테스트 메서드명: `메서드명_상황_기대결과` 형식
  - 예: `collectTrending_whenApiSuccess_returns50Videos`
- Given-When-Then 주석 구조 사용

### 테스트 커버리지 목표
- 서비스 레이어: 90% 이상
- 컨트롤러 레이어: 80% 이상
- 전체: 80% 이상 (Jacoco)

---

## 04. 테스트 하네스 (백엔드)

### 레이어별 테스트 전략

#### 단위 테스트 (Unit Test)
- 대상: Service, Util, Domain 클래스
- 도구: JUnit5 + Mockito + AssertJ
```java
@ExtendWith(MockitoExtension.class)
class TrendingServiceTest {
    @Mock private TrendingRepository trendingRepository;
    @InjectMocks private TrendingService trendingService;
}
```

#### 통합 테스트 (Integration Test)
- 대상: Repository, DB 쿼리
- 도구: Testcontainers (실제 PostgreSQL 16 컨테이너)
- **H2 인메모리 DB 사용 절대 금지**
```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Testcontainers
class TrendingVideoRepositoryTest {
    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16");
}
```

#### API 테스트 (Controller Test)
- 대상: REST API 엔드포인트
- 도구: MockMvc + RestAssured
```java
@WebMvcTest(TrendingController.class)
class TrendingControllerTest {
    @Test
    void getTrending_whenValidRequest_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/trending").param("country", "KR"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
}
```

#### 외부 API Mock (WireMock)
- 대상: YouTube Data API v3 호출
- Mock 응답 파일 위치: `src/test/resources/__files/`
```java
@SpringBootTest
@AutoConfigureWireMock(port = 0)
class YouTubeApiClientTest {
    @Test
    void fetchTrending_whenApiResponds_parsesCorrectly() {
        stubFor(get(urlPathEqualTo("/youtube/v3/videos"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBodyFile("youtube_trending_kr.json")));
    }
}
```

---

## 05. 테스트 하네스 (프론트엔드)

### 레이어별 테스트 전략

#### 단위 테스트 (Vitest)
- 대상: Zustand Store, Custom Hook, Util 함수

#### 컴포넌트 테스트 (React Testing Library)
- 대상: React 컴포넌트 렌더링 및 이벤트
- 구현 세부사항이 아닌 사용자 행동 기준으로 테스트

#### API Mock (MSW - Mock Service Worker)
- 백엔드 없이 프론트엔드 독립 개발/테스트 가능

#### E2E 테스트 (Cypress)
- 대상: 실제 브라우저 기반 주요 흐름
- `data-cy` 속성 필수 사용

---

## 06. 프로젝트 구조

### 백엔드
```
backend/
├── src/main/java/com/trendradar/
│   ├── TrendRadarApplication.java
│   ├── trending/              ← 트렌딩 도메인
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/        ← JPA + QueryDSL
│   │   ├── domain/
│   │   └── dto/
│   ├── youtube/               ← YouTube API 연동
│   │   ├── client/
│   │   └── dto/
│   ├── scheduler/             ← 수집 스케줄러
│   ├── tag/                   ← 알고리즘 태그
│   └── common/
│       ├── response/          ← ApiResponse<T>
│       ├── exception/         ← GlobalExceptionHandler
│       └── config/            ← WebConfig, AsyncConfig
├── src/main/resources/
│   ├── application.yml
│   ├── application-local.yml
│   ├── application-prod.yml
│   └── db/migration/         ← Flyway
├── src/test/
│   ├── java/com/trendradar/
│   └── resources/__files/     ← WireMock 응답 JSON
├── Dockerfile
└── build.gradle
```

### 프론트엔드
```
frontend/
├── src/
│   ├── components/
│   │   ├── trending/
│   │   ├── chart/
│   │   └── common/
│   ├── pages/
│   ├── stores/                ← Zustand
│   ├── hooks/                 ← Custom Hooks + TanStack Query
│   ├── api/                   ← Axios 인스턴스
│   ├── mocks/                 ← MSW 핸들러
│   ├── lib/                   ← shadcn/ui 유틸
│   └── utils/
├── cypress/e2e/
├── Dockerfile
└── package.json
```

---

## 07. DB 설계 원칙

### 네이밍 규칙
- 테이블명: snake_case 복수형
- 컬럼명: snake_case
- PK: id (BIGSERIAL)
- 시간 필드: TIMESTAMP WITH TIME ZONE
- video_id: VARCHAR(20) (YouTube 비디오 ID)
- country_code: VARCHAR(5) (ISO 코드)

### 주요 테이블
- `trending_videos` - 트렌딩 영상 데이터
- `view_snapshots` - 조회수 스냅샷 (시계열)
- `algorithm_tags` - 알고리즘 태그
- `countries` - 국가 마스터 (KR, US, JP, GB, DE)
- `flyway_schema_history` - 마이그레이션 이력

### Flyway 규칙
- 파일명: `V{번호}__{설명}.sql` (언더스코어 2개)
- 한번 적용된 파일 수정 금지
- 변경은 항상 새 마이그레이션 파일 추가

---

## 08. API 설계 원칙

### 표준 응답 형식 (ApiResponse)
```json
{ "success": true, "data": {}, "message": null }
{ "success": false, "data": null, "message": "에러 메시지" }
```

### 주요 엔드포인트
```
GET /api/v1/trending?country=KR&category=20&limit=10
GET /api/v1/trending/{videoId}/snapshots
GET /api/v1/countries
GET /api/v1/categories
GET /api/v1/briefing?country=KR
```

### HTTP 메서드 & 응답 코드

| 메서드 | 용도 | 코드 | 의미 |
|--------|------|------|------|
| GET | 조회 | 200 | 성공 |
| POST | 생성 | 201 | 생성됨 |
| PUT | 수정 | 200 | 성공 |
| DELETE | 삭제 | 200 | 성공 |
| - | - | 400 | 잘못된 요청 |
| - | - | 404 | 없음 |
| - | - | 429 | API 할당량 초과 |
| - | - | 500 | 서버 오류 |

---

## 09. 코딩 컨벤션

### Java (Backend)
- 클래스명: PascalCase / 메서드명: camelCase / 상수: UPPER_SNAKE_CASE
- `@Transactional(readOnly = true)`: 서비스 클래스에 기본 적용
- `@Transactional`: 변경 메서드에만 명시
- Entity를 Controller에서 직접 반환 금지 → DTO 변환 필수
- Lombok: `@RequiredArgsConstructor`, `@Getter`, `@Builder`, `@Slf4j`
- **`@Setter` 사용 금지** (불변 객체 지향)
- **`@Autowired` 필드 주입 금지** → 생성자 주입만
- 로깅: `log.info("message={}", value)` 파라미터 바인딩

### React (Frontend)
- 컴포넌트명: PascalCase (.tsx)
- 함수형 컴포넌트만 사용
- Custom Hook: `use` 접두사 필수
- TypeScript 사용 (.tsx, .ts)
- `data-cy` 속성 필수
- shadcn/ui 컴포넌트 우선 사용
- TanStack Query: 서버 상태 / Zustand: 클라이언트 상태

---

## 10. 비즈니스 로직

### 알고리즘 태그 조건
```
SURGE        → 24시간 조회수 증가량 >= 500,000
NEW_ENTRY    → 업로드 후 48시간 이내 TOP 50 진입
HOT_COMMENT  → (댓글수 / 조회수) 비율 상위 10%
HIGH_ENGAGE  → (좋아요수 / 조회수) 비율 상위 10%
LONG_RUN     → 7일 이상 연속 TOP 50 유지
GLOBAL       → 3개국 이상 동시 TOP 50 진입
COMEBACK     → 업로드 30일 이후 TOP 50 재진입
```

### 수집 스케줄러
- 1시간 주기: `@Scheduled(cron = "0 0 * * * *")`
- 5개국 병렬 수집: `@Async("trendingCollectExecutor")`
- 목표: 5개국 동시 수집 < 10초

### YouTube API 할당량
- 일일 무료: 10,000 유닛
- 5개국 × 24회 = 120 유닛/일 → 여유 충분
- 초과 시 수집 중단 + 로그 알림

---

## 11. N+1 Query 예방 (필수)

### 절대 금지
```java
// BAD: 반복문 안에서 DB 조회
for (TrendingVideo video : videoList) {
    AlgorithmTag tag = tagRepository.findByVideoId(video.getVideoId()); // N번 쿼리!
}
```

### 올바른 방법
```java
// GOOD: Batch Query + Map 변환
List<String> videoIds = videoList.stream()
    .map(TrendingVideo::getVideoId).toList();

List<AlgorithmTag> tags = tagRepository.findByVideoIdIn(videoIds); // 1번 쿼리

Map<String, List<AlgorithmTag>> tagMap = tags.stream()
    .collect(Collectors.groupingBy(AlgorithmTag::getVideoId));

for (TrendingVideo video : videoList) {
    List<AlgorithmTag> videoTags = tagMap.getOrDefault(video.getVideoId(), List.of());
}
```

### 목표 성능 기준
| 작업 | 목표 응답시간 |
|------|-------------|
| 단일 조회 | < 100ms |
| 리스트 조회 (10개) | < 300ms |
| 리스트 조회 (100개) | < 1000ms |
| 5개국 동시 수집 | < 10s |

---

## 12. 절대 금지 사항

1. **H2 인메모리 DB 사용 금지** → Testcontainers 사용
2. **`@Setter` 사용 금지** → 불변 객체 + Builder
3. **`@Autowired` 필드 주입 금지** → 생성자 주입
4. **N+1 Query 금지** → Batch Query 필수
5. **Entity 직접 반환 금지** → DTO 변환
6. **API 키 하드코딩 금지** → 환경변수
7. **`--no-verify` 사용 금지** (Git 커밋)
8. **운영 서버 직접 배포 금지** → 사용자가 직접 수행
9. **자동 커밋 금지** → 항상 사용자 승인 필요

---

## 13. Git 규칙

### Claude 커밋 규칙

| 상황 | Claude 커밋 | 비고 |
|------|:-----------:|------|
| 사용자가 명시적 요청 | ✅ 가능 | "커밋해줘", "푸시해줘" |
| 문서 작업 완료 후 | ⚠️ 확인 필요 | "커밋할까요?" 질문 |
| 코드 수정 완료 후 | ⚠️ 확인 필요 | "커밋할까요?" 질문 |
| 자동 커밋 | ❌ 금지 | 항상 사용자 승인 필요 |

**Claude 커밋 시 필수:**
1. 커밋 전 "커밋할까요?" 확인
2. 변경 내용 요약 표시
3. 커밋 메시지 제안
4. 사용자 승인 후 커밋

### 커밋 단위 규칙

**원칙: 하나의 커밋 = 하나의 논리적 변경**

| 좋은 예 ✅ | 나쁜 예 ❌ |
|-----------|-----------|
| 기능별로 분리 | 여러 기능 한 커밋에 |
| 관련 파일만 포함 | 무관한 파일 포함 |
| 완료된 작업만 | 미완성 코드 |

**커밋 크기 기준:**
- 파일 수: 1~20개 권장 (최대 50개)
- 변경 라인: 500줄 이하 권장
- 너무 크면 분리 검토

**분리 기준:**
```
1. Backend / Frontend 분리
2. 기능 추가 / 버그 수정 분리
3. 리팩토링 / 신규 코드 분리
4. 문서 / 코드 분리
```

### 브랜치 보호 규칙

| 규칙 | main | develop | feature/* |
|------|:----:|:-------:|:---------:|
| 직접 푸시 | ❌ 금지 | ⚠️ 주의 | ✅ 허용 |
| PR 필수 | ✅ 권장 | - | - |
| 코드 리뷰 | ✅ 권장 | - | - |
| Force Push | ❌ 절대 금지 | ❌ 금지 | ⚠️ 주의 |

### 커밋 메시지 규칙
```
<type>: <subject>

[optional body]

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>
```

| 타입 | 설명 | 예시 |
|------|------|------|
| `feat` | 신규 기능 | `feat: 트렌딩 수집 스케줄러 구현` |
| `fix` | 버그 수정 | `fix: YouTube API 파싱 오류 수정` |
| `refactor` | 리팩토링 | `refactor: TrendingService 쿼리 최적화` |
| `test` | 테스트 추가 | `test: 알고리즘 태그 서비스 테스트` |
| `docs` | 문서 수정 | `docs: API 명세 업데이트` |
| `chore` | 빌드/설정 | `chore: Gradle 의존성 업데이트` |

### 브랜치 구조

| 브랜치 | 용도 |
|--------|------|
| `main` | 운영 배포 (보호됨) |
| `develop` | 개발 통합 |
| `feature/*` | 신규 기능 |
| `bugfix/*` | 버그 수정 |
| `hotfix/*` | 긴급 수정 |

### 작업 흐름
```
1. develop에서 feature 브랜치 생성
2. TDD로 기능 개발 및 커밋
3. develop으로 PR 생성
4. 코드 리뷰 후 머지
5. release 브랜치로 배포 준비
6. main으로 머지 (운영 배포)
```

---

## 14. 페르소나 시스템

### TrendRadar 전용 페르소나

| 페르소나 | 전문 영역 | 자동 활성화 |
|----------|----------|------------|
| `--persona-spring` | Spring Boot 3.5, JPA, QueryDSL, N+1 해결 | `*Service.java`, `*Controller.java` |
| `--persona-database` | PostgreSQL 16, 쿼리 최적화, Flyway | `*.sql`, `*Repository.java` |
| `--persona-react` | React 18, shadcn/ui, Zustand, TanStack Query | `*.tsx`, `*.ts` (frontend/) |
| `--persona-youtube` | YouTube Data API v3, 수집/파싱 | `*Client.java`, `*Scheduler.java` |
| `--persona-api` | RESTful 설계, ApiResponse, 유효성 검증 | `*Controller.java`, `/api/**` |

### 협업 패턴

| 작업 | 페르소나 조합 |
|------|-------------|
| **도메인 개발** | spring + database |
| **API 개발** | spring + api |
| **수집 로직** | youtube + spring + database |
| **프론트 개발** | react |
| **성능 최적화** | database + spring |
| **Full-Stack** | spring + database + react + api |

---

## 15. 에이전트 워크플로우

### 3계층 에이전트 아키텍처

```
┌─────────────────────────────────────────────────────────┐
│              🔍 분석 계층 (Analysis Layer)                │
│   quick-analyzer, ba-requirements, ba-security           │
│   → 작업 전 현황 파악, 복잡도 판정, 요구사항 분석          │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│              ⚡ 실행 계층 (Execution Layer)               │
│   system-architect, code-generator                       │
│   → 설계, 코드 생성, 구현                                 │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│              ✅ 검증 계층 (Verification Layer)            │
│   qa-tester, code-reviewer                               │
│   → 테스트, 품질 검증, 보안 체크                           │
└─────────────────────────────────────────────────────────┘
```

### 작업별 에이전트 워크플로우

| 작업 유형 | 에이전트 체인 |
|----------|-------------|
| **도메인 개발** | `ba-requirements` → `system-architect` → 구현 → `qa-tester` |
| **API 개발** | `ba-requirements` → 구현 → `code-reviewer` |
| **수집 로직** | `ba-requirements` → 구현 → `qa-tester` |
| **성능 최적화** | `code-reviewer` → 분석 → 구현 → `qa-tester` |
| **버그 수정** | 트러블슈팅 검색 → 구현 → `qa-tester` |
| **보안 점검** | `ba-security` → 보고서 생성 |

### 파일 패턴 → 페르소나/에이전트 매핑

```
*Service.java          → persona-spring, ba-requirements
*Controller.java       → persona-api, code-reviewer
*Repository.java       → persona-database
*Client.java           → persona-youtube
*Scheduler.java        → persona-youtube, persona-spring
*.sql                  → persona-database
*.tsx / *.ts           → persona-react
build.gradle           → 빌드/설정 전문
application*.yml       → 설정 전문
V*__.sql (Flyway)      → persona-database
```

---

## 16. 작업별 문서/스킬 트리거

> **원칙**: 필요한 문서만 필요할 때 로드 (토큰 최적화)

| 작업 유형 | 실행할 스킬/문서 | 로드되는 내용 |
|----------|-----------------|-------------|
| **도메인 개발** | `.claude/skills/java-springboot/SKILL.md` | Entity/Repo/Service 패턴 |
| **API 개발** | `claude/analysis/_PATTERNS.md` → controller 패턴 | ApiResponse, 유효성 검증 |
| **수집 로직 개발** | `claude/analysis/patterns/youtube-client.md` | YouTube API 호출/파싱 |
| **테스트 작성** | `.claude/skills/qa-tester/SKILL.md` | TDD, Testcontainers, WireMock |
| **성능 최적화** | `CLAUDE.md` 섹션 11 (N+1) | Batch Query 패턴 |
| **버그/문제 해결** | `claude/analysis/troubleshooting/` | 기존 사례, 디버깅 체크리스트 |
| **보안 점검** | `.claude/skills/ba-security/SKILL.md` | OWASP, 정적 분석 |
| **코드 리뷰** | `.claude/skills/code-reviewer/SKILL.md` | 리뷰 레벨, 체크포인트 |
| **Obsidian 문서** | `.claude/skills/bt-obsidian/SKILL.md` | 템플릿, Vault 경로 |

### 문서 역할 분리

```
CLAUDE.md (항상 로드)
├── 핵심 규칙, 절대 금지 사항
├── 기술 스택, 서버 정보
├── N+1 예방, 코딩 컨벤션
├── MCP 작업별 조합
└── 스킬 트리거 테이블 ← 여기서 판단

.claude/skills/ (필요시 로드)
├── quick-analyzer     ← 복잡도 판정
├── java-springboot    ← Spring Boot 패턴
├── code-generator     ← 템플릿 생성
└── ...

claude/analysis/ (필요시 로드)
├── _PATTERNS.md       ← 코드 패턴 인덱스
├── domains/           ← 도메인 분석 문서
├── patterns/          ← 패턴 상세
└── troubleshooting/   ← 문제 해결 사례
```

---

## 17. 서브에이전트 & 스킬 통합

### 활성화된 서브에이전트

| 에이전트 | 용도 |
|----------|------|
| `feature-dev:code-reviewer` | 코드 리뷰 (버그, 보안, 품질) |
| `feature-dev:code-architect` | 기능 아키텍처 설계 |
| `feature-dev:code-explorer` | 코드베이스 분석 |
| `code-simplifier` | 복잡한 코드 단순화 |
| `pr-review-toolkit:*` | PR 리뷰 (6개 에이전트) |
| `coderabbit:code-reviewer` | CodeRabbit AI 코드 리뷰 |

### 활성화된 글로벌 스킬

| 스킬 | 호출 | 용도 |
|------|------|------|
| superpowers:brainstorming | 자동 | 기능 설계 전 브레인스토밍 |
| superpowers:writing-plans | 자동 | 구현 계획 작성 |
| superpowers:executing-plans | 자동 | 계획 실행 |
| superpowers:test-driven-development | 자동 | TDD 워크플로우 |
| superpowers:systematic-debugging | 자동 | 체계적 디버깅 |
| superpowers:verification-before-completion | 자동 | 완료 전 검증 |
| frontend-design | `/frontend-design` | 고품질 UI 생성 |
| document-skills | `/docx`, `/pdf`, `/xlsx` | 문서 생성 |
| claude-api | 자동 | Claude API 연동 코드 |

### SuperClaude 명령어 (/sc:*)

| 명령어 | 용도 |
|--------|------|
| `/sc:analyze` | 코드/시스템 분석 |
| `/sc:implement` | 기능 구현 |
| `/sc:improve` | 코드 개선 |
| `/sc:test` | 테스트 실행 |
| `/sc:build` | 프로젝트 빌드 |
| `/sc:git` | Git 작업 |

### TrendRadar 커맨드 체계

#### 분석/조회 계열

| 동작 | 설명 |
|------|------|
| 복잡도 판정 | quick-analyzer 스킬로 Level 0~3/D 판정 |
| 도메인 분석 | ba-requirements → Serena + Postgres MCP |
| 보안 점검 | ba-security 스킬 실행 |
| 트러블슈팅 | `claude/analysis/troubleshooting/` 검색 |

#### 작업 관리 계열

| 동작 | 설명 |
|------|------|
| 작업 완료 | 분석 문서 업데이트 + 상태 완료 변경 |
| 다음 계획 | `claude/worklog/_NEXT_PLAN.md` 저장 |
| 이전 작업 확인 | 세션 시작 시 `_NEXT_PLAN.md` 확인 |

#### 워크플로우별 조합

| 작업 유형 | 플로우 |
|----------|--------|
| **도메인 개발** | 분석 → TDD (테스트 먼저) → 구현 → 리뷰 → 완료 |
| **리팩토링** | code-reviewer 분석 → 계획 승인 → 수정 → 테스트 → 완료 |
| **버그 수정** | 트러블슈팅 검색 → 원인 분석 → 수정 → 테스트 → 완료 |
| **성능 최적화** | N+1 체크 → 쿼리 분석 → 수정 → 벤치마크 → 완료 |

---

## 18. MCP 서버 & 도구

### 활성화된 서버

| 서버 | 용도 |
|------|------|
| **serena** | 코드베이스 분석, 심볼 검색 |
| **postgres-trendradar** | trendradar DB 쿼리 |
| **context7** | 라이브러리 문서 참조 |
| **sequential-thinking** | 다단계 분석 |

### 작업별 MCP 조합

| 작업 | 추천 조합 |
|------|----------|
| **도메인 개발** | serena + postgres-trendradar + context7 |
| **API 개발** | serena + sequential-thinking |
| **수집 로직** | postgres-trendradar + context7 |
| **디버깅** | serena + postgres-trendradar + sequential-thinking |
| **문서 작성** | serena + postgres-trendradar |

---

## 19. 세션 규칙

### 세션 시작 시

```
1. Serena 프로젝트 활성화: activate_project → trendradar
2. 이전 작업 계획 확인: claude/worklog/_NEXT_PLAN.md
   → 계획이 있으면: "📋 이전 세션에서 계획한 작업이 있습니다. 이어서 진행할까요?"
```

### 세션 종료 시

| 상황 | 동작 |
|------|------|
| 이어서 할 작업 있음 | `claude/worklog/_NEXT_PLAN.md` 업데이트 |
| 작업 완료됨 | 기록 불필요 |

---

## 20. Smart Dev Team 스킬 (복잡도 기반 라우팅)

> **위치**: `.claude/skills/`

### 복잡도 판정 (모든 작업 시작 시)

`.claude/skills/quick-analyzer/SKILL.md` 참조

| Level | 이름 | 트리거 키워드 |
|:-----:|------|-------------|
| 0 | 즉시 실행 | 설정 변경, 상수 수정, yml |
| 1 | 간소화 | 필드 추가, 인덱스, 단순 API |
| 2 | 표준 | 수집 로직, 성능, 버그 수정 |
| 3 | 전체 | 새 도메인, YouTube 연동, 아키텍처 |
| D | 리팩토링 | 정리, 최적화, 중복 제거 |

### 레벨별 플로우

```
Level 0 → 직접 수정 → 완료
Level 1 → task-classifier → code-generator → 완료
Level 2 → ba-requirements → 구현 → qa-tester → 완료
Level 3 → ba-requirements → system-architect → 구현 → qa-tester → 완료
Level D → code-reviewer → 구현 → qa-tester → 완료
```

### 스킬 전체 목록 (13개)

| # | 스킬 | 역할 |
|:-:|------|------|
| 1 | quick-analyzer | 복잡도 판정 + 라우팅 |
| 2 | task-classifier | 패턴 매칭 + 이력 조회 |
| 3 | pm-coordinator | 작업 배분 + 우선순위 |
| 4 | ba-requirements | 요구사항 분석 (MCP 분석 → 질문) |
| 5 | system-architect | 신규 개발 설계 |
| 6 | code-generator | 템플릿 기반 코드 생성 |
| 7 | qa-tester | 테스트 + 검증 |
| 8 | code-reviewer | 코드 리뷰 + 리팩토링 |
| 9 | ba-security | 보안 점검 |
| 10 | local-context-manager | 로컬 컨텍스트 관리 |
| 11 | bt-obsidian | Obsidian 문서 생성 |
| 12 | java-springboot | Spring Boot 베스트 프랙티스 |
| 13 | find-skills | 스킬 검색/설치 |

### 로컬 컨텍스트 활용

> **위치**: `.context/` (Git ignore)

| 폴더 | 용도 |
|------|------|
| `project_structure.json` | 프로젝트 구조, 경로 |
| `domains/` | 도메인별 메타데이터, 수정 이력 |
| `patterns/` | 작업 패턴, 성공률 |
| `history/` | 일별 작업 이력 |

---

## 21. 단계별 개발 계획

> ⚠️ 각 단계 완료 후 반드시 테스트 통과 확인 후 다음 단계 진행
> ⚠️ 각 단계 시작 전 sequential-thinking MCP로 분석 후 사용자 승인

| Phase | 내용 | 상태 |
|-------|------|:----:|
| 0 | 인프라 준비 (로컬 환경) | ✅ 완료 |
| 1 | Spring Boot 초기화 | ✅ 완료 |
| 2 | 도메인 설계 (Entity, Repository, DTO) - TDD | ⬜ 대기 |
| 3 | YouTube API 연동 - TDD | ⬜ 대기 |
| 4 | 스케줄러 + 알고리즘 태그 - TDD | ⬜ 대기 |
| 5 | REST API 구현 - TDD | ⬜ 대기 |
| 6 | 백엔드 Docker 빌드 | ⬜ 대기 |
| 7 | React 초기화 | ⬜ 대기 |
| 8 | 프론트 화면 구현 - TDD | ⬜ 대기 |
| 9 | 프론트 Docker 빌드 | ⬜ 대기 |
| 10 | 통합 테스트 & 연동 | ⬜ 대기 |
| 11 | Vultr 서버 배포 | ⬜ 대기 |
| 12 | 검증 및 마무리 | ⬜ 대기 |

---

## 22. 개발 시작 전 체크리스트

- [ ] 예상 데이터 규모 파악
- [ ] 반복 처리가 필요한 로직인가?
- [ ] N+1 Query 발생 가능성 검토
- [ ] YouTube API 할당량 영향 확인
- [ ] Flyway 마이그레이션 번호 충돌 확인

## 23. 코드 리뷰 체크리스트

### Backend
- [ ] N+1 Query 없음
- [ ] Batch Query 사용 (`findByXxxIn()`)
- [ ] `@Transactional(readOnly=true)` 적절히 사용
- [ ] ApiResponse 통일 반환
- [ ] Entity 직접 반환 없음 (DTO 변환)
- [ ] `@Setter` 미사용
- [ ] API 키 하드코딩 없음
- [ ] 로깅에 민감 정보 없음

### Frontend
- [ ] TypeScript 타입 정의 (`any` 최소화)
- [ ] `data-cy` 속성 포함
- [ ] 에러 처리 (API 호출)
- [ ] 로딩 상태 표시
- [ ] shadcn/ui 컴포넌트 사용

### 공통
- [ ] 성능 기준 충족
- [ ] 테스트 작성 완료
- [ ] Jacoco 커버리지 80%+
