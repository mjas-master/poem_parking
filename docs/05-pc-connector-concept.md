# 05. PC 연동 프로그램 개념 설계

## 목표
관리사무소 PC에 설치되어, 백엔드의 방문차량 등록 요청을 **주차관리 프로그램**에 반영하고, 주차관리 프로그램의 **입/출차 이벤트**를 백엔드로 올린다. 주차관리 프로그램이 아직 미확정이므로 연동부는 `ParkingAdapter` 인터페이스로 격리한다.

## 제약 / 가정
- PC는 Windows, 사설망(NAT), 인바운드 불가 → **PC가 서버로만 연결**.
- 주차관리 프로그램은 24시간 상주, 재부팅 가능 → 연동 프로그램은 서비스/자동시작, 재시작 시 상태 복구.
- 네트워크 단절 시에도 입/출차는 계속 발생 → **로컬 저장 후 전송**, 재연결 시 배치 업로드.
- 아파트 1곳 = 연동 프로그램 1개 = API Key 1개.

## 구성
```
┌────────────────────── PC ──────────────────────┐
│  Connector                                     │
│  ├─ JobWorker      : 폴링 → Adapter.register/cancel → ack
│  ├─ EventCollector : Adapter.fetchGateEvents → LocalStore → 서버 batch 전송
│  ├─ Heartbeat      : 60초
│  ├─ LocalStore     : SQLite (job_log, event_log, kv)
│  └─ ParkingAdapter : Mock | DbAdapter | FileAdapter | VendorApiAdapter | UiAutomationAdapter
│                                   │
│  주차관리 프로그램 (3rd-party) ◀──┘
└────────────────────────────────────────────────┘
```

## 어댑터 구현 시 확인 사항 (체크리스트)
1. 방문차량 등록 방법: DB 테이블? import 파일? API? UI만?
2. 등록 필수 필드: 번호, 유효기간, 동/호, 메모? 번호 형식(공백 유무)?
3. 입/출차 로그 위치: 테이블/파일/API, 컬럼(번호·시각·게이트·방향), 인식 실패 표기
4. 삭제/만료 처리: 명시적 삭제 필요 vs 기간 지나면 자동 무효
5. 동시 접근 가능 여부(DB 락, 파일 잠금), 벤더 라이선스/보증 조건
6. 시간 동기화(PC 시각 vs 서버 시각) — 이벤트는 PC 시각으로 기록, 서버는 그대로 저장

## 장애 시나리오
| 상황 | 동작 |
|---|---|
| 서버 다운 | 폴링 실패 로그, 이벤트는 로컬 적재 후 복구 시 전송 |
| 주차관리 프로그램 오류 | ack(success=false) → 서버 재시도(5회) → FAILED, 사용자에 표시 |
| 연동 프로그램 종료 | 서버가 5분 후 재전달 큐로 복귀, 하트비트 미수신 → 관리자 알림(백로그 B-107) |
| 중복 이벤트 | `externalEventId` 로컬 unique, 서버 멱등(백로그 B-004) |

## 운영
- 설치: jpackage MSI, 서비스 등록(nssm 또는 WinSW), `connector.properties` 배포
- 로그: `logs/connector.log` 일 단위 롤링 30일
- 업데이트: 수동 → 추후 자동 업데이트(백로그)
