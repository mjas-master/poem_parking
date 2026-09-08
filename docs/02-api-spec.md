# 02. REST API 명세

Base URL: `/api/v1` · Content-Type: `application/json` · 시간: ISO-8601 로컬(`2026-09-08T10:00:00`)
Swagger UI: `/swagger-ui/index.html`

## 공통 오류 응답
```json
{ "code": "DUPLICATE_PLATE", "message": "해당 기간에 이미 등록된 차량번호입니다.", "timestamp": "...", "details": null }
```
| HTTP | code | 설명 |
|---|---|---|
| 400 | VALIDATION_ERROR, PLATE_INVALID, PERIOD_INVALID, PERIOD_PAST, PERIOD_TOO_LONG, OTP_INVALID, VERIFY_CODE_INVALID, IN_PROGRESS | 입력 오류 |
| 401 | UNAUTHORIZED, CONNECTOR_UNAUTHORIZED | 인증 실패 |
| 403 | FORBIDDEN | 아파트 인증 미완료 |
| 404 | NOT_FOUND | 없음 |
| 409 | DUPLICATE_PLATE, LIMIT_EXCEEDED, NOT_CANCELLABLE, ALREADY_ENTERED | 상태 충돌 |

---
## A. 인증 (인증 불필요)

### POST /auth/otp — 인증번호 요청 (목업)
`{ "phone": "01012345678" }` → 200 `{ "message": "..." }`

### POST /auth/login — 인증번호 확인 + 토큰 발급
`{ "phone": "01012345678", "otp": "123456", "name": "홍길동" }`
→ 200
```json
{ "accessToken": "eyJ...", "tokenType": "Bearer", "expiresInSeconds": 2592000,
  "user": { "id": 1, "phone": "01012345678", "name": "홍길동", "verified": false, "apartment": null, "dong": null, "ho": null } }
```

### GET /auth/apartments — 아파트 목록
→ `[ { "code": "APT-0001", "name": "행복마을 아파트", "freeMinutes": 30, "unitMinutes": 10, "unitFee": 500, "dailyCapFee": 10000 } ]`

## B. 내 정보 (JWT)

### GET /me → UserResponse
### POST /me/verify-apartment — 아파트/세대 인증 (목업)
`{ "apartmentCode": "APT-0001", "dong": "101", "ho": "1203", "verifyCode": "000000" }` → UserResponse(verified=true)

## C. 방문차량 등록 (JWT + 아파트 인증)

### POST /registrations → 201 RegistrationResponse
```json
{ "plateNo": "12가 3456", "visitorName": "김방문", "visitorPhone": "01099998888", "purpose": "가족",
  "visitFrom": "2026-09-08T10:00:00", "visitTo": "2026-09-08T20:00:00" }
```
검증: 번호 형식, 종료>시작, 미래 시간, 최대 7일, 세대 동시 활성 5건, 동일 번호 기간 중복.

### GET /registrations?page=0&size=20 → PageResponse<RegistrationResponse>
### GET /registrations/active → RegistrationResponse[] (PENDING/SYNCED/ENTERED)
### GET /registrations/{id} → RegistrationResponse
### DELETE /registrations/{id} — 취소 → RegistrationResponse(status=CANCELLED)

RegistrationResponse
```json
{ "id": 10, "plateNo": "12가3456", "visitorName": "김방문", "visitorPhone": null, "purpose": "가족",
  "visitFrom": "...", "visitTo": "...", "status": "SYNCED", "syncedAt": "...", "failReason": null, "createdAt": "..." }
```

## D. 이력 (JWT + 아파트 인증)

### GET /histories?page=0&size=20 → PageResponse<HistoryResponse>
```json
{ "id": 3, "registrationId": 10, "plateNo": "12가3456", "visitorName": "김방문",
  "enteredAt": "...", "exitedAt": "...", "durationMinutes": 95, "fee": 3500, "inProgress": false }
```
### DELETE /histories/{id} → 204 (소프트 삭제, 주차 중이면 400 IN_PROGRESS)

## E. 요금 (JWT + 아파트 인증)

### GET /fees/summary?year=2026&month=9 → FeeSummaryResponse
```json
{ "year": 2026, "month": 9, "visitCount": 4, "totalMinutes": 380, "totalFee": 9000,
  "byPlate": [ { "plateNo": "12가3456", "visitCount": 2, "totalMinutes": 200, "totalFee": 5000 } ] }
```

## F. PC 연동 프로그램 (헤더 `X-Apartment-Code`, `X-Connector-Key`)

### GET /connector/jobs?limit=20 → SyncJobResponse[]  (PENDING → DELIVERED)
```json
{ "jobId": 1, "type": "REGISTER", "registrationId": 10, "plateNo": "12가3456", "dong": "101", "ho": "1203",
  "visitorName": "김방문", "visitFrom": "...", "visitTo": "...", "attempts": 1 }
```
### POST /connector/jobs/{jobId}/ack
`{ "success": true, "externalId": "PC-778" }` 또는 `{ "success": false, "errorMessage": "..." }`

### POST /connector/events — 단건 / POST /connector/events/batch — 배열
`{ "type": "ENTRY|EXIT", "plateNo": "12가3456", "occurredAt": "2026-09-08T11:00:00", "externalEventId": "e-1" }`
→ `{ "accepted": true, "matched": true, "historyId": 3, "message": "입차 기록 완료" }`
- ENTRY: 활성 등록(PENDING/SYNCED, 기간 내)이 있으면 이력 생성 + ENTERED. 없으면 `matched=false` (기록 안 함).
- EXIT: 열려 있는 이력을 닫고 체류시간/요금 계산 + EXITED.

### POST /connector/heartbeat
`{ "version": "0.1.0", "hostName": "OFFICE-PC" }` → `{ "serverTime": "...", "apartment": "APT-0001" }`
