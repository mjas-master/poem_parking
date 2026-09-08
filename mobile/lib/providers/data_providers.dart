import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/models.dart';
import 'auth_provider.dart';

/// 현재 유효한 방문차량 목록
final activeRegistrationsProvider = FutureProvider.autoDispose<List<Registration>>(
  (ref) => ref.watch(apiServiceProvider).activeRegistrations(),
);

/// 전체 등록 목록(첫 페이지). 무한 스크롤은 화면에서 처리
final registrationsProvider = FutureProvider.autoDispose<PageResult<Registration>>(
  (ref) => ref.watch(apiServiceProvider).registrations(),
);

final registrationDetailProvider = FutureProvider.autoDispose.family<Registration, int>(
  (ref, id) => ref.watch(apiServiceProvider).registration(id),
);

/// 입출차 이력 (페이지 누적)
class HistoryListNotifier extends StateNotifier<AsyncValue<List<VisitHistory>>> {
  final Ref ref;
  int _page = 0;
  bool _hasMore = true;
  bool _loading = false;

  HistoryListNotifier(this.ref) : super(const AsyncValue.loading()) {
    refresh();
  }

  bool get hasMore => _hasMore;

  Future<void> refresh() async {
    _page = 0;
    _hasMore = true;
    state = const AsyncValue.loading();
    try {
      final p = await ref.read(apiServiceProvider).histories(page: 0);
      _hasMore = p.hasMore;
      state = AsyncValue.data(p.content);
    } catch (e, st) {
      state = AsyncValue.error(e, st);
    }
  }

  Future<void> loadMore() async {
    if (!_hasMore || _loading) return;
    _loading = true;
    try {
      final p = await ref.read(apiServiceProvider).histories(page: _page + 1);
      _page += 1;
      _hasMore = p.hasMore;
      state = AsyncValue.data([...state.value ?? [], ...p.content]);
    } finally {
      _loading = false;
    }
  }

  Future<void> delete(int id) async {
    await ref.read(apiServiceProvider).deleteHistory(id);
    state = AsyncValue.data((state.value ?? []).where((h) => h.id != id).toList());
  }
}

final historyListProvider = StateNotifierProvider.autoDispose<HistoryListNotifier, AsyncValue<List<VisitHistory>>>(
  (ref) => HistoryListNotifier(ref),
);

/// 요금 집계 (년, 월)
final feeSummaryProvider = FutureProvider.autoDispose.family<FeeSummary, (int, int)>(
  (ref, ym) => ref.watch(apiServiceProvider).feeSummary(ym.$1, ym.$2),
);
