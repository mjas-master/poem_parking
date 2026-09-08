# 03. 기능 개발 목록

범례: ✅ 구현 완료 · 🟡 목업/부분 · ⬜ 미구현 · 🔒 운영 전 필수

## 1. 인증 / 사용자
| ID | 기능 | App | Backend | 상태 | 비고 |
|---|---|---|---|---|---|
| AUTH-01 | 휴대폰 번호 OTP 로그인 | `LoginScreen` | `POST /auth/otp`, `/auth/login` | 🟡 | OTP 고정값 `123456`, SMS 미연동 |
| AUTH-02 | JWT 발급/보관/자동 로그인 | `AuthNotifier`, secure_storage | `JwtProvider`, `JwtAuthFilter` | ✅ | 30일 만료, 리프레시 토큰 없음 |
| AUTH-03 | 아파트/세대 인증 | `ApartmentVerifyScreen` | `POST /me/verify-apartment` | 🟡 | 인증코드 고정값 `000000` |
| AUTH-04 | 아파트 목록 조회 | 드롭다운 | `GET /auth/apartments` | ✅ | 시드 2개 |
| AUTH-05 | 내 정보 조회 | 설정 화면 | `GET /me` | ✅ | |
| AUTH-06 | 로그아웃 | 설정/인증 화면 | (클라이언트) | ✅ | |
| AUTH-07 | 인증 상태 기반 라우팅(스플래시→로그인→인증→홈) | `router.dart` redirect | | ✅ | |

## 2. 방문차량 등록
| ID | 기능 | App | Backend | 상태 | 비고 |
|---|---|---|---|---|---|
| REG-01 | 차량번호 입력·형식 검증·정규화 | 클라이언트 regex | `PlateValidator` | ✅ | 신형/구형 지역번호판 |
| REG-02 | 방문자 이름/연락처/목적 (선택) | 폼 | DTO | ✅ | |
| REG-03 | 방문 기간 선택 (날짜·시간 피커, 빠른 선택) | `RegisterVisitorScreen` | | ✅ | +2h/+6h/당일/+1일 |
| REG-04 | 기간 검증 (종료>시작, 미래, 최대 7일) | | `VisitorService` | ✅ | |
| REG-05 | 세대 동시 등록 한도 (5대) | | `VisitorService` | ✅ | 상수, 아파트별 설정은 백로그 |
| REG-06 | 동일 번호 기간 중복 방지 | | `findOverlapping` | ✅ | |
| REG-07 | 등록 → Outbox 큐 적재 | | `SyncOutbox` | ✅ | |
| REG-08 | 현재 유효 목록 / 전체 목록(페이지) | `VisitorListScreen` | `GET /registrations[/active]` | ✅ | |
| REG-09 | 상세 조회 (상태·반영 시각·실패 사유) | `RegistrationDetailScreen` | `GET /registrations/{id}` | ✅ | |
| REG-10 | 등록 취소 (PC 반영분은 CANCEL 작업 발행) | 상세 화면 | `DELETE /registrations/{id}` | ✅ | 입차 후 취소 불가 |
| REG-11 | 기간 만료 자동 처리 | | `@Scheduled expireOld` | ✅ | 10분 주기 |
| REG-12 | 등록 수정(기간 연장 등) | | | ⬜ | UPDATE 작업 타입은 예약됨 |
| REG-13 | 자주 오는 차량 즐겨찾기 | | | ⬜ | |

## 3. 연동 (Backend ↔ PC)
| ID | 기능 | 상태 | 비고 |
|---|---|---|---|
| SYNC-01 | 작업 폴링 API (PENDING→DELIVERED) | ✅ | `GET /connector/jobs` |
| SYNC-02 | ack API (성공→SYNCED / 실패→재시도·FAILED) | ✅ | 5회 |
| SYNC-03 | ack 타임아웃 재전달 배치 | ✅ | 5분, 1분 주기 |
| SYNC-04 | 입/출차 이벤트 수신(단건·배치) → 이력 생성/종료 → 요금 계산 | ✅ | |
| SYNC-05 | 아파트별 API Key 인증 | ✅ | yml 설정 |
| SYNC-06 | 하트비트 | 🟡 | 로그만, 상태 저장/알림 없음 |
| SYNC-07 | PC 연동 프로그램 본체 (폴링 루프, 로컬 SQLite, Mock 어댑터) | 🟡 | 스켈레톤, 실제 어댑터 없음 |
| SYNC-08 | 실제 주차관리 프로그램 어댑터 | ⬜ | 프로그램 확정 후 |
| SYNC-09 | 이벤트 중복 방지 (`externalEventId` 서버 저장) | ⬜ | 현재는 열린 이력 존재 여부로만 방어 |

## 4. 이력
| ID | 기능 | App | Backend | 상태 |
|---|---|---|---|---|
| HIST-01 | 입/출차 이력 목록 (무한 스크롤, 새로고침) | `HistoryScreen` | `GET /histories` | ✅ |
| HIST-02 | 이력 삭제 (스와이프/롱프레스, 소프트 삭제) | | `DELETE /histories/{id}` | ✅ |
| HIST-03 | 주차 중 이력 삭제 차단 | | | ✅ |
| HIST-04 | 이력 검색/필터(차량번호, 기간) | | | ⬜ |
| HIST-05 | PC 로컬 이력 저장 | pc-connector `LocalStore` | | 🟡 |

## 5. 요금
| ID | 기능 | 상태 | 비고 |
|---|---|---|---|
| FEE-01 | 요금 정책 (무료시간/단위/단가/일상한) 아파트별 | ✅ | 엔티티 컬럼 |
| FEE-02 | 출차 시 요금 확정 | ✅ | |
| FEE-03 | 월별 집계 (총액, 차량별, 잠정치 포함) | ✅ | `GET /fees/summary` |
| FEE-04 | 월 이동 UI | ✅ | |
| FEE-05 | 관리비 고지 연동 / 결제 | ⬜ | |
| FEE-06 | 요금 정책 변경 이력 (시점별 정책 적용) | ⬜ | 현재는 현재 정책으로 계산 |

## 6. 공통 / 인프라
| ID | 기능 | 상태 |
|---|---|---|
| INF-01 | 공통 오류 포맷 + 전역 예외 핸들러 | ✅ |
| INF-02 | Swagger(OpenAPI) | ✅ |
| INF-03 | H2(dev) / PostgreSQL(prod) 프로파일 | ✅ |
| INF-04 | 단위 테스트 (PlateValidator, FeeCalculator) | ✅ |
| INF-05 | 통합 테스트 (API) | ⬜ |
| INF-06 | 🔒 HTTPS, 시크릿 외부화, Rate limit | ⬜ |
| INF-07 | 🔒 DB 마이그레이션 (Flyway) | ⬜ |
| INF-08 | CI (GitHub Actions: gradle test, flutter analyze) | ⬜ |
