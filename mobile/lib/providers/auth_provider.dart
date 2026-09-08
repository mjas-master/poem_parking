import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/api/api_client.dart';
import '../core/api/api_service.dart';
import '../core/storage/token_storage.dart';
import '../models/models.dart';

enum AuthStatus { unknown, unauthenticated, authenticated }

class AuthState {
  final AuthStatus status;
  final User? user;
  const AuthState({required this.status, this.user});
  AuthState copyWith({AuthStatus? status, User? user}) => AuthState(status: status ?? this.status, user: user ?? this.user);
}

final tokenStorageProvider = Provider((_) => TokenStorage());

final apiClientProvider = Provider((ref) => ApiClient(
      tokenStorage: ref.watch(tokenStorageProvider),
      onUnauthorized: () => ref.read(authProvider.notifier).logout(),
    ));

final apiServiceProvider = Provider((ref) => ApiService(ref.watch(apiClientProvider)));

class AuthNotifier extends StateNotifier<AuthState> {
  final Ref ref;
  AuthNotifier(this.ref) : super(const AuthState(status: AuthStatus.unknown)) {
    _restore();
  }

  ApiService get _api => ref.read(apiServiceProvider);
  TokenStorage get _storage => ref.read(tokenStorageProvider);

  Future<void> _restore() async {
    final token = await _storage.read();
    if (token == null) {
      state = const AuthState(status: AuthStatus.unauthenticated);
      return;
    }
    try {
      final user = await _api.me();
      state = AuthState(status: AuthStatus.authenticated, user: user);
    } catch (_) {
      await _storage.clear();
      state = const AuthState(status: AuthStatus.unauthenticated);
    }
  }

  Future<void> requestOtp(String phone) => _api.requestOtp(phone);

  Future<void> login(String phone, String otp, String? name) async {
    final (token, user) = await _api.login(phone, otp, name);
    await _storage.write(token);
    state = AuthState(status: AuthStatus.authenticated, user: user);
  }

  Future<void> verifyApartment({required String apartmentCode, required String dong, required String ho, required String verifyCode}) async {
    final user = await _api.verifyApartment(apartmentCode: apartmentCode, dong: dong, ho: ho, verifyCode: verifyCode);
    state = state.copyWith(user: user);
  }

  Future<void> refreshMe() async {
    final user = await _api.me();
    state = state.copyWith(user: user);
  }

  Future<void> logout() async {
    await _storage.clear();
    state = const AuthState(status: AuthStatus.unauthenticated);
  }
}

final authProvider = StateNotifierProvider<AuthNotifier, AuthState>((ref) => AuthNotifier(ref));
