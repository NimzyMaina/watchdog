package com.openshamba.watchdog.utils;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.DexterBuilder;
import com.openshamba.watchdog.MainActivity;

import java.lang.ref.WeakReference;
import java.security.Permission;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by Maina on 3/13/2018.
 */

public class CustomApplication extends Application {

    private static Boolean mIsOldMtkDevice = null;
    private static WeakReference<Context> mWeakReference;

    private Context context;


    @Override
    public final void onCreate() {
        super.onCreate();
        mWeakReference = new WeakReference<>(getApplicationContext());

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

        if (!preferences.contains(Constants.PREF_OTHER[0])){
            edit.putInt(Constants.PREF_OTHER[0],MobileUtils.isMultiSim(context)).apply();
        }

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
}
