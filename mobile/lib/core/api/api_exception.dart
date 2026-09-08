class ApiException implements Exception {
  final int? statusCode;
  final String code;
  final String message;
  final Map<String, dynamic>? details;

  ApiException({this.statusCode, required this.code, required this.message, this.details});

  bool get isUnauthorized => statusCode == 401;

  @override
  String toString() => message;
}
