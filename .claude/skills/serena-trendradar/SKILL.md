---
description: "세션 시작 - Serena MCP 활성화 및 작업 계획 확인"
---

## 세션 시작 - Serena MCP 활성화 및 작업 계획 확인

### Step 1: Serena MCP 프로젝트 활성화
Execute: `mcp__plugin_serena_serena__activate_project("trendradar")`

### Step 2: 이전 작업 계획 확인
Read file: `claude/worklog/_NEXT_PLAN.md`

### Step 3: 작업 계획 표시
파일 내용이 있으면 다음 형식으로 표시:

```
📋 이전 세션에서 계획한 작업이 있습니다:

[_NEXT_PLAN.md 내용 요약]

이어서 진행할까요?
```

파일이 비어있거나 없으면:
```
✅ Serena MCP 활성화 완료. 대기 중인 작업 계획이 없습니다.
무엇을 도와드릴까요?
```
