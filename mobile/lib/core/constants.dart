class AppConfig {
  /// Android 에뮬레이터: http://10.0.2.2:8080 / iOS 시뮬레이터: http://localhost:8080
  static const baseUrl = String.fromEnvironment('API_BASE_URL', defaultValue: 'http://10.0.2.2:8080');
  static const apiPrefix = '/api/v1';
}
