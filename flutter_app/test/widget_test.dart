import 'package:flutter_test/flutter_test.dart';
import 'package:kgtts_app/app.dart';

void main() {
  testWidgets('App smoke test', (WidgetTester tester) async {
    await tester.pumpWidget(const KgttsApp());
    // App should render without crashing.
    expect(find.byType(KgttsApp), findsOneWidget);
  });
}
