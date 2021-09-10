import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:yaband_sunmi_plugins/yaband_sunmi_plugins.dart';

void main() {
  const MethodChannel channel = MethodChannel('yaband_sunmi_plugins');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await YabandSunmiPlugins.platformVersion, '42');
  });
}
