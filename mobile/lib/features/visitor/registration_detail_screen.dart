import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/utils/formatters.dart';
import '../../models/models.dart';
import '../../providers/auth_provider.dart';
import '../../providers/data_providers.dart';
import '../../widgets/common.dart';

class RegistrationDetailScreen extends ConsumerWidget {
  final int id;
  const RegistrationDetailScreen({super.key, required this.id});

  Future<void> _cancel(BuildContext context, WidgetRef ref, Registration r) async {
    final ok = await confirm(context, title: '등록 취소', message: '${fmtPlate(r.plateNo)} 차량의 방문 등록을 취소할까요?', okLabel: '취소하기');
    if (!ok) return;
    try {
      await ref.read(apiServiceProvider).cancelRegistration(r.id);
      ref.invalidate(activeRegistrationsProvider);
      ref.invalidate(registrationsProvider);
      ref.invalidate(registrationDetailProvider(id));
      if (context.mounted) showInfo(context, '취소되었습니다.');
    } catch (e) {
      if (context.mounted) showError(context, e.toString());
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final reg = ref.watch(registrationDetailProvider(id));
    return Scaffold(
      appBar: AppBar(title: const Text('등록 상세')),
      body: reg.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => ErrorBox(message: e.toString(), onRetry: () => ref.invalidate(registrationDetailProvider(id))),
        data: (r) => ListView(padding: const EdgeInsets.all(20), children: [
          Center(child: PlateBadge(fmtPlate(r.plateNo), fontSize: 26)),
          const SizedBox(height: 12),
          Center(child: StatusChip(r.status)),
          if (r.status == RegistrationStatus.failed && r.failReason != null)
            Padding(padding: const EdgeInsets.only(top: 8), child: Text(r.failReason!, textAlign: TextAlign.center, style: const TextStyle(color: Colors.red))),
          const SizedBox(height: 24),
          _row('방문자', r.visitorName ?? '-'),
          _row('연락처', r.visitorPhone ?? '-'),
          _row('방문 목적', r.purpose ?? '-'),
          _row('방문 시작', fmtDateTime(r.visitFrom)),
          _row('방문 종료', fmtDateTime(r.visitTo)),
          _row('등록 일시', fmtDateTime(r.createdAt)),
          _row('시스템 반영', r.syncedAt == null ? '대기 중' : fmtDateTime(r.syncedAt)),
          const SizedBox(height: 32),
          if (r.status.cancellable)
            OutlinedButton.icon(
              style: OutlinedButton.styleFrom(foregroundColor: Colors.red, minimumSize: const Size.fromHeight(48)),
              onPressed: () => _cancel(context, ref, r),
              icon: const Icon(Icons.cancel_outlined),
              label: const Text('등록 취소'),
            ),
          if (r.status == RegistrationStatus.entered)
            const Text('입차한 차량은 취소할 수 없습니다. 출차 시 자동으로 완료 처리됩니다.', textAlign: TextAlign.center, style: TextStyle(color: Colors.grey)),
          TextButton(onPressed: () => context.pop(), child: const Text('닫기')),
        ]),
      ),
    );
  }

  Widget _row(String k, String v) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 8),
        child: Row(children: [
          SizedBox(width: 90, child: Text(k, style: const TextStyle(color: Colors.grey))),
          Expanded(child: Text(v, style: const TextStyle(fontSize: 15))),
        ]),
      );
}
