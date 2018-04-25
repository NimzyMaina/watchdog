package com.openshamba.watchdog.services;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.openshamba.watchdog.MainActivity;
import com.openshamba.watchdog.R;
import com.openshamba.watchdog.data.DatabaseCreator;
import com.openshamba.watchdog.data.WatchDogDatabase;
import com.openshamba.watchdog.entities.Call;
import com.openshamba.watchdog.entities.doas.CallDAO;
import com.openshamba.watchdog.events.NewOutgoingCallEvent;
import com.openshamba.watchdog.utils.Constants;
import com.openshamba.watchdog.utils.CustomApplication;
import com.openshamba.watchdog.utils.MobileUtils;
import com.openshamba.watchdog.utils.SessionManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by Maina on 3/13/2018.
 */

public class CallLoggerService extends Service {

    private BroadcastReceiver mCallStartedReceiver, mCallAnsweredReceiver, mCallEndedReceiver;
    private Intent mDialogIntent = null;
    private Context mContext;
    private SharedPreferences mPrefs;
    private boolean mIsOutgoing = false;
    private Service mService = null;
    private String phone;

    // Pop up shit
    private WindowManager windowManager;
    private RelativeLayout chatheadView;
    private WindowManager.LayoutParams params;
    private FrameLayout content;
    private Spinner spinner;
    private ImageView close, bobo;
    private TextView title;
    private Button personal, business;
    private ContentObserver observer;
    private SessionManager session;

    private final Executor executor = Executors.newFixedThreadPool(2);
    private CallDAO callDAO = null;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public final void onCreate() {
        super.onCreate();
        callDAO = DatabaseCreator.getWatchDogDatabase(getApplicationContext()).CallDatabase();
        mContext = CustomApplication.getAppContext();
        mService = this;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        session = new SessionManager(getApplicationContext());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            observer = new MySentSmsHandler(getApplicationContext());
        } else {
            observer = new MySupportSentSmsHandler(getApplicationContext());
        }

        getContentResolver().registerContentObserver(
                Uri.parse("content://sms"), true, observer
        );

