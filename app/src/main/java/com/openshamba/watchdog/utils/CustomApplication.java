package com.openshamba.watchdog.utils;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.DexterBuilder;
import com.openshamba.watchdog.MainActivity;

import java.io.File;
import java.lang.ref.WeakReference;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by Maina on 3/13/2018.
 */

public class CustomApplication extends Application {

    private static Boolean mIsOldMtkDevice = null;
    private static WeakReference<Context> mWeakReference;
    private static boolean mIsActivityVisible;

    private Context context;


    @Override
    public final void onCreate() {
        super.onCreate();
        mWeakReference = new WeakReference<>(getApplicationContext());

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getAppContext());



    }


    // Supported MTK devices
    private static final Set<String> OLD_MTK_DEVICES = new HashSet<>(Arrays.asList(
            new String[]{
                    // Single-core SoC
                    "mt6575",
                    // Dual-core SoC
                    "mt6572",
                    "mt6577",
                    "mt8377",
                    // Quad-core SoC
                    "mt6582",
                    "mt6582m",
                    "mt6589",
                    "mt8389",
                    // Octa-core SoC
                    "mt6592"
            }
    ));

    public static boolean isOldMtkDevice() {
        if (mIsOldMtkDevice == null)
            mIsOldMtkDevice = (OLD_MTK_DEVICES.contains(Build.HARDWARE.toLowerCase()) ||
                    OLD_MTK_DEVICES.contains(System.getProperty("ro.mediatek.platform", "")) ||
                    OLD_MTK_DEVICES.contains(System.getProperty("ro.board.platform", ""))) &&
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP;
        return mIsOldMtkDevice;
    }

    public static void setUp(Context context){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = preferences.edit();

        ArrayList imsi = MobileUtils.getSimIds(context);
        if (imsi.size() > 0 && preferences.getBoolean(Constants.PREF_OTHER[2], false))
            loadTrafficPreferences(imsi);

        Log.d(Constants.LOG,"SIM QTY: "+MobileUtils.isMultiSim(context));
        Log.d(Constants.LOG,"PREF SIM: "+preferences.getInt(Constants.PREF_OTHER[0],-1));
        if (!preferences.contains(Constants.PREF_OTHER[0]))
            edit.putInt(Constants.PREF_OTHER[0], MobileUtils.isMultiSim(context))
                    .apply();

        Log.d(Constants.LOG,"PREF SIM Again: "+preferences.getInt(Constants.PREF_OTHER[0],-2));

        //Store subids
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            SubscriptionManager sm = SubscriptionManager.from(context);
            List<SubscriptionInfo> sl = sm.getActiveSubscriptionInfoList();
            StringBuilder subInfo = new StringBuilder();
            for (SubscriptionInfo si : sl) {
                subInfo.append(si.getSubscriptionId()).append(";");
            }
            subInfo = new StringBuilder(subInfo.substring(0, subInfo.length() - 1));
            preferences.edit()
                    .putString(Constants.PREF_OTHER[1], subInfo.toString())
                    .apply();
        }
    }

    public static void sleep(long time) {
        try {
            TimeUnit.MILLISECONDS.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static Context getAppContext() {
        return mWeakReference.get();
    }

    public static boolean isMyServiceRunning(Class<?> serviceClass) {
        try {
            ActivityManager manager = (ActivityManager) mWeakReference.get().getSystemService(Context.ACTIVITY_SERVICE);
            if (manager != null) {
                for (ActivityManager.RunningServiceInfo serviceInfo : manager.getRunningServices(Integer.MAX_VALUE)) {
                    if (serviceClass.getName().equals(serviceInfo.service.getClassName()))
                        return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public static void putObject(SharedPreferences.Editor editor, String key, Object o) {
        if (o == null)
            editor.putString(key, "null");
        else if (o instanceof String)
            editor.putString(key, (String) o);
        else if (o instanceof Boolean)
            editor.putBoolean(key, (boolean) o);
    }

    public static void loadTrafficPreferences(ArrayList imsi) {
        if (imsi != null && imsi.size() > 0) {
            Context context = mWeakReference.get();
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            int simQuantity = preferences.getInt(Constants.PREF_OTHER[55], 1);
            String path = context.getFilesDir().getParent() + "/shared_prefs/";
            SharedPreferences.Editor editor = preferences.edit();
            SharedPreferences prefSim;
            Map<String, ?> prefsMap;
            String name = Constants.TRAFFIC + "_" + imsi.get(0);
            if (new File(path + name + ".xml").exists()) {
                prefSim = context.getSharedPreferences(name, Context.MODE_PRIVATE);
                prefsMap = prefSim.getAll();
                if (prefsMap.size() != 0)
                    for (String key : prefsMap.keySet()) {
                        Object o = prefsMap.get(key);
                        key = key + 1;
                        putObject(editor, key, o);
                    }
                prefSim = null;
            }
            if (simQuantity >= 2) {
                name = Constants.TRAFFIC + "_" + imsi.get(1);
                if (new File(path + name + ".xml").exists()) {
                    prefSim = context.getSharedPreferences(name, Context.MODE_PRIVATE);
                    prefsMap = prefSim.getAll();
                    if (prefsMap.size() != 0)
                        for (String key : prefsMap.keySet()) {
                            Object o = prefsMap.get(key);
                            key = key + 2;
                            putObject(editor, key, o);
                        }
                    prefSim = null;
                }
            }
            if (simQuantity == 3) {
                name = Constants.TRAFFIC + "_" + imsi.get(2);
                if (new File(path + name + ".xml").exists()) {
                    prefSim = context.getSharedPreferences(name, Context.MODE_PRIVATE);
                    prefsMap = prefSim.getAll();
                    if (prefsMap.size() != 0)
                        for (String key : prefsMap.keySet()) {
                            Object o = prefsMap.get(key);
                            key = key + 3;
                            putObject(editor, key, o);
                        }
                    prefSim = null;
                }
            }
            editor.apply();
        }
    }

    public static boolean isActivityVisible() {
        return mIsActivityVisible;
    }

    public static void resumeActivity() {
        mIsActivityVisible = true;
    }

    public static void pauseActivity() {
        mIsActivityVisible = false;
    }

    public static boolean isScreenOn() {
        PowerManager pm = (PowerManager) mWeakReference.get().getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
                return pm.isInteractive();
            else
                return pm.isScreenOn();
        }
        return false;
    }
}
