package com.openshamba.watchdog.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.openshamba.watchdog.services.CallLoggerService;
import com.openshamba.watchdog.utils.Constants;
import com.openshamba.watchdog.utils.MobileUtils;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Maina on 3/13/2018.
 */

public class CallReciever extends BroadcastReceiver {

    private SharedPreferences mPrefs;

    @Override
    public void onReceive(Context context, Intent intent) {

        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL) && getResultData() != null) {
            Log.d("NIMZYMAINA","Making a call");
            Intent i = new Intent(context, CallLoggerService.class);
            i.setAction(intent.getAction());
            i.putExtra(Intent.EXTRA_PHONE_NUMBER, getResultData());
            i.setFlags(intent.getFlags());
            context.startService(i);
        }

    }
}
