import 'package:dio/dio.dart';

import '../constants.dart';
import '../storage/token_storage.dart';
import 'api_exception.dart';

class ApiClient {
  final TokenStorage tokenStorage;
  final void Function()? onUnauthorized;
  late final Dio dio;

  ApiClient({required this.tokenStorage, this.onUnauthorized}) {
    dio = Dio(BaseOptions(
      baseUrl: '${AppConfig.baseUrl}${AppConfig.apiPrefix}',
      connectTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 15),
      headers: {'Content-Type': 'application/json'},
    ));
    dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) async {
        final token = await tokenStorage.read();
        if (token != null && !options.path.startsWith('/auth')) {
          options.headers['Authorization'] = 'Bearer $token';
        }
        handler.next(options);
      },
      onError: (e, handler) {
        if (e.response?.statusCode == 401) onUnauthorized?.call();
        handler.next(e);
      },
    ));
  }

  Future<T> _wrap<T>(Future<Response<dynamic>> Function() call, T Function(dynamic data) map) async {
    try {
      final res = await call();
      return map(res.data);
    } on DioException catch (e) {
      final data = e.response?.data;
      if (data is Map<String, dynamic>) {
        throw ApiException(
          statusCode: e.response?.statusCode,
          code: data['code']?.toString() ?? 'ERROR',
          message: data['message']?.toString() ?? '요청에 실패했습니다.',
          details: data['details'] as Map<String, dynamic>?,
        );
      }
      throw ApiException(
        statusCode: e.response?.statusCode,
        code: 'NETWORK',
        message: e.type == DioExceptionType.connectionError || e.type == DioExceptionType.connectionTimeout
            ? '서버에 연결할 수 없습니다.'
            : (e.message ?? '네트워크 오류'),
      );
    }
  }

  Future<T> get<T>(String path, T Function(dynamic) map, {Map<String, dynamic>? query}) =>
      _wrap(() => dio.get(path, queryParameters: query), map);

  Future<T> post<T>(String path, T Function(dynamic) map, {Object? body}) =>
      _wrap(() => dio.post(path, data: body), map);

  Future<T> delete<T>(String path, T Function(dynamic) map) => _wrap(() => dio.delete(path), map);
}
