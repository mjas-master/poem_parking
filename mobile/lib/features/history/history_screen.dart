import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/utils/formatters.dart';
import '../../models/models.dart';
import '../../providers/data_providers.dart';
import '../../widgets/common.dart';

/// 입/출차 이력 (조회 + 스와이프 삭제)
class HistoryScreen extends ConsumerStatefulWidget {
  const HistoryScreen({super.key});
  @override
  ConsumerState<HistoryScreen> createState() => _State();
}

class _State extends ConsumerState<HistoryScreen> {
  final _scroll = ScrollController();

  @override
  void initState() {
    super.initState();
    _scroll.addListener(() {
      if (_scroll.position.pixels > _scroll.position.maxScrollExtent - 200) {
        ref.read(historyListProvider.notifier).loadMore();
      }
    });
  }

  Future<void> _delete(VisitHistory h) async {
    if (h.inProgress) {
      showError(context, '주차 중인 차량 이력은 삭제할 수 없습니다.');
      return;
    }
    final ok = await confirm(context, title: '이력 삭제', message: '${fmtPlate(h.plateNo)} 이력을 목록에서 삭제할까요?\n(요금 집계에는 계속 포함됩니다)', okLabel: '삭제');
    if (!ok) return;
    try {
      await ref.read(historyListProvider.notifier).delete(h.id);
    } catch (e) {
      if (mounted) showError(context, e.toString());
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(historyListProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('입출차 이력')),
      body: RefreshIndicator(
        onRefresh: () => ref.read(historyListProvider.notifier).refresh(),
        child: state.when(
          loading: () => const Center(child: CircularProgressIndicator()),
          error: (e, _) => ErrorBox(message: e.toString(), onRetry: () => ref.read(historyListProvider.notifier).refresh()),
          data: (list) => list.isEmpty
              ? const EmptyBox(icon: Icons.history, message: '입출차 이력이 없습니다')
              : ListView.separated(
                  controller: _scroll,
                  padding: const EdgeInsets.symmetric(vertical: 8),
                  itemCount: list.length,
                  separatorBuilder: (_, __) => const Divider(height: 1),
                  itemBuilder: (_, i) {
                    final h = list[i];
                    return Dismissible(
                      key: ValueKey(h.id),
                      direction: h.inProgress ? DismissDirection.none : DismissDirection.endToStart,
                      confirmDismiss: (_) async {
                        await _delete(h);
                        return false; // 목록 갱신은 notifier가 처리
                      },
                      background: Container(
                        color: Colors.red,
                        alignment: Alignment.centerRight,
                        padding: const EdgeInsets.only(right: 20),
                        child: const Icon(Icons.delete, color: Colors.white),
                      ),
                      child: ListTile(
                        leading: Icon(h.inProgress ? Icons.local_parking : Icons.check_circle_outline, color: h.inProgress ? Colors.green : Colors.grey),
                        title: Row(children: [
                          Text(fmtPlate(h.plateNo), style: const TextStyle(fontWeight: FontWeight.bold)),
                          if (h.visitorName != null) ...[const SizedBox(width: 8), Text(h.visitorName!, style: const TextStyle(color: Colors.grey))],
                        ]),
                        subtitle: Text('입차 ${fmtDateTime(h.enteredAt)}\n출차 ${h.inProgress ? '주차 중' : fmtDateTime(h.exitedAt)}'),
                        isThreeLine: true,
                        trailing: Column(mainAxisAlignment: MainAxisAlignment.center, crossAxisAlignment: CrossAxisAlignment.end, children: [
                          Text(h.inProgress ? '-' : fmtWon(h.fee), style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
                          Text(fmtDuration(h.durationMinutes), style: const TextStyle(fontSize: 12, color: Colors.grey)),
                        ]),
                        onLongPress: () => _delete(h),
                      ),
                    );
                  },
                ),
        ),
      ),
    );
  }
}
