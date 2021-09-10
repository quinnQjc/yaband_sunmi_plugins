
import 'dart:async';

import 'package:flutter/services.dart';

class YabandSunmiPlugins {
  static const MethodChannel _channel =
      const MethodChannel('yaband_sunmi_plugins');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<void> printTransactionDetail(Map<String,Map<String,dynamic>> sendData) async {
    try {
      final int result = await _channel.invokeMethod("printOrder",sendData);
    } on PlatformException catch (e) {
      print("Failed to Print : '${e.message}'.");
    }
  }

  static Future<void> printSummary(Map<String,Map<String,String>> sendData) async {
    try {
      await _channel.invokeMethod("printSummay",sendData);
    } on PlatformException catch (e) {
      print("Failed to Print : '${e.message}'.");
    }
  }
}
