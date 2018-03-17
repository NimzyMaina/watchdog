package com.openshamba.watchdog.services;

import android.app.Service;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;

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
}
