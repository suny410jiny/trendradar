---
name: ba-security
description: TrendRadar 보안 종합 점검. Backend 정적 분석(API 키 노출, SQL injection, CORS, 인증/인가) 중심. 추후 Frontend 추가 시 런타임 검사 확장. Grep 도구 사용.
---

# BA Security - 보안 종합 점검

**2단계 보안 점검**: 런타임 검사 (agent-browser) + 정적 코드 분석 (Grep/Read)
**결과는 `claude/analysis/audit/_SECURITY.md`에 별도 기록한다** (일반 감사 파일이 아닌 별도).

## 사전 조건

- agent-browser 설치 필요
- browser-audit 메인에서 호출 시: 이미 페이지 접속 상태
- 단독 실행 시: 사용자에게 URL 질문 후 접속

## eval 규칙

> **중요**: `agent-browser eval`에서 `return` 문을 직접 사용할 수 없다.
> 반드시 IIFE `(() => { ... })()` 로 감싸야 한다.

## 실행 명령어

### 1. 쿠키 보안 플래그

```bash
agent-browser cookies get
```

→ 각 쿠키의 `httpOnly`, `secure`, `sameSite` 플래그 확인
→ 인증 관련 쿠키(token, session)에 플래그 누락 시 Critical

### 2. localStorage 민감정보

```bash
agent-browser eval "
  const items = {};
  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i);
    const val = localStorage.getItem(key);
    const sensitive = /token|jwt|password|secret|key|auth/i.test(key) ||
                      /^eyJ/.test(val);
    if (sensitive) items[key] = val.substring(0, 20) + '...';
  }
  return { sensitiveItems: items, totalItems: localStorage.length };
"
```

### 3. sessionStorage 민감정보

```bash
agent-browser eval "
  const items = {};
  for (let i = 0; i < sessionStorage.length; i++) {
    const key = sessionStorage.key(i);
    const val = sessionStorage.getItem(key);
    const sensitive = /token|jwt|password|secret|key|auth/i.test(key) ||
                      /^eyJ/.test(val);
    if (sensitive) items[key] = val.substring(0, 20) + '...';
  }
  return { sensitiveItems: items, totalItems: sessionStorage.length };
"
```

### 4. 콘솔 정보 유출

```bash
agent-browser console
```

→ 콘솔 로그에서 토큰/키/비밀번호 패턴 감지
→ 프로덕션 환경에서 디버그 로그 노출 확인

### 5. 혼합 콘텐츠 (Mixed Content)

```bash
agent-browser eval "
  const insecure = performance.getEntriesByType('resource')
    .filter(r => r.name.startsWith('http://'));
  return insecure.map(r => r.name);
"
```

### 6. 비밀번호 필드 자동완성

```bash
agent-browser eval "
  const pwFields = document.querySelectorAll('input[type=password]');
  return Array.from(pwFields).map(f => ({
    autocomplete: f.autocomplete,
    name: f.name,
    id: f.id
  }));
"
```

---

## Phase 2: 정적 코드 분석

> Grep/Read 도구로 소스 코드를 직접 검사한다. agent-browser 불필요.

### 실행 범위 결정

- **대상 화면 지정 시**: 해당 Vue 파일 + 연관 Backend 파일만
- **전체 점검 시**: `front-admin/src/`, `front-user/src/`, `backend-api/src/`

---

### A. Frontend 정적 분석 (XSS/DOM 패턴)

#### A1. Vue XSS 취약점 (CRITICAL)

```
검색 패턴: v-html
대상: front-admin/src/**/*.vue, front-user/src/**/*.vue
```

→ `v-html`에 사용자 입력이 바인딩되면 XSS 공격 가능
→ 허용: 관리자가 작성한 콘텐츠 (TiptapEditor 출력 등)
→ 금지: 사용자 입력값 직접 바인딩

#### A2. 직접 DOM 조작 (HIGH)

```
검색 패턴: innerHTML|outerHTML|document\.write|insertAdjacentHTML
대상: front-admin/src/**/*.{vue,ts,js}
```

→ DOM 직접 조작은 XSS 우회 경로
→ Vue 반응성 시스템 사용으로 대체 권장

#### A3. 코드 실행 패턴 (CRITICAL)

