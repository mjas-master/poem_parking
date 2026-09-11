import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../providers/auth_provider.dart';
import '../../widgets/common.dart';

/// 휴대폰 OTP 로그인 (목업: OTP 123456)
class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});
  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _phone = TextEditingController();
  final _otp = TextEditingController();
  final _name = TextEditingController();
  bool _otpSent = false;
  bool _busy = false;

  Future<void> _sendOtp() async {
    final phone = _phone.text.replaceAll('-', '').trim();
    if (phone.isEmpty) {
      showError(context, '휴대폰 번호를 입력하세요.');
      return;
    }
    setState(() => _busy = true);
    try {
      await ref.read(authProvider.notifier).requestOtp(phone);
      setState(() => _otpSent = true);
      if (mounted) showInfo(context, '인증번호가 발송되었습니다. (아무 값이나 입력 가능)');
    } catch (e) {
      if (mounted) showError(context, e.toString());
    } finally {
      setState(() => _busy = false);
    }
  }

  Future<void> _login() async {
    setState(() => _busy = true);
    try {
      final otpVal = _otp.text.trim().isEmpty ? '123456' : _otp.text.trim();
      await ref.read(authProvider.notifier).login(
            _phone.text.replaceAll('-', '').trim(), otpVal, _name.text.trim().isEmpty ? null : _name.text.trim());
    } catch (e) {
      if (mounted) showError(context, e.toString());
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('로그인')),
      body: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          const SizedBox(height: 16),
          const Icon(Icons.local_parking, size: 64, color: Color(0xFF1E5EFF)),
          const SizedBox(height: 8),
          const Text('휴대폰 번호로 시작하기', textAlign: TextAlign.center, style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600)),
          const SizedBox(height: 24),
          TextField(
            controller: _phone,
            keyboardType: TextInputType.phone,
            enabled: !_otpSent,
            decoration: const InputDecoration(labelText: '휴대폰 번호', hintText: '01012345678'),
          ),
          const SizedBox(height: 12),
          if (!_otpSent) ...[
            FilledButton(onPressed: _busy ? null : _sendOtp, child: const Text('인증번호 받기')),
            const SizedBox(height: 8),
            OutlinedButton(
              onPressed: _busy ? null : () async {
                setState(() => _busy = true);
                try {
                  await ref.read(authProvider.notifier).login(
                        '01012345678', '123456', '테스트유저');
                } catch (e) {
                  if (mounted) showError(context, e.toString());
                } finally {
                  if (mounted) setState(() => _busy = false);
                }
              },
              child: const Text('테스트 빠른 시작 (인증 및 동호수 자동 세팅)'),
            ),
          ] else ...[
            TextField(
              controller: _otp,
              keyboardType: TextInputType.text,
              decoration: const InputDecoration(labelText: '인증번호 (아무 값이나 입력 가능)'),
            ),
            TextField(controller: _name, decoration: const InputDecoration(labelText: '이름 (선택)')),
            const SizedBox(height: 16),
            FilledButton(onPressed: _busy ? null : _login, child: const Text('확인')),
            TextButton(onPressed: () => setState(() => _otpSent = false), child: const Text('번호 다시 입력')),
          ],
        ]),
      ),
    );
  }
}
