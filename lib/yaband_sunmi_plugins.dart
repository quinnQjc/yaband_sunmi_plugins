
import 'dart:async';

import 'package:flutter/services.dart';

class YabandSunmiPlugins {
  static const MethodChannel _channel =
      const MethodChannel('yaband_sunmi_plugins');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<void> printOrder() async {
    String merchantName = "testmerchantName";
    String storeName = "teststoreName";
    Map<String,String> transactionDetailData = {'merchantName':merchantName,
      'storeName':storeName,'storeAddress1':"storeAddress1",'storeAddress2':"storeAddress2",
      'tradeId':"trade_id",
      'method':"sub_pay_method",'description':"description",
      'date': "created_at",
      'amount':"currency",
      'orderId':"order_id",
    };
    Map<String,Map<String,String>> sendData = {'data':transactionDetailData};
    try {
      final int result = await _channel.invokeMethod("printOrder",sendData);
    } on PlatformException catch (e) {
      print("Failed to Print : '${e.message}'.");
    }
  }
}
