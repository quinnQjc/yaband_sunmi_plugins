package com.example.yaband_sunmi_plugins;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.view.View;

import androidx.annotation.NonNull;

import com.tools.command.EscCommand;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import io.flutter.Log;
import io.flutter.app.FlutterActivity;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED;

/** YabandSunmiPluginsPlugin */
public class YabandSunmiPluginsPlugin extends FlutterActivity implements FlutterPlugin, ActivityAware, MethodCallHandler  {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;
  static Context context ;
  private Activity activity;
  public static String TAG = "YabandPay>>>";
  Result returnResult;
  private ThreadPool		threadPool;

  /**
   * 判断打印机所使用指令是否是ESC指令
   */
  private int	id = 0;

  private byte[]		tscmode		= { 0x1f, 0x1b, 0x1f, (byte) 0xfc, 0x01, 0x02, 0x03, 0x33 };
  private byte[]		cpclmode	= { 0x1f, 0x1b, 0x1f, (byte) 0xfc, 0x01, 0x02, 0x03, 0x44 };
  private byte[]		escmode		= { 0x1f, 0x1b, 0x1f, (byte) 0xfc, 0x01, 0x02, 0x03, 0x55 };
  private byte[]		selftest	= { 0x1f, 0x1b, 0x1f, (byte) 0x93, 0x10, 0x11, 0x12, 0x15, 0x16, 0x17, 0x10, 0x00 };
  private int		printcount	= 0;
  private boolean		continuityprint = false;
  public static Context getContext() {
    return context;
  }
  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "yaband_sunmi_plugins");
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    context = activity.getApplicationContext();
    AidlUtil.getInstance().connectPrinterService(context);
    returnResult = result;
    if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + android.os.Build.VERSION.RELEASE);
    } else if(call.method.equals("printOrder")){
      /**打印交易详情*/
      Map<String,Object> transactionDetailData = call.argument("data");
      posPrintReceipt(transactionDetailData);
    }else {
      result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }


  /**打印订单详情*/
  public void posPrintReceipt(Map<String,Object> data) {
    try {
      List<String> refundInfo = (List<String>)data.get("refundInfo");
      Log.i(TAG, "printReceipt Argument: refundInfo="+refundInfo);
      String description = data.get("description")+"";
      if(description!=null && description.length() > 32){
        description = description.substring(0, 32)+"..";
      }
      AidlUtil aidlUtil = AidlUtil.getInstance();
      String merchantName = data.get("merchantName")+"";
//            aidlUtil.printText("PayPro Receipt\n--------------------",34,true,false,1);
      aidlUtil.printText("YabandPay Receipt\n--------------------",34,true,false,1);
      aidlUtil.printText(data.get("storeName")+"",24,true,false,0);
      aidlUtil.printText(data.get("storeAddress1")+"\n"+data.get("storeAddress2")+"\n",24,false,false,0);
      StringBuilder sb = new StringBuilder();
      sb.append("Merchant: ").append(merchantName).append("\n")
              .append("Method: "+data.get("method")).append("\n")
              .append("Trade ID: ").append(data.get("tradeId")).append("\n")
              .append("Order ID: "+data.get("orderId")).append("\n")
              .append("Date: ").append(data.get("date")).append("\n")
              .append("Description: ").append(description).append("\n");
      sb.append("--------------------------------");
      aidlUtil.printText(sb.toString(),24,false,false,0);
      String substring = data.get("tradeId")+"";
      AidlUtil.getInstance().printQr(substring, 5, 1);
      if (refundInfo != null && refundInfo.size() > 0) {
        aidlUtil.printText("Total: "+data.get("amount")+ data.get("currency") +
                " \n--------------------\nSUCCESS\n\n---------------------------",28,true,false,1);
        StringBuilder refundSB= new StringBuilder();
        aidlUtil.printText("Refund detail",28,true,false,1);
        int size = ((refundInfo.size()+1) / 3);
        for (int i = 0; i < size; i++) {
          if(i == (size-1)){
            refundSB.append("Amount: ").append(refundInfo.get(i*3)).append("\n")
                    .append("State: "+refundInfo.get((i*3)+1)).append("\n")
                    .append("Date: ").append(refundInfo.get((i*3)+2)).append("\n\n\n");
          }else{
            refundSB.append("Amount: ").append(refundInfo.get(i*3)).append("\n")
                    .append("State: "+refundInfo.get((i*3)+1)).append("\n")
                    .append("Date: ").append(refundInfo.get((i*3)+2)).append("\n--------------------------------\n");
          }
        }
        aidlUtil.printText(refundSB.toString(),24,false,false,0);
      }else{
        aidlUtil.printText("Total: "+data.get("amount")+ data.get("currency") +
                " \n--------------------\nSUCCESS\n\n\n",28,true,false,1);
      }
    } catch (Exception e) {
      Log.i(TAG,"Print Receipt Fileure."+e.getMessage());
      e.printStackTrace();
    }
  }


  /**
   * 统计页面，打印小票
   */
  public void PosPrintSummaryReceipt(Map<String,String> data) {
    try {
      AidlUtil aidlUtil = AidlUtil.getInstance();
      String Merchant = data.get("merchantName");
      String abbreviation = data.get("storeName");
      String initial = data.get("cashierName");
      aidlUtil.printText("YabandPay Summary\n--------------------",34,true,false,1);
      String info = "Merchant:  "+Merchant+"\nStore:     "+abbreviation+"\nCashier:   "+initial+"";
      aidlUtil.printText(info,22,true,false,0);
      StringBuilder sb = new StringBuilder();
      sb.append(      "Duration:    ").append(data.get("duration")).append("\n")
              .append("Income:      ").append(data.get("okAmount")+"").append("\n")
              .append("TRX:         ").append(data.get("okTRX")).append("\n")
              .append("Refund:      ").append(data.get("refundAmount")+""+"").append("\n")
              .append("TRX:         ").append(data.get("refundTRX")).append("\n")
              .append("--------------------------------");
      aidlUtil.printText(sb.toString(),24,false,false,0);
//      aidlUtil.printText("Total Amount:    "+data.get("okAmount")+""+" EUR\n"
//              +("TRX:             ") +AllTRX,24,true,false,0);
      Date d = new Date();
      SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
      aidlUtil.printText(sdf.format(d)+"\n\n",28,false,false,1);
    } catch (Exception e) {
      Log.i(TAG, "printSummaryReceipt: ");
      e.printStackTrace();
    }
  }
  @Override
  public void onAttachedToActivity(ActivityPluginBinding binding) {
    activity = binding.getActivity();
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    this.onDetachedFromActivity();
  }

  @Override
  public void onReattachedToActivityForConfigChanges(ActivityPluginBinding binding) {
    this.onAttachedToActivity(binding);
  }

  @Override
  public void onDetachedFromActivity() {
    activity = null;
  }

}
