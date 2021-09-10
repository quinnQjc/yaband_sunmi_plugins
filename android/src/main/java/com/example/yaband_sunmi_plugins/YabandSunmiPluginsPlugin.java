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
import static com.example.yaband_sunmi_plugins.Constant.MESSAGE_UPDATE_PARAMETER;
import static com.example.yaband_sunmi_plugins.DeviceConnFactoryManager.CONN_STATE_FAILED;

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

  private static final int	REQUEST_CODE = 0x004;


  /**
   * 连接状态断开
   */
  private static final int CONN_STATE_DISCONN = 0x007;


  /**
   * 使用打印机指令错误
   */
  private static final int PRINTER_COMMAND_ERROR = 0x008;


  /**
   * ESC查询打印机实时状态指令
   */
  private byte[] esc = { 0x10, 0x04, 0x02 };


  /**
   * CPCL查询打印机实时状态指令
   */
  private byte[] cpcl = { 0x1b, 0x68 };


  /**
   * TSC查询打印机状态指令
   */
  private byte[] tsc = { 0x1b, '!', '?' };

  private static final int	CONN_MOST_DEVICES	= 0x11;
  private static final int	CONN_PRINTER		= 0x12;
  @Override
  protected void onActivityResult( int requestCode, int resultCode, Intent data )
  {
    Log.i(TAG, "onActivityResult: resultCode="+resultCode+"  requestCode="+requestCode);
    super.onActivityResult( requestCode, resultCode, data );
    if ( resultCode == RESULT_OK )
    {
      switch ( requestCode )
      {
        case CONN_MOST_DEVICES:
          Log.i(TAG, "CONN_MOST_DEVICES="+CONN_MOST_DEVICES+"  id="+id);
          id = data.getIntExtra( "id", -1 );
          if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] != null &&
                  DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState() )
          {
            returnResult.success("connected  "+getConnDeviceInfo());
          } else {
            returnResult.success("disconnect");
          }
          break;
        default:
          break;
      }
    }
  }

  /**
   * 打印机状态查询
   *
   * @param view
   */
  public void btnPrinterState( View view )
  {
    /* 打印机状态查询 */
    if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null ||
            !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState() )
    {
      Utils.toast( this, "btnPrinterState");
      return;
    }
    ThreadPool.getInstantiation().addTask( new Runnable()
    {
      @Override
      public void run()
      {
        Vector<Byte> data = new Vector<>( esc.length );
        if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.ESC )
        {
          for ( int i = 0; i < esc.length; i++ )
          {
            data.add( esc[i] );
          }
          DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately( data );
        }else if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.TSC )
        {
          for ( int i = 0; i < tsc.length; i++ )
          {
            data.add( tsc[i] );
          }
          DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately( data );
        }else if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.CPCL )
        {
          for ( int i = 0; i < cpcl.length; i++ )
          {
            data.add( cpcl[i] );
          }
          DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately( data );
        }
      }
    } );
  }


  /**
   * 重新连接回收上次连接的对象，避免内存泄漏
   */
  private void closeport()
  {
    if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] != null &&DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].mPort != null )
    {
      DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].reader.cancel();
      DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].mPort.closePort();
      DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].mPort = null;
    }
  }

  private String getConnDeviceInfo()
  {
    String				str				= " ... ";
    DeviceConnFactoryManager	deviceConnFactoryManager	= DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id];
    if ( deviceConnFactoryManager != null
            && deviceConnFactoryManager.getConnState() )
    {
      if ( "USB".equals( deviceConnFactoryManager.getConnMethod().toString() ) )
      {
        str	+= "USB\n";
        str	+= "USB Name: " + deviceConnFactoryManager.usbDevice().getDeviceName();
      } else if ( "WIFI".equals( deviceConnFactoryManager.getConnMethod().toString() ) )
      {
        str	+= "WIFI\n";
        str	+= "IP: " + deviceConnFactoryManager.getIp() + "\t";
        str	+= "Port: " + deviceConnFactoryManager.getPort();
      } else if ( "BLUETOOTH".equals( deviceConnFactoryManager.getConnMethod().toString() ) )
      {
        str	+= "BLUETOOTH\n";
        str	+= "MacAddress: " + deviceConnFactoryManager.getMacAddress();
      } else if ( "SERIAL_PORT".equals( deviceConnFactoryManager.getConnMethod().toString() ) )
      {
        str	+= "SERIAL_PORT\n";
        str	+= "Path: " + deviceConnFactoryManager.getSerialPortPath() + "\t";
        str	+= "Baudrate: " + deviceConnFactoryManager.getBaudrate();
      }
    }
    return(str);
  }

  private BroadcastReceiver receiver = new BroadcastReceiver()
  {
    @Override
    public void onReceive( Context context, Intent intent )
    {
      Log.i(TAG, "onReceive: "+intent.getAction());
      String action = intent.getAction();
      switch ( action )
      {
        /* Usb连接断开、蓝牙连接断开广播 */
        case ACTION_USB_DEVICE_DETACHED:
          mHandler.obtainMessage( CONN_STATE_DISCONN ).sendToTarget();
          break;
        case DeviceConnFactoryManager.ACTION_CONN_STATE:
          int state = intent.getIntExtra( DeviceConnFactoryManager.STATE, -1 );
          int deviceId = intent.getIntExtra( DeviceConnFactoryManager.DEVICE_ID, -1 );
          switch ( state )
          {
            case DeviceConnFactoryManager.CONN_STATE_DISCONNECT:
              if ( id == deviceId )
              {

              }
              break;
            case DeviceConnFactoryManager.CONN_STATE_CONNECTING:
//                            returnResult.success("CONNECTING");
              break;
            case DeviceConnFactoryManager.CONN_STATE_CONNECTED:
//                            returnResult.success("CONNECTED");
              break;
            case CONN_STATE_FAILED:
              Utils.toast( context, getString( R.string.str_conn_fail ) );
              break;
            default:
              break;
          }
          break;
        default:
          break;
      }
    }
  };
  private Handler mHandler = new Handler()
  {
    @Override
    public void handleMessage( Message msg )
    {
      switch ( msg.what )
      {
        case CONN_STATE_DISCONN:
          if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] != null || !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState() )
          {
            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].closePort( id );
            Utils.toast( YabandSunmiPluginsPlugin.this,"CONN_STATE_DISCONN");
          }
          break;
        case PRINTER_COMMAND_ERROR:
          Utils.toast( YabandSunmiPluginsPlugin.this, "PRINTER_COMMAND_ERROR" );
          break;
        case CONN_PRINTER:
          Utils.toast( YabandSunmiPluginsPlugin.this, "CONN_PRINTER");
          break;
        case MESSAGE_UPDATE_PARAMETER:
          String strIp = msg.getData().getString( "Ip" );
          String strPort = msg.getData().getString( "Port" );
          /* 初始化端口信息 */
          new DeviceConnFactoryManager.Build()
                  /* 设置端口连接方式 */
                  .setConnMethod( DeviceConnFactoryManager.CONN_METHOD.WIFI )
                  /* 设置端口IP地址 */
                  .setIp( strIp )
                  /* 设置端口ID（主要用于连接多设备） */
                  .setId( id )
                  /* 设置连接的热点端口号 */
                  .setPort( Integer.parseInt( strPort ) )
                  .build();
          threadPool = ThreadPool.getInstantiation();
          threadPool.addTask( new Runnable()
          {
            @Override
            public void run()
            {
              DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].openPort();
            }
          } );
          break;
        default:
          new DeviceConnFactoryManager.Build()
                  /* 设置端口连接方式 */
                  .setConnMethod( DeviceConnFactoryManager.CONN_METHOD.WIFI )
                  /* 设置端口IP地址 */
                  .setIp( "192.168.2.227" )
                  /* 设置端口ID（主要用于连接多设备） */
                  .setId( id )
                  /* 设置连接的热点端口号 */
                  .setPort( 9100 )
                  .build();
          threadPool.addTask( new Runnable()
          {
            @Override
            public void run()
            {
              DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].openPort();
            }
          } );
          break;
      }
    }
  };

  /**
   * 打印自检页
   * @param view
   */
  public void btnPrintSelftest( View view )
  {
    threadPool = ThreadPool.getInstantiation();
    threadPool.addTask( new Runnable()
    {
      @Override
      public void run()
      {
        if ( DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null ||
                !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState() )
        {
          mHandler.obtainMessage( CONN_PRINTER ).sendToTarget();
          return;
        }
        Vector<Byte> data = new Vector<>( tscmode.length );
        for ( int i = 0; i < selftest.length; i++ )
        {
          data.add( selftest[i] );
        }
        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately( data );
      }
    } );
  }

}
