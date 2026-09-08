# poem_parking (Flutter)

입주민이 방문차량을 등록하고, 입출차 이력·주차료를 확인하는 모바일 앱.

## 실행
```bash
flutter pub get
# Android 에뮬레이터 (기본 baseUrl = http://10.0.2.2:8080)
flutter run
# iOS 시뮬레이터 / 실기기
flutter run --dart-define=API_BASE_URL=http://localhost:8080
flutter run --dart-define=API_BASE_URL=http://192.168.0.10:8080
```
> Android에서 http 통신을 허용하려면 `android/app/src/main/AndroidManifest.xml`의 `<application>`에
> `android:usesCleartextTraffic="true"` 를 추가하세요. (`flutter create .` 로 플랫폼 폴더 생성 후)

## 목업 값
- 휴대폰 인증번호: `123456`
- 아파트 인증코드: `000000`

## 구조
```
lib/
├── main.dart
├── app/            MaterialApp, GoRouter (인증 상태에 따른 redirect)
├── core/
│   ├── api/        Dio 클라이언트(JWT 인터셉터), ApiService(엔드포인트 래퍼), ApiException
│   ├── storage/    토큰 보관 (flutter_secure_storage)
│   └── utils/      날짜/금액/차량번호 포맷
├── models/         User, Registration, VisitHistory, FeeSummary ...
├── providers/      Riverpod (auth, 목록/상세/이력/요금)
├── widgets/        공통 위젯 (StatusChip, PlateBadge, ErrorBox ...)
└── features/
    ├── auth/       스플래시, OTP 로그인, 아파트 인증(목업)
    ├── home/       하단 탭 쉘
    ├── visitor/    방문차량 목록 / 등록 / 상세(취소)
    ├── history/    입출차 이력 (무한 스크롤, 스와이프 삭제)
    ├── fee/        월별 주차료 집계
    └── settings/   프로필, 로그아웃
```
