class ApartmentSummary {
  final String code, name;
  final int freeMinutes, unitMinutes, unitFee, dailyCapFee;
  ApartmentSummary({required this.code, required this.name, required this.freeMinutes, required this.unitMinutes, required this.unitFee, required this.dailyCapFee});

  factory ApartmentSummary.fromJson(Map<String, dynamic> j) => ApartmentSummary(
        code: j['code'], name: j['name'],
        freeMinutes: j['freeMinutes'], unitMinutes: j['unitMinutes'],
        unitFee: j['unitFee'], dailyCapFee: j['dailyCapFee'],
      );
}

class User {
  final int id;
  final String phone, name;
  final bool verified;
  final ApartmentSummary? apartment;
  final String? dong, ho;
  User({required this.id, required this.phone, required this.name, required this.verified, this.apartment, this.dong, this.ho});

  factory User.fromJson(Map<String, dynamic> j) => User(
        id: j['id'], phone: j['phone'], name: j['name'], verified: j['verified'] == true,
        apartment: j['apartment'] == null ? null : ApartmentSummary.fromJson(j['apartment']),
        dong: j['dong'], ho: j['ho'],
      );

  String get householdLabel => apartment == null ? '' : '${apartment!.name} $dong동 $ho호';
}

enum RegistrationStatus { pending, synced, entered, exited, cancelled, expired, failed, unknown }

RegistrationStatus statusFrom(String s) => switch (s) {
      'PENDING' => RegistrationStatus.pending,
      'SYNCED' => RegistrationStatus.synced,
      'ENTERED' => RegistrationStatus.entered,
      'EXITED' => RegistrationStatus.exited,
      'CANCELLED' => RegistrationStatus.cancelled,
      'EXPIRED' => RegistrationStatus.expired,
      'FAILED' => RegistrationStatus.failed,
      _ => RegistrationStatus.unknown,
    };

extension RegistrationStatusX on RegistrationStatus {
  String get label => switch (this) {
        RegistrationStatus.pending => '전송 대기',
        RegistrationStatus.synced => '등록 완료',
        RegistrationStatus.entered => '주차 중',
        RegistrationStatus.exited => '출차 완료',
        RegistrationStatus.cancelled => '취소됨',
        RegistrationStatus.expired => '기간 만료',
        RegistrationStatus.failed => '등록 실패',
        RegistrationStatus.unknown => '알 수 없음',
      };
  bool get isActive => this == RegistrationStatus.pending || this == RegistrationStatus.synced || this == RegistrationStatus.entered;
  bool get cancellable => this == RegistrationStatus.pending || this == RegistrationStatus.synced;
}

class Registration {
  final int id;
  final String plateNo;
  final String? visitorName, visitorPhone, purpose, failReason;
  final DateTime visitFrom, visitTo, createdAt;
  final DateTime? syncedAt;
  final RegistrationStatus status;

  Registration({required this.id, required this.plateNo, this.visitorName, this.visitorPhone, this.purpose, this.failReason,
      required this.visitFrom, required this.visitTo, required this.createdAt, this.syncedAt, required this.status});

  factory Registration.fromJson(Map<String, dynamic> j) => Registration(
        id: j['id'], plateNo: j['plateNo'], visitorName: j['visitorName'], visitorPhone: j['visitorPhone'],
        purpose: j['purpose'], failReason: j['failReason'],
        visitFrom: DateTime.parse(j['visitFrom']), visitTo: DateTime.parse(j['visitTo']),
        createdAt: DateTime.parse(j['createdAt']),
        syncedAt: j['syncedAt'] == null ? null : DateTime.parse(j['syncedAt']),
        status: statusFrom(j['status']),
      );
}

class VisitHistory {
  final int id;
  final int? registrationId, durationMinutes, fee;
  final String plateNo;
  final String? visitorName;
  final DateTime enteredAt;
  final DateTime? exitedAt;
  final bool inProgress;

  VisitHistory({required this.id, this.registrationId, required this.plateNo, this.visitorName, required this.enteredAt,
      this.exitedAt, this.durationMinutes, this.fee, required this.inProgress});

  factory VisitHistory.fromJson(Map<String, dynamic> j) => VisitHistory(
        id: j['id'], registrationId: j['registrationId'], plateNo: j['plateNo'], visitorName: j['visitorName'],
        enteredAt: DateTime.parse(j['enteredAt']),
        exitedAt: j['exitedAt'] == null ? null : DateTime.parse(j['exitedAt']),
        durationMinutes: j['durationMinutes'], fee: j['fee'], inProgress: j['inProgress'] == true,
      );
}

class PlateFee {
  final String plateNo;
  final int visitCount, totalMinutes, totalFee;
  PlateFee({required this.plateNo, required this.visitCount, required this.totalMinutes, required this.totalFee});
  factory PlateFee.fromJson(Map<String, dynamic> j) =>
      PlateFee(plateNo: j['plateNo'], visitCount: j['visitCount'], totalMinutes: j['totalMinutes'], totalFee: j['totalFee']);
}

class FeeSummary {
  final int year, month, visitCount, totalMinutes, totalFee;
  final List<PlateFee> byPlate;
  FeeSummary({required this.year, required this.month, required this.visitCount, required this.totalMinutes, required this.totalFee, required this.byPlate});
  factory FeeSummary.fromJson(Map<String, dynamic> j) => FeeSummary(
        year: j['year'], month: j['month'], visitCount: j['visitCount'], totalMinutes: j['totalMinutes'],
        totalFee: j['totalFee'], byPlate: (j['byPlate'] as List).map((e) => PlateFee.fromJson(e)).toList(),
      );
}

class PageResult<T> {
  final List<T> content;
  final int page, totalPages;
  final int totalElements;
  PageResult({required this.content, required this.page, required this.totalPages, required this.totalElements});
  bool get hasMore => page + 1 < totalPages;

  factory PageResult.fromJson(Map<String, dynamic> j, T Function(Map<String, dynamic>) f) => PageResult(
        content: (j['content'] as List).map((e) => f(e)).toList(),
        page: j['page'], totalPages: j['totalPages'], totalElements: j['totalElements'],
      );
}
