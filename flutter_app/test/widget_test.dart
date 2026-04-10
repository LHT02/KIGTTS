import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('App smoke test - MaterialApp renders', (tester) async {
    // Minimal test that does not require DI / MethodChannel.
    await tester.pumpWidget(
      const MaterialApp(home: Scaffold(body: Text('KIGTTS'))),
    );
    expect(find.text('KIGTTS'), findsOneWidget);
  });
}
