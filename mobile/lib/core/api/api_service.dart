import '../../models/models.dart';
import 'api_client.dart';

/// 백엔드 REST API 래퍼 (엔드포인트 1:1)
class ApiService {
  final ApiClient _c;
  ApiService(this._c);

  // ---- auth
  Future<void> requestOtp(String phone) => _c.post('/auth/otp', (_) {}, body: {'phone': phone});

  Future<(String token, User user)> login(String phone, String otp, String? name) =>
      _c.post('/auth/login', (d) => (d['accessToken'] as String, User.fromJson(d['user'])),
          body: {'phone': phone, 'otp': otp, 'name': name});

  Future<List<ApartmentSummary>> apartments() =>
      _c.get('/auth/apartments', (d) => (d as List).map((e) => ApartmentSummary.fromJson(e)).toList());

  Future<User> me() => _c.get('/me', (d) => User.fromJson(d));

  Future<User> verifyApartment({required String apartmentCode, required String dong, required String ho, required String verifyCode}) =>
      _c.post('/me/verify-apartment', (d) => User.fromJson(d),
          body: {'apartmentCode': apartmentCode, 'dong': dong, 'ho': ho, 'verifyCode': verifyCode});

  // ---- registrations
  Future<Registration> register({
    required String plateNo, String? visitorName, String? visitorPhone, String? purpose,
    required DateTime visitFrom, required DateTime visitTo,
  }) =>
      _c.post('/registrations', (d) => Registration.fromJson(d), body: {
        'plateNo': plateNo, 'visitorName': visitorName, 'visitorPhone': visitorPhone, 'purpose': purpose,
        'visitFrom': visitFrom.toIso8601String().split('.').first,
        'visitTo': visitTo.toIso8601String().split('.').first,
      });

  Future<PageResult<Registration>> registrations({int page = 0, int size = 20}) =>
      _c.get('/registrations', (d) => PageResult.fromJson(d, Registration.fromJson), query: {'page': page, 'size': size});

  Future<List<Registration>> activeRegistrations() =>
      _c.get('/registrations/active', (d) => (d as List).map((e) => Registration.fromJson(e)).toList());

  Future<Registration> registration(int id) => _c.get('/registrations/$id', (d) => Registration.fromJson(d));

  Future<Registration> cancelRegistration(int id) => _c.delete('/registrations/$id', (d) => Registration.fromJson(d));

  // ---- histories
  Future<PageResult<VisitHistory>> histories({int page = 0, int size = 20}) =>
      _c.get('/histories', (d) => PageResult.fromJson(d, VisitHistory.fromJson), query: {'page': page, 'size': size});

  Future<void> deleteHistory(int id) => _c.delete('/histories/$id', (_) {});

  // ---- fees
  Future<FeeSummary> feeSummary(int year, int month) =>
      _c.get('/fees/summary', (d) => FeeSummary.fromJson(d), query: {'year': year, 'month': month});
}
