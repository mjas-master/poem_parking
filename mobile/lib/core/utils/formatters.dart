import 'package:intl/intl.dart';

final _dt = DateFormat('M/d(E) HH:mm', 'ko_KR');
final _d = DateFormat('yyyy.MM.dd', 'ko_KR');
final _won = NumberFormat('#,###', 'ko_KR');

String fmtDateTime(DateTime? d) => d == null ? '-' : _dt.format(d);
String fmtDate(DateTime d) => _d.format(d);
String fmtWon(int? v) => v == null ? '-' : '${_won.format(v)}원';

String fmtDuration(int? minutes) {
  if (minutes == null) return '-';
  final h = minutes ~/ 60, m = minutes % 60;
  if (h == 0) return '$m분';
  return m == 0 ? '$h시간' : '$h시간 $m분';
}

/// 차량번호 표시용: 12가3456 -> 12가 3456
String fmtPlate(String p) {
  final m = RegExp(r'^(.*?)(\d{4})$').firstMatch(p);
  return m == null ? p : '${m.group(1)} ${m.group(2)}';
}
