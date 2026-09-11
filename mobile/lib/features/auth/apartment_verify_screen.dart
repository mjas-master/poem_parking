import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../models/models.dart';
import '../../providers/auth_provider.dart';
import '../../widgets/common.dart';

final _apartmentsProvider = FutureProvider.autoDispose((ref) => ref.watch(apiServiceProvider).apartments());

/// 아파트/세대 인증 화면 (목업: 인증코드 000000)
class ApartmentVerifyScreen extends ConsumerStatefulWidget {
  const ApartmentVerifyScreen({super.key});
  @override
  ConsumerState<ApartmentVerifyScreen> createState() => _State();
}

class _State extends ConsumerState<ApartmentVerifyScreen> {
  final _form = GlobalKey<FormState>();
  final _dong = TextEditingController();
  final _ho = TextEditingController();
  final _code = TextEditingController();
  ApartmentSummary? _apt;
  bool _busy = false;

  Future<void> _submit() async {
    if (_apt == null) {
      final apts = ref.read(_apartmentsProvider).valueOrNull;
      if (apts != null && apts.isNotEmpty) {
        _apt = apts.first;
      } else {
        showError(context, '아파트를 선택하세요.');
        return;
      }
    }
    if (!_form.currentState!.validate()) return;
    setState(() => _busy = true);
    try {
      final vCode = _code.text.trim().isEmpty ? '000000' : _code.text.trim();
      await ref.read(authProvider.notifier).verifyApartment(
            apartmentCode: _apt!.code, dong: _dong.text.trim(), ho: _ho.text.trim(), verifyCode: vCode);
    } catch (e) {
      if (mounted) showError(context, e.toString());
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final apts = ref.watch(_apartmentsProvider);
    return Scaffold(
      appBar: AppBar(
        title: const Text('아파트 인증'),
        actions: [IconButton(onPressed: () => ref.read(authProvider.notifier).logout(), icon: const Icon(Icons.logout))],
      ),
      body: Form(
        key: _form,
        child: ListView(padding: const EdgeInsets.all(20), children: [
          const Text('거주하시는 아파트와 동/호수를 입력하고\n관리사무소에서 받은 인증코드를 입력하세요.', style: TextStyle(color: Colors.black54)),
          const SizedBox(height: 20),
          apts.when(
            loading: () => const LinearProgressIndicator(),
            error: (e, _) => ErrorBox(message: e.toString(), onRetry: () => ref.invalidate(_apartmentsProvider)),
            data: (list) => DropdownButtonFormField<ApartmentSummary>(
              value: _apt,
              decoration: const InputDecoration(labelText: '아파트'),
              items: list.map((a) => DropdownMenuItem(value: a, child: Text('${a.name} (${a.code})'))).toList(),
              onChanged: (v) => setState(() => _apt = v),
            ),
          ),
          const SizedBox(height: 12),
          Row(children: [
            Expanded(
              child: TextFormField(
                controller: _dong,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(labelText: '동'),
                validator: (v) => (v == null || v.trim().isEmpty) ? '필수' : null,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: TextFormField(
                controller: _ho,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(labelText: '호'),
                validator: (v) => (v == null || v.trim().isEmpty) ? '필수' : null,
              ),
            ),
          ]),
          const SizedBox(height: 12),
          TextFormField(
            controller: _code,
            keyboardType: TextInputType.text,
            decoration: const InputDecoration(labelText: '인증코드', helperText: '어느 값을 입력하더라도 인증됩니다.'),
            validator: (v) => null,
          ),
          const SizedBox(height: 20),
          FilledButton(onPressed: _busy ? null : _submit, child: const Text('인증하기')),
        ]),
      ),
    );
  }
}
