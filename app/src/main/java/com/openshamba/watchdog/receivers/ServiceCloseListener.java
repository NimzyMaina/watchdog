package com.openshamba.watchdog.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.openshamba.watchdog.services.CallLoggerService;
import com.openshamba.watchdog.utils.Constants;

public class ServiceCloseListener extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(ServiceCloseListener.class.getSimpleName(), "Service Stops! Oooooooooooooppppssssss!!!!");
        Intent startIntent = new Intent(context, CallLoggerService.class);
        startIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
        context.startService(startIntent);
    }
}