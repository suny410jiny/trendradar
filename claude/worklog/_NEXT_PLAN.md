# 다음 세션 작업 계획

> 마지막 업데이트: 2026-04-01

## 현재 상태

### 완료된 Phase
- ✅ Phase 0-12 완료 (MVP 완성)
- ✅ 백엔드: Vultr 배포 완료 (Docker Compose, 8081 포트)
- ✅ 프론트: Vercel 배포 완료 (https://trend-rada.com)
- ✅ API: https://api.trend-rada.com (SSL, Nginx 리버스 프록시)
- ✅ 5개국 데이터 수집 완료 (KR, US, JP, GB, DE x 50개)
- ✅ 알고리즘 태그 동작 (NEW_ENTRY, HOT_COMMENT, HIGH_ENGAGE, GLOBAL)
- ✅ 1시간 주기 자동 수집 스케줄러 동작

### 알려진 이슈
- ⚠️ 알고리즘 태그 중복: 동일 video_id에 국가 수만큼 태그 중복 저장됨
- ⚠️ Claude API 키 미설정: AI 브리핑 "준비 중" 메시지 반환
- ⚠️ 프론트 → 백엔드 실제 브라우저 연동 미확인 (API는 정상)

## 다음 작업 (우선순위)

### 1순위: 알고리즘 태그 중복 버그 수정
- 원인: collectAllCountries()에서 5개국 수집 후 allVideos에 전체 합쳐서 태그 계산
- 같은 videoId가 여러 국가에 존재 시 태그가 중복 계산됨
- 해결: video_id 기준 중복 제거 후 태그 계산, 또는 태그 저장 시 UPSERT

### 2순위: Claude API 키 설정
- .env에 CLAUDE_API_KEY 추가
- AI 브리핑 실제 동작 확인

### 3순위: 프론트엔드 브라우저 연동 확인
- trend-rada.com에서 실제 데이터 로드 확인
- CORS 이슈 없는지 확인

### 4순위: 모니터링 설정
- Docker 로그 로테이션
- 디스크/메모리 모니터링
