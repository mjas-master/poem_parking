import 'package:flutter/material.dart';

import '../models/models.dart';

void showError(BuildContext context, String msg) =>
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg), backgroundColor: Colors.red.shade700));

void showInfo(BuildContext context, String msg) => ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));

Future<bool> confirm(BuildContext context, {required String title, required String message, String okLabel = '확인'}) async {
  final r = await showDialog<bool>(
    context: context,
    builder: (_) => AlertDialog(
      title: Text(title),
      content: Text(message),
      actions: [
        TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('취소')),
        FilledButton(onPressed: () => Navigator.pop(context, true), child: Text(okLabel)),
      ],
    ),
  );
  return r == true;
}

class ErrorBox extends StatelessWidget {
  final String message;
  final VoidCallback? onRetry;
  const ErrorBox({super.key, required this.message, this.onRetry});

  @override
  Widget build(BuildContext context) => Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            const Icon(Icons.error_outline, size: 40, color: Colors.redAccent),
            const SizedBox(height: 8),
            Text(message, textAlign: TextAlign.center),
            if (onRetry != null) TextButton(onPressed: onRetry, child: const Text('다시 시도')),
          ]),
        ),
      );
}

class EmptyBox extends StatelessWidget {
  final IconData icon;
  final String message;
  const EmptyBox({super.key, required this.icon, required this.message});

  @override
  Widget build(BuildContext context) => Center(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          Icon(icon, size: 56, color: Colors.grey.shade400),
          const SizedBox(height: 12),
          Text(message, style: TextStyle(color: Colors.grey.shade600)),
        ]),
      );
}

class StatusChip extends StatelessWidget {
  final RegistrationStatus status;
  const StatusChip(this.status, {super.key});

  Color get _color => switch (status) {
        RegistrationStatus.pending => Colors.orange,
        RegistrationStatus.synced => Colors.blue,
        RegistrationStatus.entered => Colors.green,
        RegistrationStatus.exited => Colors.grey,
        RegistrationStatus.cancelled => Colors.grey,
        RegistrationStatus.expired => Colors.grey,
        RegistrationStatus.failed => Colors.red,
        RegistrationStatus.unknown => Colors.grey,
      };

  @override
  Widget build(BuildContext context) => Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
        decoration: BoxDecoration(color: _color.withOpacity(0.12), borderRadius: BorderRadius.circular(12)),
        child: Text(status.label, style: TextStyle(color: _color, fontSize: 12, fontWeight: FontWeight.w600)),
      );
}

/// 차량번호판 스타일 표시
class PlateBadge extends StatelessWidget {
  final String plate;
  final double fontSize;
  const PlateBadge(this.plate, {super.key, this.fontSize = 18});

  @override
  Widget build(BuildContext context) => Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        decoration: BoxDecoration(
          color: Colors.white,
          border: Border.all(color: Colors.black87, width: 1.5),
          borderRadius: BorderRadius.circular(6),
        ),
        child: Text(plate, style: TextStyle(fontSize: fontSize, fontWeight: FontWeight.bold, letterSpacing: 1.5)),
      );
}
