package com.openshamba.watchdog.services;

import android.app.Service;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by Maina on 3/17/2018.
 */

public class SmsLoggerService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public final void onCreate() {
        super.onCreate();
        ContentObserver observer=new MySentSmsHandler(getApplicationContext());
        getContentResolver().registerContentObserver(
                Uri.parse("content://sms"), true, observer
        );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("NIMZYMAINA","SMS logger started");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d("NIMZYMAINA","SMS Logger closed");
        super.onDestroy();
    }
}