```
검색 패턴: \beval\s*\(|new\s+Function\s*\(|setTimeout\s*\(\s*['"`]|setInterval\s*\(\s*['"`]
대상: front-admin/src/**/*.{vue,ts,js}
```

→ 문자열을 코드로 실행하는 패턴 = injection 경로
→ eval 대신 JSON.parse, Function 대신 명시적 로직 사용

#### A4. URL 기반 injection (HIGH)

```
검색 패턴: location\.href\s*=|location\.replace\s*\(|window\.open\s*\(
대상: front-admin/src/**/*.{vue,ts,js}
```

→ 사용자 입력이 URL에 포함되면 open redirect / javascript: 실행 가능
→ URL은 반드시 화이트리스트 또는 프로토콜 검증 필요

#### A5. 하드코딩 시크릿 - Frontend (CRITICAL)

```
검색 패턴: (api[_-]?key|secret|password|token|credential)\s*[:=]\s*['"][^'"]{8,}
대상: front-admin/src/**/*.{vue,ts,js,env*}
```

→ 소스 코드에 API 키/비밀번호 하드코딩 = 배포 시 노출
→ .env 파일 또는 서버사이드에서 관리

---

### B. Backend 정적 분석 (OWASP Top 10)

#### B1. SQL Injection (CRITICAL)

```
검색 패턴: \$\{.*\}
대상: backend/src/**/*.java (QueryDSL/JPA 사용으로 위험 낮음)
```

→ MyBatis에서 `${}` 사용 = SQL injection 취약 (문자열 직접 삽입)
→ `#{}` (PreparedStatement 바인딩) 사용 필수
→ 예외: ORDER BY, 테이블명 등 동적 SQL이 불가피한 경우 화이트리스트 검증

#### B2. 인증/인가 누락 (CRITICAL)

```
검색 패턴: @(GetMapping|PostMapping|PutMapping|DeleteMapping|RequestMapping)
대상: backend-api/src/**/*Controller.java
```

→ 엔드포인트에 @PreAuthorize, @Secured, 또는 SecurityConfig 등록 확인
→ 공개 API는 SecurityConfig의 permitAll에 명시적 등록 필요

#### B3. 민감정보 로깅 (HIGH)

```
검색 패턴: log\.(info|debug|warn|error)\s*\(.*(?i)(password|token|secret|credit|card)
대상: backend-api/src/**/*.java
```

→ 로그에 비밀번호/토큰/카드번호 기록 = 로그 유출 시 2차 피해
→ 민감 필드는 마스킹 처리 후 로깅

#### B4. 하드코딩 시크릿 - Backend (CRITICAL)

```
검색 패턴: (password|secret|apiKey|token)\s*=\s*"[^"]{8,}"
대상: backend-api/src/**/*.java
```

→ application.yml/properties 또는 환경변수로 분리 필수

#### B5. CSRF 보호 (HIGH)

```
검색 패턴: csrf\(\)\.disable\(\)
대상: backend-api/src/**/*SecurityConfig*.java
```

→ CSRF 비활성화 시 JWT 토큰 기반 인증인지 확인
→ 세션 기반이면 CSRF 활성화 필수

#### B6. CORS 설정 (HIGH)

```
검색 패턴: allowedOrigins\s*\(\s*"\*"|Access-Control-Allow-Origin.*\*
대상: backend-api/src/**/*.java
```

→ `*` 와일드카드 = 모든 도메인에서 API 접근 가능
→ 운영 환경에서는 명시적 도메인 목록 사용

#### B7. 암호화 취약점 (HIGH)

```
검색 패턴: MD5|SHA-1|SHA1|DES\b|ECB
대상: backend-api/src/**/*.java
```

→ MD5/SHA-1은 충돌 공격에 취약, DES/ECB는 안전하지 않음
→ SHA-256 이상, AES-GCM 사용 권장

#### B8. 파일 업로드 보안 (HIGH)

```
검색 패턴: MultipartFile|getOriginalFilename|transferTo
대상: backend-api/src/**/*.java
```

→ 파일 확장자/MIME 타입 검증, 저장 경로 traversal 방지 확인
→ 실행 가능 파일(.jsp, .sh) 업로드 차단 확인

---

### C. 설정 파일 점검

#### C1. 디버그 모드 (MEDIUM)

```
검색 패턴: debug\s*[:=]\s*true|devtools\s*[:=]\s*true
대상: backend-api/src/**/application*.yml, front-admin/vite.config.*
```

→ 운영 환경에서 디버그 모드 활성화 = 내부 정보 노출

#### C2. 기본 자격증명 (CRITICAL)

```
검색 패턴: (admin|root|test).*[:=].*(admin|root|test|1234|password)
대상: backend-api/src/**/application*.yml
```

→ 기본 계정/비밀번호로 운영 시 무단 접근 위험

---

## 민감 데이터 처리 규칙

- 토큰/키 값: 앞 10자만 표시 + `...` (예: `eyJhbGciOi...`)
- 비밀번호: 절대 기록하지 않음
- 콘솔 로그: 민감 패턴 감지 시 마스킹
- _SECURITY.md: `.gitignore`에 의해 커밋 방지

## 점검 항목별 등급

### Critical (즉시 조치)

| 발견 사항 | Phase |
|----------|:-----:|
| 인증 쿠키에 httpOnly 미설정 | 런타임 |
| localStorage에 JWT 평문 저장 | 런타임 |
| 콘솔에 토큰/키 노출 | 런타임 |
| v-html에 사용자 입력 바인딩 | 정적-FE |
| 동적 코드 실행 패턴 (A3 참조) | 정적-FE |
| 소스 코드에 하드코딩 시크릿 | 정적-FE/BE |
| MyBatis dollar-sign SQL injection | 정적-BE |
| 인증 없는 API 엔드포인트 | 정적-BE |
| 기본 자격증명 사용 | 정적-설정 |

### High (우선 조치)

| 발견 사항 | Phase |
|----------|:-----:|
| 직접 DOM 조작 (A2 참조) | 정적-FE |
| URL injection (A4 참조) | 정적-FE |
| CSRF 비활성화 (세션 기반일 경우) | 정적-BE |
| CORS 와일드카드 설정 | 정적-BE |
| 취약 해시/암호화 알고리즘 | 정적-BE |
| 파일 업로드 검증 미흡 | 정적-BE |
| 민감정보 로깅 | 정적-BE |

### Warning (개선 권장)

| 발견 사항 | Phase |
|----------|:-----:|
| 쿠키에 secure 미설정 | 런타임 |
| 혼합 콘텐츠 (HTTP 리소스) | 런타임 |
| 비밀번호 필드 autocomplete 미설정 | 런타임 |
| sessionStorage에 민감정보 | 런타임 |
| 운영 환경 디버그 모드 활성화 | 정적-설정 |

> 보안은 총점 계산에서 제외된다. _SECURITY.md에 별도 관리.

## 산출물 형식 (_SECURITY.md에 기록)

```markdown
### [S-001] {제목}
- **화면**: {화면ID} (또는 "전체")
- **유형**: 쿠키/스토리지/XSS/정보노출
- **상세**: {설명}
- **파일**: `{소스 파일:라인}`
- **수정 방법**: {가이드}
- **상태**: ⬜ 대기
```

## 실행 모드

### 전체 점검 (기본)

Phase 1 런타임 + Phase 2 정적 분석 모두 수행

### 런타임만

`/ba-security runtime` — agent-browser 필요, 브라우저 접속 상태에서만

### 정적 분석만

`/ba-security static` — agent-browser 불필요, 코드만 분석

### 특정 화면/파일 지정

`/ba-security [파일명 또는 화면ID]` — 해당 파일 + 연관 파일만 분석

---

## 키워드 트리거 제안 형식

```
ba-security 실행을 제안합니다.

ba-security (보안 종합 점검)
  Phase 1 - 런타임: 쿠키, 스토리지, 혼합콘텐츠, 콘솔 유출
  Phase 2 - 정적 분석:
    Frontend: XSS(v-html), DOM조작, 동적코드실행, URL injection, 시크릿
    Backend: SQL injection, 인증/인가, CSRF, CORS, 암호화, 파일업로드, 로깅
    설정: 디버그 모드, 기본 자격증명

실행할까요? (전체 / runtime / static)
```
