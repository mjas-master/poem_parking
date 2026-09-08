import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../app/router.dart';
import '../../core/utils/formatters.dart';
import '../../models/models.dart';
import '../../providers/auth_provider.dart';
import '../../providers/data_providers.dart';
import '../../widgets/common.dart';

/// 홈: 현재 유효한 방문차량 + 최근 등록 목록
class VisitorListScreen extends ConsumerWidget {
  const VisitorListScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(authProvider).user!;
    final active = ref.watch(activeRegistrationsProvider);
    final all = ref.watch(registrationsProvider);

    return Scaffold(
      appBar: AppBar(title: Text(user.householdLabel)),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => context.push(Routes.register),
        icon: const Icon(Icons.add),
        label: const Text('방문차량 등록'),
      ),
      body: RefreshIndicator(
        onRefresh: () async {
          ref.invalidate(activeRegistrationsProvider);
          ref.invalidate(registrationsProvider);
        },
        child: ListView(padding: const EdgeInsets.fromLTRB(16, 12, 16, 96), children: [
          _SectionTitle('현재 유효한 방문차량', trailing: active.valueOrNull?.length.toString()),
          active.when(
            loading: () => const Padding(padding: EdgeInsets.all(24), child: Center(child: CircularProgressIndicator())),
            error: (e, _) => ErrorBox(message: e.toString(), onRetry: () => ref.invalidate(activeRegistrationsProvider)),
            data: (list) => list.isEmpty
                ? const Padding(padding: EdgeInsets.symmetric(vertical: 24), child: EmptyBox(icon: Icons.no_crash_outlined, message: '등록된 방문차량이 없습니다'))
                : Column(children: list.map((r) => _RegistrationCard(r, highlight: true)).toList()),
          ),
          const SizedBox(height: 20),
          const _SectionTitle('최근 등록 내역'),
          all.when(
            loading: () => const SizedBox.shrink(),
            error: (e, _) => ErrorBox(message: e.toString()),
            data: (p) {
              final past = p.content.where((r) => !r.status.isActive).toList();
              if (past.isEmpty) return const Padding(padding: EdgeInsets.all(16), child: Text('내역이 없습니다', style: TextStyle(color: Colors.grey)));
              return Column(children: past.map((r) => _RegistrationCard(r)).toList());
            },
          ),
        ]),
      ),
    );
  }
}

class _SectionTitle extends StatelessWidget {
  final String title;
  final String? trailing;
  const _SectionTitle(this.title, {this.trailing});
  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 8),
        child: Row(children: [
          Text(title, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          if (trailing != null) ...[
            const SizedBox(width: 8),
            Text(trailing!, style: TextStyle(color: Theme.of(context).colorScheme.primary, fontWeight: FontWeight.bold)),
          ],
        ]),
      );
}

class _RegistrationCard extends StatelessWidget {
  final Registration r;
  final bool highlight;
  const _RegistrationCard(this.r, {this.highlight = false});

  @override
  Widget build(BuildContext context) => Card(
        elevation: highlight ? 1 : 0,
        color: highlight ? null : Colors.grey.shade50,
        margin: const EdgeInsets.only(bottom: 8),
        child: ListTile(
          onTap: () => context.push(Routes.registration(r.id)),
          leading: PlateBadge(fmtPlate(r.plateNo), fontSize: 15),
          title: Text(r.visitorName?.isNotEmpty == true ? r.visitorName! : '방문자'),
          subtitle: Text('${fmtDateTime(r.visitFrom)} ~ ${fmtDateTime(r.visitTo)}', style: const TextStyle(fontSize: 12)),
          trailing: StatusChip(r.status),
        ),
      );
}
