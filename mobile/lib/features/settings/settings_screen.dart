import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/constants.dart';
import '../../providers/auth_provider.dart';
import '../../widgets/common.dart';

class SettingsScreen extends ConsumerWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(authProvider).user!;
    return Scaffold(
      appBar: AppBar(title: const Text('설정')),
      body: ListView(children: [
        ListTile(
          leading: const CircleAvatar(child: Icon(Icons.person)),
          title: Text(user.name),
          subtitle: Text(user.phone),
        ),
        ListTile(
          leading: const Icon(Icons.apartment),
          title: const Text('내 세대'),
          subtitle: Text(user.householdLabel),
        ),
        const Divider(),
        ListTile(
          leading: const Icon(Icons.notifications_outlined),
          title: const Text('입출차 알림'),
          subtitle: const Text('추후 지원 예정'),
          trailing: Switch(value: false, onChanged: null),
        ),
        const ListTile(
          leading: Icon(Icons.dns_outlined),
          title: Text('서버'),
          subtitle: Text(AppConfig.baseUrl),
        ),
        const ListTile(leading: Icon(Icons.info_outline), title: Text('앱 버전'), subtitle: Text('0.1.0')),
        const Divider(),
        ListTile(
          leading: const Icon(Icons.logout, color: Colors.red),
          title: const Text('로그아웃', style: TextStyle(color: Colors.red)),
          onTap: () async {
            if (await confirm(context, title: '로그아웃', message: '로그아웃 할까요?')) {
              ref.read(authProvider.notifier).logout();
            }
          },
        ),
      ]),
    );
  }
}
