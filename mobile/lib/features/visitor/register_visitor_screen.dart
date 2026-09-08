import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/utils/formatters.dart';
import '../../providers/auth_provider.dart';
import '../../providers/data_providers.dart';
import '../../widgets/common.dart';

class RegisterVisitorScreen extends ConsumerStatefulWidget {
  const RegisterVisitorScreen({super.key});
  @override
  ConsumerState<RegisterVisitorScreen> createState() => _State();
}

class _State extends ConsumerState<RegisterVisitorScreen> {
  final _form = GlobalKey<FormState>();
  final _plate = TextEditingController();
  final _name = TextEditingController();
  final _phone = TextEditingController();
  final _purpose = TextEditingController();
  late DateTime _from;
  late DateTime _to;
  bool _busy = false;

  static final _plateRegex = RegExp(r'^(\d{2,3}[가-힣]\d{4}|[가-힣]{2}\d{1,2}[가-힣]\d{4})$');

  @override
  void initState() {
    super.initState();
    final now = DateTime.now();
    _from = DateTime(now.year, now.month, now.day, now.hour, (now.minute ~/ 10) * 10);
    _to = _from.add(const Duration(hours: 4));
  }

  Future<void> _pick(bool isFrom) async {
    final base = isFrom ? _from : _to;
    final d = await showDatePicker(
        context: context, initialDate: base, firstDate: DateTime.now().subtract(const Duration(days: 1)), lastDate: DateTime.now().add(const Duration(days: 30)));
    if (d == null || !mounted) return;
    final t = await showTimePicker(context: context, initialTime: TimeOfDay.fromDateTime(base));
    if (t == null) return;
    final v = DateTime(d.year, d.month, d.day, t.hour, t.minute);
    setState(() {
      if (isFrom) {
        _from = v;
        if (!_to.isAfter(_from)) _to = _from.add(const Duration(hours: 4));
      } else {
        _to = v;
      }
    });
  }

  void _quick(Duration d) => setState(() => _to = _from.add(d));

  Future<void> _submit() async {
    if (!_form.currentState!.validate()) return;
    if (!_to.isAfter(_from)) {
      showError(context, '종료 시간은 시작 시간 이후여야 합니다.');
      return;
    }
    setState(() => _busy = true);
    try {
      await ref.read(apiServiceProvider).register(
            plateNo: _plate.text.replaceAll(RegExp(r'[\s-]'), ''),
            visitorName: _name.text.trim().isEmpty ? null : _name.text.trim(),
            visitorPhone: _phone.text.trim().isEmpty ? null : _phone.text.trim(),
            purpose: _purpose.text.trim().isEmpty ? null : _purpose.text.trim(),
            visitFrom: _from,
            visitTo: _to,
          );
      ref.invalidate(activeRegistrationsProvider);
      ref.invalidate(registrationsProvider);
      if (mounted) {
        showInfo(context, '방문차량이 등록되었습니다. 주차관리 시스템에 전송 중입니다.');
        context.pop();
      }
    } catch (e) {
      if (mounted) showError(context, e.toString());
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final apt = ref.watch(authProvider).user?.apartment;
    return Scaffold(
      appBar: AppBar(title: const Text('방문차량 등록')),
      body: Form(
        key: _form,
        child: ListView(padding: const EdgeInsets.all(20), children: [
          TextFormField(
            controller: _plate,
            autofocus: true,
            textCapitalization: TextCapitalization.none,
            style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold, letterSpacing: 2),
            decoration: const InputDecoration(labelText: '차량번호', hintText: '12가3456'),
            validator: (v) {
              final p = (v ?? '').replaceAll(RegExp(r'[\s-]'), '');
              if (p.isEmpty) return '차량번호를 입력하세요';
              if (!_plateRegex.hasMatch(p)) return '형식이 올바르지 않습니다 (예: 12가3456)';
              return null;
            },
          ),
          const SizedBox(height: 16),
          TextFormField(controller: _name, decoration: const InputDecoration(labelText: '방문자 이름 (선택)')),
          const SizedBox(height: 12),
          TextFormField(controller: _phone, keyboardType: TextInputType.phone, decoration: const InputDecoration(labelText: '방문자 연락처 (선택)')),
          const SizedBox(height: 12),
          TextFormField(controller: _purpose, decoration: const InputDecoration(labelText: '방문 목적 (선택)', hintText: '예: 가족 방문, 택배, 수리')),
          const SizedBox(height: 24),
          const Text('방문 기간', style: TextStyle(fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          _TimeTile(label: '시작', value: fmtDateTime(_from), onTap: () => _pick(true)),
          _TimeTile(label: '종료', value: fmtDateTime(_to), onTap: () => _pick(false)),
          const SizedBox(height: 8),
          Wrap(spacing: 8, children: [
            ActionChip(label: const Text('+2시간'), onPressed: () => _quick(const Duration(hours: 2))),
            ActionChip(label: const Text('+6시간'), onPressed: () => _quick(const Duration(hours: 6))),
            ActionChip(label: const Text('당일'), onPressed: () => setState(() => _to = DateTime(_from.year, _from.month, _from.day, 23, 59))),
            ActionChip(label: const Text('+1일'), onPressed: () => _quick(const Duration(days: 1))),
          ]),
          if (apt != null) ...[
            const SizedBox(height: 24),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(color: Colors.blue.shade50, borderRadius: BorderRadius.circular(8)),
              child: Text(
                '요금 안내: 최초 ${apt.freeMinutes}분 무료, 이후 ${apt.unitMinutes}분당 ${fmtWon(apt.unitFee)}'
                '${apt.dailyCapFee > 0 ? ' (1일 최대 ${fmtWon(apt.dailyCapFee)})' : ''}',
                style: const TextStyle(fontSize: 13),
              ),
            ),
          ],
          const SizedBox(height: 24),
          FilledButton(onPressed: _busy ? null : _submit, child: _busy ? const SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2)) : const Text('등록하기')),
        ]),
      ),
    );
  }
}

class _TimeTile extends StatelessWidget {
  final String label, value;
  final VoidCallback onTap;
  const _TimeTile({required this.label, required this.value, required this.onTap});
  @override
  Widget build(BuildContext context) => ListTile(
        contentPadding: EdgeInsets.zero,
        leading: SizedBox(width: 40, child: Text(label, style: const TextStyle(color: Colors.grey))),
        title: Text(value, style: const TextStyle(fontSize: 16)),
        trailing: const Icon(Icons.edit_calendar_outlined),
        onTap: onTap,
      );
}
