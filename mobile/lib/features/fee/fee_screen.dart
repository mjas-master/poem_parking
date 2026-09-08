import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/utils/formatters.dart';
import '../../providers/auth_provider.dart';
import '../../providers/data_providers.dart';
import '../../widgets/common.dart';

class FeeScreen extends ConsumerStatefulWidget {
  const FeeScreen({super.key});
  @override
  ConsumerState<FeeScreen> createState() => _State();
}

class _State extends ConsumerState<FeeScreen> {
  late int _year;
  late int _month;

  @override
  void initState() {
    super.initState();
    final now = DateTime.now();
    _year = now.year;
    _month = now.month;
  }

  void _shift(int delta) {
    final d = DateTime(_year, _month + delta, 1);
    setState(() {
      _year = d.year;
      _month = d.month;
    });
  }

  @override
  Widget build(BuildContext context) {
    final summary = ref.watch(feeSummaryProvider((_year, _month)));
    final apt = ref.watch(authProvider).user?.apartment;
    final isCurrent = DateTime.now().year == _year && DateTime.now().month == _month;

    return Scaffold(
      appBar: AppBar(title: const Text('방문 주차료')),
      body: Column(children: [
        Row(mainAxisAlignment: MainAxisAlignment.center, children: [
          IconButton(onPressed: () => _shift(-1), icon: const Icon(Icons.chevron_left)),
          Text('$_year년 $_month월', style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          IconButton(onPressed: isCurrent ? null : () => _shift(1), icon: const Icon(Icons.chevron_right)),
        ]),
        Expanded(
          child: summary.when(
            loading: () => const Center(child: CircularProgressIndicator()),
            error: (e, _) => ErrorBox(message: e.toString(), onRetry: () => ref.invalidate(feeSummaryProvider((_year, _month)))),
            data: (s) => ListView(padding: const EdgeInsets.all(16), children: [
              Card(
                color: Theme.of(context).colorScheme.primary,
                child: Padding(
                  padding: const EdgeInsets.all(20),
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    const Text('이번 달 주차료 합계', style: TextStyle(color: Colors.white70)),
                    const SizedBox(height: 4),
                    Text(fmtWon(s.totalFee), style: const TextStyle(color: Colors.white, fontSize: 32, fontWeight: FontWeight.bold)),
                    const SizedBox(height: 12),
                    Text('방문 ${s.visitCount}회 · 총 ${fmtDuration(s.totalMinutes)}', style: const TextStyle(color: Colors.white)),
                  ]),
                ),
              ),
              if (apt != null)
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 12),
                  child: Text('요금 기준: ${apt.freeMinutes}분 무료 · ${apt.unitMinutes}분당 ${fmtWon(apt.unitFee)}'
                      '${apt.dailyCapFee > 0 ? ' · 1일 최대 ${fmtWon(apt.dailyCapFee)}' : ''}\n주차 중인 차량은 현재 시각 기준 잠정 요금입니다.',
                      style: const TextStyle(color: Colors.grey, fontSize: 12)),
                ),
              const Text('차량별 집계', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
              const SizedBox(height: 8),
              if (s.byPlate.isEmpty) const Padding(padding: EdgeInsets.all(24), child: Center(child: Text('방문 기록이 없습니다', style: TextStyle(color: Colors.grey)))),
              ...s.byPlate.map((p) => ListTile(
                    leading: PlateBadge(fmtPlate(p.plateNo), fontSize: 14),
                    title: Text('${p.visitCount}회 · ${fmtDuration(p.totalMinutes)}'),
                    trailing: Text(fmtWon(p.totalFee), style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                  )),
            ]),
          ),
        ),
      ]),
    );
  }
}
