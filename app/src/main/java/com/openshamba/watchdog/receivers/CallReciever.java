package com.openshamba.watchdog.receivers;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.CallLog;
import android.support.v4.app.ActivityCompat;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.openshamba.watchdog.data.DatabaseCreator;
import com.openshamba.watchdog.data.ServiceGenerator;
import com.openshamba.watchdog.data.responses.ApiResponse;
import com.openshamba.watchdog.entities.Call;
import com.openshamba.watchdog.entities.doas.CallDAO;
import com.openshamba.watchdog.events.NewOutgoingCallEvent;
import com.openshamba.watchdog.services.CallLoggerService;
import com.openshamba.watchdog.utils.Constants;
import com.openshamba.watchdog.utils.CustomApplication;
import com.openshamba.watchdog.utils.MobileUtils;
import com.openshamba.watchdog.utils.SessionManager;

import org.greenrobot.eventbus.EventBus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Maina on 3/13/2018.
 */

public class CallReciever extends BaseCallReceiver {

    private SharedPreferences mPrefs;
    private Context context;
    private CallDAO callDAO = null;
    private final Executor executor = Executors.newFixedThreadPool(2);
    private Call call;
    private SessionManager session;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context,intent);
        this.context = context;
        callDAO = DatabaseCreator.getWatchDogDatabase(context).CallDatabase();
        session = new SessionManager(context);

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

            getLastCall(number,start,end);
    }

    @Override
    protected void onMissedCall(String number, Date start) {

    }

    private void getCallDetails(Date start,Date end) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        CustomApplication.sleep(1000);
        Cursor managedCursor = context.getContentResolver().query(CallLog.Calls.CONTENT_URI, null,
                null, null, null);
        int number = managedCursor.getColumnIndex(CallLog.Calls.NUMBER);
        int type = managedCursor.getColumnIndex(CallLog.Calls.TYPE);
        int date = managedCursor.getColumnIndex(CallLog.Calls.DATE);
        int duration = managedCursor.getColumnIndex(CallLog.Calls.DURATION);
        int acc = managedCursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID);

        managedCursor.moveToLast();
        String phNumber = managedCursor.getString(number);
        String callType = managedCursor.getString(type);
        String callDate = managedCursor.getString(date);
        Date callDayTime = new Date(Long.valueOf(callDate));
        String callDuration = managedCursor.getString(duration);
        String callsim = managedCursor.getString(acc);

        String dir = null;
        int dircode = Integer.parseInt(callType);

        switch (dircode) {
            case CallLog.Calls.OUTGOING_TYPE:
                dir = "OUTGOING";
                break;

            case CallLog.Calls.INCOMING_TYPE:
                dir = "INCOMING";
                break;

            case CallLog.Calls.MISSED_TYPE:
                dir = "MISSED";
                break;
        }

        if(dir.equals("OUTGOING")){
            //whatever you want here
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            call.setDuration(callDuration);
            call.setComplete(true);
            call.setStart(sdf.format(start));
            call.setEnd(sdf.format(end));
            executor.execute(() -> {
                callDAO.insertCall(call);
                Log.d("NIMZYMAINA","Call updated");
                Log.d("NIMZYMAINA",call.toString());
            });

            final Call c = call;

            retrofit2.Call<ApiResponse> call = ServiceGenerator.getClient(session.getKeyApiKey()).saveCall(this.call);

            call.enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(retrofit2.Call<ApiResponse> call, Response<ApiResponse> response) {
                    if(response.isSuccessful()){
                        c.setSynced(true);
                        executor.execute(() -> {
                            callDAO.insertCall(c);
                            Log.d("NIMZYMAINA","Call Synced");
                            Log.d("NIMZYMAINA",call.toString());
                        });
                    }else{
                        Log.d("NIMZYMAINA","Call server reached but failed to sync");
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<ApiResponse> call, Throwable t) {
                    Log.d("NIMZYMAINA","Call server not reachable");
                }
            });
        }

        managedCursor.close();

    }

    public void getLastCall(String phone,Date start,Date end){
        new AsyncTask<Void, Void, Call>() {
            @Override
            protected Call doInBackground(Void... params) {
                return callDAO.getLastCall(phone);
            }
            @Override
            protected void onPostExecute(Call calld) {
                call = calld;
                if(call != null){
                    getCallDetails(start,end);
                }
            }
        }.execute();
    }

    private String printDifference(Date startDate, Date endDate) {
        //milliseconds
        long different = endDate.getTime() - startDate.getTime();

        System.out.println("startDate : " + startDate);
        System.out.println("endDate : "+ endDate);
        System.out.println("different : " + different);

        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        long elapsedDays = different / daysInMilli;
        different = different % daysInMilli;

        long elapsedHours = different / hoursInMilli;
        different = different % hoursInMilli;

        long elapsedMinutes = different / minutesInMilli;
        different = different % minutesInMilli;

        long elapsedSeconds = different / secondsInMilli;

        return  elapsedMinutes + " minutes, " + elapsedSeconds +" seconds";
//
//        System.out.printf(
//                "%d days, %d hours, %d minutes, %d seconds%n",
//                elapsedDays, elapsedHours, elapsedMinutes, elapsedSeconds);
    }
}
