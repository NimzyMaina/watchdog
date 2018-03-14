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

import com.openshamba.watchdog.events.NewOutgoingCallEvent;
import com.openshamba.watchdog.services.CallLoggerService;
import com.openshamba.watchdog.utils.Constants;
import com.openshamba.watchdog.utils.CustomApplication;
import com.openshamba.watchdog.utils.MobileUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

/**
 * Created by Maina on 3/13/2018.
 */

public class CallReciever extends BaseCallReceiver {

    private SharedPreferences mPrefs;
    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context,intent);
        this.context = context;

        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL) && getResultData() != null) {
            if (!CustomApplication.isMyServiceRunning(CallLoggerService.class)) {
                Log.d("NIMZYMAINA", "Making a call");
                Intent i = new Intent(context, CallLoggerService.class);
                i.setAction(intent.getAction());
                i.putExtra(Intent.EXTRA_PHONE_NUMBER, getResultData());
                i.setFlags(intent.getFlags());
                context.startService(i);
                Log.d("NIMZYMAINA","Making a call service");
            }else{
                EventBus.getDefault().post(new NewOutgoingCallEvent(getResultData()));
                Log.d("NIMZYMAINA","Making a call event");
            }
        }

    }

    @Override
    protected void onIncomingCallStarted(String number, Date start) {

    }

    @Override
    protected void onOutgoingCallStarted(String number, Date start) {

    }

    @Override
    protected void onIncomingCallEnded(String number, Date start, Date end) {

    }

    @Override
    protected void onOutgoingCallEnded(String number, Date start, Date end) {
        Toast.makeText(context,"Call to "+number+" ended!!",Toast.LENGTH_SHORT).show();
        Log.d("NIMZYMAINA","Call to "+number+" ended!!");
    }

    @Override
    protected void onMissedCall(String number, Date start) {

    }
}
