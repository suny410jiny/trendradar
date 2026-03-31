# 다음 세션 작업 계획

> 마지막 업데이트: 2026-04-01

## 다음 작업: PHASE 2 - 도메인 설계 (TDD)

### 목표
- Entity 클래스 작성 (TDD): Country, TrendingVideo, ViewSnapshot, AlgorithmTag
- Repository 작성 (TDD): 각 도메인 Repository + QueryDSL Custom
- DTO 클래스 작성

### 선행 조건
- ✅ PostgreSQL 실행 중
- ✅ Flyway 마이그레이션 5개 적용 완료
- ✅ 기본 패키지 구조 생성 완료

### 작업 순서
1. Entity 테스트 작성 → Entity 구현 (Testcontainers)
2. Repository 테스트 작성 → Repository 구현
3. DTO 클래스 작성
4. 전체 테스트 통과 확인
