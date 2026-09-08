# poem-parking-connector (PC 연동 프로그램) — 개념 설계 + 스켈레톤

주차관리 프로그램이 설치된 PC에 함께 설치되어, **백엔드 ↔ 주차관리 프로그램** 사이를 중계한다.
주차관리 프로그램의 연동 방식이 아직 확정되지 않았으므로 `ParkingAdapter` 인터페이스 뒤에 숨기고, 현재는 `MockParkingAdapter`만 제공한다.

## 동작
```
[백엔드]  ──(폴링 GET /connector/jobs)──▶  [연동 프로그램]  ──▶ ParkingAdapter.registerVisitor()  ──▶ [주차관리 프로그램]
[백엔드]  ◀─(POST /connector/jobs/{id}/ack)── [연동 프로그램]
[백엔드]  ◀─(POST /connector/events/batch)─── [연동 프로그램]  ◀── ParkingAdapter.fetchGateEvents() ◀── [주차관리 프로그램]
```
- 아웃바운드(PC→서버)만 사용하므로 PC에 인바운드 포트 개방이 필요 없다. (아파트 관리사무소 네트워크 환경 고려)
- 모든 처리 결과와 이벤트는 로컬 SQLite(`connector.db`)에 먼저 기록 후 전송 → 서버 장애 시 재전송, 로컬 이력 감사.
- 인증: `X-Apartment-Code` + `X-Connector-Key` 헤더 (아파트 단위 API Key).

## 어댑터 후보 (주차관리 프로그램 확인 후 선택)
| 방식 | 조건 | 장단점 |
|---|---|---|
| A. DB 직접 접근 | 프로그램 DB(MSSQL/Access 등) 접근 가능 | 가장 확실. 스키마 분석 필요, 벤더 정책 확인 |
| B. 파일 연동 | CSV/Excel import·export 기능 존재 | 구현 쉬움. 실시간성 낮음 |
| C. 벤더 API/SDK | 벤더가 제공 | 가장 안정적. 계약/비용 |
| D. UI 자동화 | 위 방법 모두 불가 | 최후 수단. 깨지기 쉬움 |

## 실행
```bash
cp connector.properties.example connector.properties   # 값 수정
./gradlew run
```
## 배포 (예정)
- `jpackage`로 Windows 서비스/트레이 앱 패키징, 자동 시작 등록, 로그 롤링
