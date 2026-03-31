---
name: code-generator
description: TrendRadar 프로젝트 템플릿 기반 자동 코드 생성. Spring Boot 도메인(Entity, Repository, Service, Controller, DTO) 및 Flyway 마이그레이션을 빠르게 생성한다.
---

# Code Generator (TrendRadar)

TrendRadar 프로젝트 패턴 기반 자동 코드 생성 에이전트.

## MCP 활용

- **Serena**: 기존 코드 패턴 분석
- **Context7**: 프레임워크 최신 문법 참조
- **Postgres (trendradar)**: 테이블 스키마 확인

---

## 지원 템플릿

### 1. 도메인 CRUD (Spring Boot)

```
generate crud --name {DomainName} --fields "field1:type,field2:type"
```

생성 파일:
```
backend/src/main/java/com/trendradar/{domain}/
├── domain/{DomainName}.java          # JPA Entity
├── repository/{DomainName}Repository.java
├── repository/{DomainName}RepositoryCustom.java    # QueryDSL
├── repository/{DomainName}RepositoryImpl.java
├── service/{DomainName}Service.java
├── controller/{DomainName}Controller.java
├── dto/{DomainName}Request.java
└── dto/{DomainName}Response.java
```

### 2. Flyway 마이그레이션

```
generate migration --name "create_{table_name}" --table "{table_name}"
```

생성 파일:
```
backend/src/main/resources/db/migration/
└── V{next}__create_{table_name}.sql
```

### 3. YouTube 수집 Job

```
generate collector --name {CollectorName} --endpoint "{youtube_api_endpoint}"
```

생성 파일:
```
backend/src/main/java/com/trendradar/youtube/client/{CollectorName}Client.java
backend/src/main/java/com/trendradar/youtube/dto/{CollectorName}Response.java
backend/src/main/java/com/trendradar/scheduler/{CollectorName}Scheduler.java
```

### 4. API 엔드포인트

```
generate api --domain {domain} --method {GET|POST|PUT|DELETE} --path "/api/v1/{path}"
```

---

## 템플릿 변수

### 공통 변수
```
{{DomainName}}      # PascalCase (TrendingVideo)
{{domainName}}      # camelCase (trendingVideo)
{{domain_name}}     # snake_case (trending_video)
{{DOMAIN_NAME}}     # UPPER_CASE (TRENDING_VIDEO)
```

---

## Entity 생성 규칙

```java
@Entity
@Table(name = "{{domain_name}}s")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class {{DomainName}} {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // fields...

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Builder
    private {{DomainName}}(/* params */) {
        // constructor
    }

    public static {{DomainName}} create(/* params */) {
        return {{DomainName}}.builder()
            // ...
            .build();
    }
}
```

- `@Setter` 사용 금지
- `@Builder`는 private 생성자에 적용
- 정적 팩토리 메서드 (`create()`) 패턴 사용
- 시간 필드는 `OffsetDateTime` 사용

## Repository 생성 규칙

```java
public interface {{DomainName}}Repository extends JpaRepository<{{DomainName}}, Long>,
        {{DomainName}}RepositoryCustom {
    // 단순 쿼리는 메서드 네이밍
}

public interface {{DomainName}}RepositoryCustom {
    // QueryDSL 동적 쿼리 메서드
}
```

## Service 생성 규칙

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class {{DomainName}}Service {

    private final {{DomainName}}Repository {{domainName}}Repository;

    // 조회 메서드: @Transactional(readOnly = true) 상속
    // 변경 메서드: @Transactional 명시
}
```

## Controller 생성 규칙

```java
@RestController
@RequestMapping("/api/v1/{{domain-name}}")
@RequiredArgsConstructor
public class {{DomainName}}Controller {

    private final {{DomainName}}Service {{domainName}}Service;

    // ApiResponse<T> 통일 반환
}
```

---

## 생성 프로세스

```
1. 패턴 확인
   - task-classifier에서 패턴 정보 수신
   - 또는 사용자 요청에서 도메인/필드 추출

2. 기존 코드 확인
   - Serena로 기존 도메인 패턴 분석
   - 네이밍 컨벤션 확인

3. 스키마 확인
   - Postgres MCP로 관련 테이블 존재 여부 확인
   - Flyway 마이그레이션 번호 확인

4. 코드 생성
   - 패키지 구조에 맞게 파일 생성
   - import 자동 추가

5. 검증
   - 컴파일 오류 체크
   - 기존 코드와 충돌 확인
```

---

## 다음 단계 연결

생성 완료 후:
- 테스트 작성 필요 시 → qa-tester
- 커스텀 로직 필요 시 → 직접 구현
