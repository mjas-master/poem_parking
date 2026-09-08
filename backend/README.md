# poem-parking-backend (Kotlin / Spring Boot 3)

## 실행
```bash
./gradlew bootRun          # http://localhost:8080
# Swagger UI : http://localhost:8080/swagger-ui/index.html
# H2 콘솔    : http://localhost:8080/h2  (jdbc:h2:file:./data/parking, sa / 비번없음)
```
> Gradle Wrapper는 포함되어 있지 않습니다. `gradle wrapper --gradle-version 8.10` 으로 생성하세요.

## 목업 값 (application.yml)
| 항목 | 값 |
|---|---|
| SMS OTP | `123456` |
| 아파트 인증코드 | `000000` |
| 시드 아파트 | `APT-0001` (행복마을), `APT-0002` (푸른숲) |
| 연동 프로그램 API Key | `APT-0001` → `connector-key-apt-0001` |

## 빠른 테스트 (curl)
```bash
# 1. 로그인
TOKEN=$(curl -s -X POST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"phone":"01012345678","otp":"123456","name":"홍길동"}' | jq -r .accessToken)

# 2. 아파트 인증
curl -s -X POST localhost:8080/api/v1/me/verify-apartment -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"apartmentCode":"APT-0001","dong":"101","ho":"1203","verifyCode":"000000"}'

# 3. 방문차량 등록
curl -s -X POST localhost:8080/api/v1/registrations -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"plateNo":"12가3456","visitorName":"김방문","visitFrom":"2026-09-08T10:00:00","visitTo":"2026-09-08T20:00:00"}'

# 4. (PC 연동 프로그램 역할) 작업 폴링 → ack → 입차 → 출차
H='-H X-Apartment-Code:APT-0001 -H X-Connector-Key:connector-key-apt-0001 -H Content-Type:application/json'
curl -s $H localhost:8080/api/v1/connector/jobs
curl -s $H -X POST localhost:8080/api/v1/connector/jobs/1/ack -d '{"success":true,"externalId":"PC-1"}'
curl -s $H -X POST localhost:8080/api/v1/connector/events -d '{"type":"ENTRY","plateNo":"12가3456","occurredAt":"2026-09-08T11:00:00"}'
curl -s $H -X POST localhost:8080/api/v1/connector/events -d '{"type":"EXIT","plateNo":"12가3456","occurredAt":"2026-09-08T12:35:00"}'

# 5. 이력 / 요금
curl -s -H "Authorization: Bearer $TOKEN" localhost:8080/api/v1/histories
curl -s -H "Authorization: Bearer $TOKEN" localhost:8080/api/v1/fees/summary
```

## 패키지 구조
```
com.poem.parking
├── config      Security(JWT + Connector API Key), JwtProvider, 시드 데이터
├── domain      JPA 엔티티 (Apartment, Household, User, VisitorRegistration, VisitHistory, SyncOutbox)
├── repository  Spring Data JPA
├── service     비즈니스 로직 (검증, 요금 계산, Outbox, 이벤트 처리)
├── controller  REST API (앱용 / 연동 프로그램용)
├── dto         요청/응답 모델
└── exception   공통 예외 + 핸들러
```
