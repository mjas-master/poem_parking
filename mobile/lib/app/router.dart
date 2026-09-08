import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../features/auth/apartment_verify_screen.dart';
import '../features/auth/login_screen.dart';
import '../features/auth/splash_screen.dart';
import '../features/fee/fee_screen.dart';
import '../features/history/history_screen.dart';
import '../features/home/home_shell.dart';
import '../features/settings/settings_screen.dart';
import '../features/visitor/register_visitor_screen.dart';
import '../features/visitor/registration_detail_screen.dart';
import '../features/visitor/visitor_list_screen.dart';
import '../providers/auth_provider.dart';

class Routes {
  static const splash = '/';
  static const login = '/login';
  static const verify = '/verify';
  static const visitors = '/home/visitors';
  static const register = '/home/visitors/register';
  static const history = '/home/history';
  static const fee = '/home/fee';
  static const settings = '/home/settings';
  static String registration(int id) => '/home/visitors/$id';
}

final routerProvider = Provider<GoRouter>((ref) {
  final auth = ref.watch(authProvider);
  return GoRouter(
    initialLocation: Routes.splash,
    redirect: (context, state) {
      final loc = state.matchedLocation;
      if (auth.status == AuthStatus.unknown) return loc == Routes.splash ? null : Routes.splash;
      if (auth.status == AuthStatus.unauthenticated) return loc == Routes.login ? null : Routes.login;
      final user = auth.user!;
      if (!user.verified) return loc == Routes.verify ? null : Routes.verify;
      if (loc == Routes.splash || loc == Routes.login || loc == Routes.verify) return Routes.visitors;
      return null;
    },
    routes: [
      GoRoute(path: Routes.splash, builder: (_, __) => const SplashScreen()),
      GoRoute(path: Routes.login, builder: (_, __) => const LoginScreen()),
      GoRoute(path: Routes.verify, builder: (_, __) => const ApartmentVerifyScreen()),
      StatefulShellRoute.indexedStack(
        builder: (_, __, shell) => HomeShell(shell: shell),
        branches: [
          StatefulShellBranch(routes: [
            GoRoute(
              path: Routes.visitors,
              builder: (_, __) => const VisitorListScreen(),
              routes: [
                GoRoute(path: 'register', builder: (_, __) => const RegisterVisitorScreen()),
                GoRoute(
                  path: ':id',
                  builder: (_, s) => RegistrationDetailScreen(id: int.parse(s.pathParameters['id']!)),
                ),
              ],
            ),
          ]),
          StatefulShellBranch(routes: [GoRoute(path: Routes.history, builder: (_, __) => const HistoryScreen())]),
          StatefulShellBranch(routes: [GoRoute(path: Routes.fee, builder: (_, __) => const FeeScreen())]),
          StatefulShellBranch(routes: [GoRoute(path: Routes.settings, builder: (_, __) => const SettingsScreen())]),
        ],
      ),
    ],
  );
});
