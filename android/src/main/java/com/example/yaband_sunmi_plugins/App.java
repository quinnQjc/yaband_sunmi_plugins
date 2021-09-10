package com.example.yaband_sunmi_plugins;

import android.app.Application;
import android.content.Context;
import android.util.Log;

/**
 * Created by Administrator
 *
 * @author 猿史森林
 *         Date: 2017/11/28
 *         Class description:
 */
public class App extends Application {

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("App Application ", "onCreate: mContext==null "+(mContext==null));
        mContext = getApplicationContext();
        Log.i("App Application ", "onCreate: mContext==null "+(mContext==null));
    }

    public static Context getContext() {
        return YabandSunmiPluginsPlugin.getContext();
    }
}
