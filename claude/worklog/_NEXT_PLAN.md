# 다음 세션 작업 계획

> 마지막 업데이트: 2026-04-01

## 현재 상태

### 완료된 Phase
- ✅ Phase 0-8 완료
- ✅ 백엔드: 26개 테스트 통과, API 6개, 포트 8081
- ✅ 프론트: React + shadcn + Tailwind, 전체 화면 구현, 포트 5180
- ✅ Vultr 서버: SSL 발급 (trend-rada.com, api.trend-rada.com)
- ✅ GitHub 푸시 완료 (suny410jiny/trendradar)
- ✅ SSH 키 등록 (ssh root@158.247.241.196)

### 포트 정보 (다른 프로젝트와 충돌 방지)
- 백엔드: **8081** (다른 프로젝트가 8080 사용)
- 프론트: **5180** (Vite dev server)
- CORS: `http://localhost:*` 패턴으로 허용

### 미커밋 변경사항
- 포트 변경 (8080→8081): application.yml, Dockerfile, docker-compose.yml, nginx.conf
- 프론트 포트 (5180): vite.config.ts, .env.local, api/client.ts
- CORS 수정: WebConfig.java (`http://localhost:*`)
- CLAUDE.md SSH 정보 추가
→ **다음 세션 시작 시 커밋 + 푸시 필요**

## 다음 작업

### 1순위: 미커밋 변경사항 커밋 + 푸시
```
git add .
git commit -m "chore: 포트 변경 (8081) 및 CORS 수정"
git push origin main
```

### 2순위: Phase 9 - 프론트 Docker 빌드
- frontend/Dockerfile 작성 (multi-stage: Node build → Nginx serve)
- docker-compose.yml에 frontend 서비스 추가

### 3순위: Phase 10 - 통합 테스트 & 연동
- 백엔드 + 프론트 연동 확인 (실제 YouTube API 데이터)
- 스케줄러 수동 트리거 → DB 데이터 확인 → 프론트 표시

### 4순위: Phase 11 - Vultr 배포
- git clone → docker compose up
- Nginx SSL 프록시 → backend:8081 연결
- 실제 도메인 접속 확인

### 5순위: Phase 12 - 검증 마무리
- 전체 흐름 E2E 확인
- 모니터링 설정
