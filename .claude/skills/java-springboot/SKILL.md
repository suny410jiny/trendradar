---
name: java-springboot
description: 'TrendRadar 프로젝트 Spring Boot 개발 가이드 및 베스트 프랙티스'
---

# TrendRadar Spring Boot Best Practices

TrendRadar 프로젝트(YouTube Trend Intelligence Platform)에 특화된 Spring Boot 개발 가이드.

## 프로젝트 기술 스택

- **Spring Boot 3.5.0** / Java 21
- **Gradle** (build.gradle)
- **PostgreSQL 16** + Flyway 마이그레이션
- **QueryDSL 5.0** (동적 쿼리)
- **p6spy** (SQL 로깅)
- **Lombok** (@Slf4j, @Getter, @Builder 등)
- **Jacoco** (커버리지 80% 이상)

## 패키지 구조 (도메인 기반)

```
com.trendradar/
├── common/
│   ├── response/      # ApiResponse 공통 응답
│   ├── exception/     # GlobalExceptionHandler
│   └── config/        # WebConfig, AsyncConfig
├── trending/
│   ├── controller/    # REST API
│   ├── service/       # 비즈니스 로직
│   ├── repository/    # JPA + QueryDSL
│   ├── domain/        # JPA Entity
│   └── dto/           # Request/Response DTO
├── youtube/
│   ├── client/        # YouTube Data API 클라이언트
│   └── dto/           # YouTube API 응답 매핑
├── scheduler/         # 수집 스케줄러
└── tag/               # 알고리즘 태그
```

- 새 도메인 추가 시 동일 구조 (controller/service/repository/domain/dto) 유지
- 도메인 간 의존은 service → service, repository 직접 참조 금지

## Dependency Injection

- **생성자 주입만 사용**, `@Autowired` 필드 주입 금지
- 의존성 필드는 `private final`
- 단일 생성자면 `@RequiredArgsConstructor` (Lombok) 활용

```java
@Service
@RequiredArgsConstructor
public class TrendingService {
    private final TrendingRepository trendingRepository;
    private final YouTubeClient youTubeClient;
}
```

## 설정 (Configuration)

- `application.yml` 공통, `application-local.yml` / `application-prod.yml` 환경별 분리
- 민감 정보(API 키, DB 비밀번호)는 `.env` 또는 환경변수로 관리, 절대 커밋 금지
- `@ConfigurationProperties`로 타입 안전한 설정 바인딩

## 컨트롤러 (Web Layer)

- 모든 API 경로는 `/api/v1/` 접두사 사용
- `ApiResponse<T>`로 일관된 응답 형태 유지
- JPA Entity를 직접 반환하지 않고 **DTO** 사용
- `@Valid`로 요청 검증, `GlobalExceptionHandler`에서 일괄 처리

```java
@RestController
@RequestMapping("/api/v1/trending")
@RequiredArgsConstructor
public class TrendingController {

    private final TrendingService trendingService;

    @GetMapping
    public ApiResponse<List<TrendingVideoResponse>> getTrending(
            @RequestParam String countryCode) {
        return ApiResponse.of(trendingService.getTrending(countryCode));
    }
}
```

## 서비스 (Service Layer)

- 비즈니스 로직은 `@Service`에 집중, 컨트롤러는 위임만
- `@Transactional(readOnly = true)` 조회, `@Transactional` 변경
- 외부 API 호출(YouTube)은 별도 client 클래스로 분리

## 데이터 (Repository Layer)

- Spring Data JPA + QueryDSL 조합
- 단순 쿼리: `JpaRepository` 메서드 네이밍
- 복잡한 동적 쿼리: `QueryDSL` 사용 (CustomRepository 패턴)
- DTO Projection으로 필요한 컬럼만 조회

```java
public interface TrendingRepository extends JpaRepository<TrendingVideo, Long>,
        TrendingRepositoryCustom {
}

public interface TrendingRepositoryCustom {
    List<TrendingVideo> findByCondition(TrendingSearchCondition condition);
}
```

## Flyway 마이그레이션

- 파일명: `V{번호}__{설명}.sql` (언더스코어 2개)
- 한번 적용된 마이그레이션 파일 수정 금지
- 스키마 변경은 항상 새 마이그레이션 파일 추가
- `src/main/resources/db/migration/` 위치

## 로깅

- Lombok `@Slf4j` 사용 (직접 Logger 선언 금지)
- 파라미터 바인딩: `log.info("Collecting trending for country={}", countryCode);`
- SQL 로깅은 p6spy가 처리, hibernate show-sql 사용 금지

## 비동기 처리

- YouTube 수집 등 I/O 작업은 `@Async("trendingCollectExecutor")` 활용
- 비동기 메서드는 반드시 `CompletableFuture` 또는 `void` 반환
- 트랜잭션과 비동기 혼용 주의 (별도 트랜잭션으로 처리)

## 테스트

- **단위 테스트**: JUnit 5 + Mockito, `@ExtendWith(MockitoExtension.class)`
- **통합 테스트**: `@SpringBootTest` + Testcontainers (PostgreSQL)
- **API 테스트**: `@WebMvcTest` + MockMvc 또는 RestAssured
- **외부 API 모킹**: WireMock으로 YouTube API 응답 모킹
- **TDD 원칙**: 테스트 먼저 작성 → 구현 → 리팩토링
- **커버리지**: Jacoco 80% 이상 유지, dto/config 제외

```java
@DisplayName("트렌딩 서비스 테스트")
@ExtendWith(MockitoExtension.class)
class TrendingServiceTest {

    @InjectMocks
    private TrendingService trendingService;

    @Mock
    private TrendingRepository trendingRepository;

    @Test
    @DisplayName("국가별 트렌딩 조회 시 결과 반환")
    void getTrending_returnsResults() {
        // Given - When - Then
    }
}
```

## 보안

- API 키는 환경변수로 주입, 코드에 하드코딩 금지
- Spring Data JPA / QueryDSL 사용으로 SQL Injection 방지
- CORS 설정은 `WebConfig`에서 허용 Origin 명시
- 추후 Spring Security 도입 시 JWT 기반 인증 권장

## 코드 스타일

- Lombok 적극 활용: `@Getter`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j`
- `@Setter` 사용 금지 (불변 객체 지향)
- Entity에 `@Builder` 사용 시 정적 팩토리 메서드 패턴 병행
- 메서드명은 동사로 시작: `collectTrending()`, `calculateTags()`
