package com.openshamba.watchdog.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;

import com.openshamba.watchdog.events.MobileConnectionEvent;
import com.openshamba.watchdog.events.NoConnectivityEvent;
import com.openshamba.watchdog.services.TrafficCountService;
import com.openshamba.watchdog.services.WatchDogService;
import com.openshamba.watchdog.utils.Constants;
import com.openshamba.watchdog.utils.CustomApplication;
import com.openshamba.watchdog.utils.MobileUtils;

import org.greenrobot.eventbus.EventBus;

public class ConnectivityChangeReceiver extends BroadcastReceiver {

    @Override
    public final void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit()
                .putInt(Constants.PREF_OTHER[0], MobileUtils.isMultiSim(context))
                .apply();

        if (intent.getAction() != null && intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, Boolean.FALSE) ||
                    !MobileUtils.isMobileDataActive(context)) {
                if (CustomApplication.isMyServiceRunning(TrafficCountService.class))
                    EventBus.getDefault().post(new NoConnectivityEvent());
            } else {
                //start WatchDogService
                if (prefs.getBoolean(Constants.PREF_OTHER[4], true))
                    context.startService(new Intent(context, WatchDogService.class));
                if (!CustomApplication.isMyServiceRunning(TrafficCountService.class) &&
                        !prefs.getBoolean(Constants.PREF_OTHER[3], false)) {
                    Intent i = new Intent(context, TrafficCountService.class);
                    i.setAction(intent.getAction());
                    i.putExtras(intent.getExtras());
                    i.setFlags(intent.getFlags());
                    context.startService(i);
                } else if (CustomApplication.isMyServiceRunning(TrafficCountService.class))
                    EventBus.getDefault().post(new MobileConnectionEvent());
            }
        }
    }
}