package com.openshamba.watchdog.services;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.ContactsContract;
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

import com.openshamba.watchdog.R;
import com.openshamba.watchdog.data.DatabaseCreator;
import com.openshamba.watchdog.data.ServiceGenerator;
import com.openshamba.watchdog.data.responses.ApiResponse;
import com.openshamba.watchdog.entities.Sms;
import com.openshamba.watchdog.entities.doas.SmsDAO;
import com.openshamba.watchdog.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static android.content.Context.WINDOW_SERVICE;
import static android.provider.Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT;
import static android.provider.Telephony.TextBasedSmsColumns.STATUS_COMPLETE;

public class MySentSmsHandler extends ContentObserver {
    private Context mContext;

    // Pop up shit
    private WindowManager windowManager;
    private RelativeLayout chatheadView;
    private WindowManager.LayoutParams params;
    private FrameLayout content;
    private Spinner spinner;
    private ImageView close,bobo;
    private TextView title;
    private Button personal,business;
    private String phone;
    private Sms sms;
    private SessionManager session;

    private final Executor executor = Executors.newFixedThreadPool(2);
    private SmsDAO smsDAO;

    public MySentSmsHandler(Context context){
        super(new Handler());
        mContext=context;
        smsDAO = DatabaseCreator.getWatchDogDatabase(context).SmsDatabase();
        session = new SessionManager(context);
    }
    public void onChange(boolean selfChange){
        Cursor cursor = mContext.getContentResolver().query(
                Uri.parse("content://sms"), null, null, null, null);
        if (cursor.moveToNext()) {
            String protocol = cursor.getString(cursor.getColumnIndex("protocol"));
            int type = cursor.getInt(cursor.getColumnIndex("type"));
            int status = cursor.getInt(cursor.getColumnIndex("status"));
            String sim = cursor.getString(cursor.getColumnIndex("sub_id"));
            String phone = cursor.getString(cursor.getColumnIndex("address"));
            this.phone = phone;
            String id = cursor.getString(cursor.getColumnIndex("_id"));
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date time = new Date(cursor.getLong(cursor.getColumnIndex("date")));
            // Only processing outgoing sms event & only when it
            // is sent successfully (available in SENT box).
            if (protocol != null || type != MESSAGE_TYPE_SENT || status != STATUS_COMPLETE || !sim.equals("1")) {
                return;
            }


            sms = new Sms(Integer.parseInt(id),phone,"","", UUID.randomUUID().toString(),sdf.format(time));

            setUpDialogWindow();

            fetchContactName(phone);

            showDialog(true);

            Toast.makeText(mContext,"SMS sent to " + phone + " from sim " + sim,Toast.LENGTH_LONG).show();

            Log.d("NIMZYMAINA","SMS sent to " + phone + " from sim " + sim);
        }
    }

    private void setUpDialogWindow(){
        windowManager = (WindowManager) mContext.getSystemService(WINDOW_SERVICE);
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 40;
        params.y = 200;

        chatheadView = (RelativeLayout) inflater.inflate(R.layout.sms_pop_up, null);
        close=(ImageView)chatheadView.findViewById(R.id.close2);
        content=(FrameLayout)chatheadView.findViewById(R.id.content2);
        bobo = (ImageView) chatheadView.findViewById(R.id.imagenotileft2);
        spinner = (Spinner) chatheadView.findViewById(R.id.spinner2);
        title = (TextView) chatheadView.findViewById(R.id.title2);
        personal = (Button) chatheadView.findViewById(R.id.personal2);
        business = (Button) chatheadView.findViewById(R.id.business2);

        ArrayAdapter adapter = ArrayAdapter.createFromResource(mContext,
                R.array.codes_arrays, R.layout.spinner_item);

        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinner.setAdapter(adapter);

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(false);
            }
        });

        personal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(mContext,"Personal SMS",Toast.LENGTH_SHORT).show();
                sms.setContact(getContactName(phone,mContext));
                sms.setType("PERSONAL");
                saveSms(sms);
                showDialog(false);

                syncSms(sms);
            }
        });

        business.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String x = spinner.getSelectedItem().toString();
                Toast.makeText(mContext,"Business client "+ x,Toast.LENGTH_SHORT).show();
                sms.setContact(getContactName(phone,mContext));
                sms.setCharge_code(x);
                sms.setType("BUSINESS");
                saveSms(sms);
                showDialog(false);

                syncSms(sms);
            }
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

    private void saveSms(Sms sms){
        executor.execute(() -> {
            smsDAO.insertSms(sms);
            Log.d("NIMZYMAINA","Sms Saved");
            Log.d("NIMZYMAINA",sms.toString());
        });
    }

    private void syncSms(Sms sms){
        Call<ApiResponse> call = ServiceGenerator.getClient(session.getKeyApiKey()).saveSms(sms);

        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if(response.isSuccessful()){
                    sms.setSynced(true);
                    executor.execute(()->{
                        smsDAO.insertSms(sms);
                        Log.d("NIMZYMAINA","Sms Synced");
                        Log.d("NIMZYMAINA",sms.toString());
                    });
                }else {
                    Log.d("NIMZYMAINA","Sms server reached but failed to sync");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.d("NIMZYMAINA","SMS server not reachable");
            }
        });
    }

}