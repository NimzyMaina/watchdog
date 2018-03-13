package com.openshamba.watchdog.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.openshamba.watchdog.utils.Constants;
import com.openshamba.watchdog.utils.CustomApplication;
import com.openshamba.watchdog.utils.MobileUtils;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Maina on 3/13/2018.
 */

public class CallLoggerService extends Service {

    private BroadcastReceiver mCallStartedReceiver, mCallAnsweredReceiver, mCallEndedReceiver;
    private Intent mDialogIntent = null;
    private Context mContext;
    private SharedPreferences mPrefs;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public final void onCreate() {
        super.onCreate();
        mContext = CustomApplication.getAppContext();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

    }

    @Override
    public final int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && intent.getAction() != null && intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {

            Log.d("NIMZYMAINA","Making a call received");

            mCallStartedReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    //Toast.makeText(mContext, "Call started!", Toast.LENGTH_LONG).show();
                    CustomApplication.sleep(1000);
                    if (mDialogIntent != null)
                        mContext.startActivity(mDialogIntent);
                }
            };
            IntentFilter start = new IntentFilter(Constants.OUTGOING_CALL_STARTED);
            registerReceiver(mCallStartedReceiver, start);

            startTask(intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER));

        }

        return START_STICKY;
    }

    private void startTask(String number) {

        final TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

        if (tm != null) {
            tm.listen(new PhoneStateListener(){
                @Override
                public void onCallStateChanged(int state, String incomingNumber) {

                    switch (state) {
                        case TelephonyManager.CALL_STATE_OFFHOOK:
                            final int sim;
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                                sim = MobileUtils.getActiveSimForCall(mContext, mPrefs.getInt(Constants.PREF_OTHER[0], 1));
                            else {
                                Log.d("NIMZYMAINA",mPrefs.getString(Constants.PREF_OTHER[1], ""));
                                ArrayList<String> list = new ArrayList<>(Arrays.asList(mPrefs.getString(Constants.PREF_OTHER[1], "").split(";")));
                                sim = MobileUtils.getActiveSimForCallM(mContext, mPrefs.getInt(Constants.PREF_OTHER[0], 1), list);
                            }

                            Log.d("NIMZYMAINA","Making a call on sim "+ sim);
                            Toast.makeText(mContext,"SIM LOLO!! : " + sim,Toast.LENGTH_LONG).show();

                            break;
                    }

                }
            }, PhoneStateListener.LISTEN_CALL_STATE);
        }

    }

}