        setUpDialogWindow();

    }

    @Override
    public final int onStartCommand(Intent intent, int flags, int startId) {

        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);

        if (intent != null && intent.getAction() != null && intent.getAction().equals(Constants.ACTION.SHOW_HUD)) {
            String number = intent.getStringExtra("phone");
            phone = number;
            fetchContactName(number);
            showDialog(true);
            mIsOutgoing = true;
        }

        if (intent != null && intent.getAction() != null && intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {

            Log.d("NIMZYMAINA", "Making a call received");

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

        } else if (intent != null && intent.getAction() != null && intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setAction(Constants.ACTION.MAIN_ACTION);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);

            Bitmap icon = BitmapFactory.decodeResource(getResources(),
                    R.mipmap.ic_launcher);

            Notification notification = new NotificationCompat.Builder(this)
                    .setContentTitle("WatchDog Service")
                    .setTicker("WatchDog Service")
                    .setContentText("WatchDog Service")
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setLargeIcon(
                            Bitmap.createScaledBitmap(icon, 128, 128, false))
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .build();
            startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,
                    notification);
        } else if (intent != null && intent.getAction() != null && intent.getAction().equals(Constants.ACTION.STOPFOREGROUND_ACTION)) {
            Log.d("NIMZYMAINA", "Received Stop Foreground Intent");
            stopForeground(true);
            stopSelf();
        }

        return START_STICKY;
    }

    private void startTask(final String number) {

        final TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        phone = number;

        mIsOutgoing = false;

        if (tm != null) {
            tm.listen(new PhoneStateListener() {
                @Override
                public void onCallStateChanged(int state, String incomingNumber) {

                    if (CustomApplication.isMyServiceRunning(CallLoggerService.class) && !mIsOutgoing)
                        switch (state) {
                            case TelephonyManager.CALL_STATE_OFFHOOK:
                                final int sim;
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                                    sim = MobileUtils.getActiveSimForCall(mContext, mPrefs.getInt(Constants.PREF_OTHER[0], 1));
                                else {
                                    Log.d("NIMZYMAINA", mPrefs.getString(Constants.PREF_OTHER[1], ""));
                                    ArrayList<String> list = new ArrayList<>(Arrays.asList(mPrefs.getString(Constants.PREF_OTHER[1], "").split(";")));
                                    sim = MobileUtils.getActiveSimForCallM(mContext, mPrefs.getInt(Constants.PREF_OTHER[0], 1), list);
                                }

                                Log.d("NIMZYMAINA", "Making a call on sim " + sim);
                                Toast.makeText(mContext, "SIM : " + sim, Toast.LENGTH_SHORT).show();
                                // todo - update sim data for call

                                filterSim(number,Integer.toString(sim));

//                                if (sim == 0) {
//                                    fetchContactName(number);
//                                    showDialog(true);
//                                    mIsOutgoing = true;
//                                } else {
//                                    mService.stopSelf();
//                                }

                                break;
                        }

                }
            }, PhoneStateListener.LISTEN_CALL_STATE);
        }

    }

    private void setUpDialogWindow() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 40;
        params.y = 200;

        chatheadView = (RelativeLayout) inflater.inflate(R.layout.pop_up_dialog, null);
        close = (ImageView) chatheadView.findViewById(R.id.close);
        content = (FrameLayout) chatheadView.findViewById(R.id.content);
        bobo = (ImageView) chatheadView.findViewById(R.id.imagenotileft);
        spinner = (Spinner) chatheadView.findViewById(R.id.spinner);
        title = (TextView) chatheadView.findViewById(R.id.title);
        personal = (Button) chatheadView.findViewById(R.id.personal);
        business = (Button) chatheadView.findViewById(R.id.business);

        ArrayAdapter adapter = ArrayAdapter.createFromResource(this,
                R.array.codes_arrays, R.layout.spinner_item);

        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinner.setAdapter(adapter);

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(false);
                // stopService(new Intent(getApplicationContext(), CallLoggerService.class));
            }
        });

        personal.setOnClickListener(v -> {
            Toast.makeText(getApplicationContext(), "Personal Call", Toast.LENGTH_SHORT).show();
            String contact = getContactName(phone, mContext);
            Call call = new Call(phone, contact, "PERSONAL");
            saveCall(call);
            showDialog(false);
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phone));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_FROM_BACKGROUND);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            session.setCall(true);
            mContext.startActivity(intent);
        });

        business.setOnClickListener(v -> {
            String x = spinner.getSelectedItem().toString();
            Toast.makeText(getApplicationContext(), "Business client " + x, Toast.LENGTH_SHORT).show();
            String contact = getContactName(phone, mContext);
            Call call = new Call(phone, contact, "BUSINESS", x);
            saveCall(call);
            showDialog(false);
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phone));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_FROM_BACKGROUND);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            session.setCall(true);
            mContext.startActivity(intent);
        });

    }

    private void showDialog(boolean state){
        if(state){
            windowManager.addView(chatheadView, params);
        }else{
            windowManager.removeViewImmediate(chatheadView);
        }
    }

    private String getContactName(final String phoneNumber, Context context) {
        Uri uri=Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,Uri.encode(phoneNumber));

        String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME};

        String contactName = phoneNumber;
        Cursor cursor=context.getContentResolver().query(uri,projection,null,null,null);

        if (cursor != null) {
            if(cursor.moveToFirst()) {
                contactName=cursor.getString(0);
            }
            cursor.close();
        }

        return contactName;
    }

    private void fetchContactName(String phone){

        if(hasPermission(Manifest.permission.READ_CONTACTS)){
            title.setText(getContactName(phone,mContext));
        }else{
            title.setText(phone);
        }

    }

    private boolean hasPermission(String permission){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return(mContext.checkSelfPermission(permission)== PackageManager.PERMISSION_GRANTED);
        }

        return true;

    }

    @Subscribe
    public final void onMessageEvent(NewOutgoingCallEvent event) {
        startTask(event.number);
    }

    public void saveCall(Call call){
        executor.execute(() -> {
            callDAO.insertCall(call);
            Log.d("NIMZYMAINA","Call Saved");
            Log.d("NIMZYMAINA",call.toString());
        });
    }

    public void filterSim(String phone,String sim){
        new AsyncTask<Void, Void, Call>() {
            @Override
            protected Call doInBackground(Void... params) {
                return callDAO.getLastCall(phone);
            }
            @Override
            protected void onPostExecute(Call calld) {
                Call call = calld;
                if(call != null){
                    call.setSim(sim);
                    saveCall(call);
                }
            }
        }.execute();
    }

    @Override
    public final void onDestroy() {
        super.onDestroy();

        Log.i("NIMZYMAINA", "In onDestroy");
        Intent broadcastIntent = new Intent(Constants.ACTION.RESTART_SERVICE);
        sendBroadcast(broadcastIntent);

        if (mCallAnsweredReceiver != null)
            unregisterReceiver(mCallAnsweredReceiver);
        if (mCallEndedReceiver != null)
            unregisterReceiver(mCallEndedReceiver);
        if (mCallStartedReceiver != null)
            unregisterReceiver(mCallStartedReceiver);

        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);

        if(observer != null)
            getContentResolver().unregisterContentObserver(observer);
    }

}
