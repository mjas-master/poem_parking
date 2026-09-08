# 01. 아키텍처

## 1. 컴포넌트

| 컴포넌트 | 기술 | 역할 |
|---|---|---|
| Mobile App | Flutter 3.x, Riverpod, GoRouter, Dio, secure_storage | 로그인/아파트 인증, 방문차량 등록·취소, 이력 조회·삭제, 요금 조회 |
| Backend | Kotlin 2.0, Spring Boot 3.3, Spring Security(JWT), JPA, H2(dev)/PostgreSQL(prod), springdoc | 데이터 검증, 상태 관리, 요금 계산, 연동 큐(Outbox), 이력 원장 |
| PC Connector | Kotlin, Ktor client, SQLite | 서버 작업 폴링 → 주차관리 프로그램 반영, 입/출차 이벤트 수집 → 서버 전송, 로컬 이력 |
| 주차관리 프로그램 | 3rd-party (미확정) | 차단기/LPR 연동, 방문차량 허용 목록 관리 |

## 2. 핵심 설계 결정

1. **PC → 서버 아웃바운드 폴링만 사용.** 관리사무소 PC에 포트 개방/고정 IP를 요구하지 않는다. 10초 주기 폴링(설정 가능). 필요 시 WebSocket/SSE로 대체 가능(백로그).
2. **Outbox 패턴.** 등록/취소는 `sync_outbox` 테이블에 작업으로 적재 → PC가 가져가면 `DELIVERED` → 처리 결과 `ack` → `ACKED/FAILED`. 5분간 ack 없으면 재전달, 5회 실패 시 `FAILED` 로 확정하고 사용자에게 노출.
3. **이력은 이중 보관.** 서버(`visit_histories`)와 PC 로컬(`event_log`) 모두 기록. 서버가 과금 원장, PC는 감사/재전송용.
4. **소프트 삭제.** 사용자가 이력을 삭제하면 목록에서만 숨기고 요금 집계에는 계속 포함(분쟁 방지).
5. **차량번호 정규화.** 공백/하이픈 제거 후 저장·비교. 신형(`12가3456`,`123가4567`)·구형 지역번호판 허용.
6. **인증 2단계.** ① 휴대폰 OTP(목업) → JWT, ② 아파트/세대 인증(목업 코드 `000000`). 인증 전에는 `/registrations` 등 세대 API 접근 불가(403).

## 3. 데이터 모델

```
apartments ──< households ──< users
                  │
                  ├──< visitor_registrations ──< sync_outbox
                  │            │
                  └──< visit_histories (registration_id nullable)
```

| 테이블 | 주요 컬럼 |
|---|---|
| apartments | code, name, freeMinutes, unitMinutes, unitFee, dailyCapFee |
| households | apartment_id, dong, ho (unique) |
| users | phone(unique), name, household_id, role, verified |
| visitor_registrations | household_id, user_id, plateNo, visitorName/Phone, purpose, visitFrom, visitTo, status, externalId, syncedAt, failReason |
| visit_histories | household_id, registration_id, plateNo, enteredAt, exitedAt, durationMinutes, fee, deletedByUser |
| sync_outbox | apartment_id, registration_id, type(REGISTER/CANCEL/UPDATE), status, attempts, deliveredAt, ackedAt, lastError |

## 4. 등록 상태 흐름

```
        앱 등록                PC ack(success)          입차 이벤트            출차 이벤트
PENDING ─────────▶ SYNCED ───────────────────▶ ENTERED ─────────────▶ EXITED
   │                  │
   │ 사용자 취소       │ 사용자 취소(CANCEL 작업 발행)
   ▼                  ▼
CANCELLED         CANCELLED
   
PENDING/SYNCED ── visitTo 경과 (10분 배치) ──▶ EXPIRED
PENDING ── ack 실패 5회 / 타임아웃 ──▶ FAILED
```

## 5. 요금 계산

`FeeCalculator.calculate(apt, minutes)`
- 체류 분 = ceil((출차 − 입차) / 60s)
- 과금 분 = max(0, 체류 − freeMinutes)
- 요금 = ceil(과금 분 / unitMinutes) × unitFee, 상한 = dailyCapFee × 일수(ceil(체류/1440))
- 예) 무료 30분, 10분 500원, 상한 1만원: 95분 → 3,500원 / 12시간 → 10,000원

월별 집계(`/fees/summary`)는 `enteredAt` 기준으로 해당 월에 입차한 건을 합산하며, 주차 중인 건은 현재 시각 기준 잠정치.

## 6. 보안

- 앱: `Authorization: Bearer <JWT>` (HS256, 30일). 사용자 ↔ 세대 검증은 항상 서버에서 `household` 로 스코프.
- PC: `X-Apartment-Code` + `X-Connector-Key`. 키는 `application.yml`(prod: 환경변수/시크릿 매니저)에서 아파트별 관리.
- 운영 전 필수: HTTPS, JWT 시크릿 교체, H2 콘솔 비활성(prod 프로파일 처리됨), Rate limit(백로그).
