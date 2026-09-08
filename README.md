# poem_parking — 아파트 방문차량 주차관리 시스템

입주민이 **스마트폰으로 방문차량 번호를 등록**하면 아파트 **주차관리 프로그램에 자동 반영**되고,
입/출차 이력을 기반으로 **방문 주차료를 집계**하는 시스템.

```
┌──────────────┐  HTTPS/JSON   ┌────────────────────┐  HTTPS/JSON(폴링)  ┌─────────────────────┐        ┌──────────────────┐
│  Flutter App │ ────────────▶ │  Kotlin Backend    │ ◀───────────────── │ PC 연동 프로그램     │ ◀────▶ │ 주차관리 프로그램  │
│  (입주민)     │ ◀──────────── │  Spring Boot + DB  │ ─────────────────▶ │ (Kotlin, SQLite)    │        │ (3rd-party)      │
└──────────────┘               └────────────────────┘                    └─────────────────────┘        └──────────────────┘
     mobile/                          backend/                                pc-connector/
```

| 디렉터리 | 내용 | 상태 |
|---|---|---|
| `mobile/` | Flutter 앱 (Riverpod, GoRouter, Dio) | ✅ MVP 구현 |
| `backend/` | Kotlin Spring Boot 3 REST API, JWT, H2/PostgreSQL | ✅ MVP 구현 |
| `pc-connector/` | PC 연동 프로그램 — 개념 설계 + Mock 어댑터 스켈레톤 | 🟡 개념/스켈레톤 |
| `docs/` | 아키텍처, API 명세, 기능 목록, 백로그 | ✅ |

## 문서
- [docs/01-architecture.md](docs/01-architecture.md) — 아키텍처, 데이터 모델, 상태 흐름
- [docs/02-api-spec.md](docs/02-api-spec.md) — REST API 명세
- [docs/03-features.md](docs/03-features.md) — 기능 개발 목록 (완료/미완료)
- [docs/04-backlog.md](docs/04-backlog.md) — 백로그 (우선순위별)
- [docs/05-pc-connector-concept.md](docs/05-pc-connector-concept.md) — PC 연동 프로그램 개념 설계

## 빠른 시작
```bash
# 1) 백엔드
cd backend && gradle wrapper --gradle-version 8.10 && ./gradlew bootRun
# 2) 앱
cd mobile && flutter create . --platforms=android,ios && flutter pub get && flutter run
# 3) (선택) 연동 프로그램 목업 — 등록 30초 후 입차, 2분 후 출차 이벤트를 자동 발생
cd pc-connector && cp connector.properties.example connector.properties && gradle wrapper && ./gradlew run
```
목업 인증: SMS OTP `123456`, 아파트 인증코드 `000000`, 아파트 코드 `APT-0001`.

> 참고 요청하신 템플릿 저장소 `mjas-master/mjas-app-template` 는 접근 시 404 로 확인되지 않아(비공개 또는 삭제),
> 일반적인 Flutter(feature-first + Riverpod) / Spring Boot(layered) 구조로 구성했습니다. 템플릿을 공개해 주시면 구조를 맞출 수 있습니다.
