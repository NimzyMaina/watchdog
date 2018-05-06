package com.openshamba.watchdog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.openshamba.watchdog.adapters.Group;
import com.openshamba.watchdog.data.DatabaseCreator;
import com.openshamba.watchdog.data.repositories.DataRepository;
import com.openshamba.watchdog.data.repositories.TrafficRepository;
import com.openshamba.watchdog.entities.Data;
import com.openshamba.watchdog.entities.Traffic;
import com.openshamba.watchdog.entities.doas.DataDAO;
import com.openshamba.watchdog.entities.doas.TrafficDAO;
import com.openshamba.watchdog.utils.Constants;
import com.openshamba.watchdog.utils.CustomApplication;
import com.openshamba.watchdog.utils.DataFormat;
import com.openshamba.watchdog.utils.MobileUtils;
import com.openshamba.watchdog.utils.Tools;

import org.greenrobot.eventbus.EventBus;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;

import java.util.ArrayList;

public class TrafficActivity extends BaseActivity {

    private Toolbar toolbar;
    private ActionBar actionBar;
    private View parent_view;
    private Group group;
    public static String KEY_GROUP = "com.openshamba.watchdog.GROUP";

    private TextView SIM, TOT1, TOT2, TOT3, TX1, TX2, TX3, RX1, RX2, RX3, TIP, SIM1, SIM2, SIM3;
    private BroadcastReceiver mTrafficDataReceiver;
    private AppCompatButton bSet;
    private SharedPreferences mPrefs;
    private String[] mOperatorNames = new String[3];
    private int mSimQuantity;
    private boolean mIsRunning = false;
    private Context mContext;
    private ArrayList<String> mIMSI = null;
    private Data mTrafficData;
    private static Traffic mTraffic = new Traffic();

    private DataDAO dataDAO = null;
    private TrafficDAO trafficDAO = null;
    private static DataRepository dataRepository = null;
    private static TrafficRepository trafficRepository = null;

    public static void navigate(AppCompatActivity activity, View transitionImage, Group obj) {
        Intent intent = new Intent(activity,TrafficActivity.class);
        intent.putExtra(KEY_GROUP,obj);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, transitionImage, KEY_GROUP);
        ActivityCompat.startActivity(activity,intent,options.toBundle());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_traffic);
        parent_view = findViewById(android.R.id.content);
        checkAuth();

        if (mContext == null)
            mContext = getApplicationContext();

        ViewCompat.setTransitionName(parent_view, KEY_GROUP);

        Intent intent = getIntent();
        group = (Group) intent.getExtras().getSerializable(KEY_GROUP);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mSimQuantity = mPrefs.getInt(Constants.PREF_OTHER[0], 1);

        dataDAO = DatabaseCreator.getWatchDogDatabase(mContext).DataDatabase();
        trafficDAO = DatabaseCreator.getWatchDogDatabase(mContext).TrafficDatabase();
        dataRepository = new DataRepository(mContext);
        trafficRepository  = new TrafficRepository(mContext);

        mTrafficData = new Data();

        //readTrafficDataFromDatabase();

        initComponents();
        prepareActionBar(toolbar);

        mOperatorNames = new String[]{MobileUtils.getName(mContext, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1),
                MobileUtils.getName(mContext, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2),
                MobileUtils.getName(mContext, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3)};


        if (mPrefs.getBoolean(Constants.PREF_OTHER[2], false)) {
            mIMSI = MobileUtils.getSimIds(mContext);
        }


        mTrafficDataReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {

                try{
                    if(RX1 != null)
                        RX1.setText(DataFormat.formatData(context,intent.getLongExtra(Constants.SIM1RX,0L)));

                    if(TX1 != null)
                        TX1.setText(DataFormat.formatData(context,intent.getLongExtra(Constants.SIM1TX,0L)));

                    TOT1.setText(DataFormat.formatData(context,intent.getLongExtra(Constants.TOTAL1,0L)));

                    if (intent.hasExtra(Constants.OPERATOR1) || !intent.getStringExtra(Constants.OPERATOR1).equals("")) {
                        mOperatorNames[0] = intent.getStringExtra(Constants.OPERATOR1);
                        SIM1.setText(mOperatorNames[0]);
                    }else{
                        SIM1.setText(mOperatorNames[0]);
                    }

                    if (mSimQuantity >= 2) {

                        if(RX2 != null)
                            RX2.setText(DataFormat.formatData(context,intent.getLongExtra(Constants.SIM2RX,0L)));

                        if(TX2 != null)
                            TX2.setText(DataFormat.formatData(context,intent.getLongExtra(Constants.SIM2TX,0L)));

                        TOT2.setText(DataFormat.formatData(context,intent.getLongExtra(Constants.TOTAL2,0L)));

                        if (intent.hasExtra(Constants.OPERATOR2) || !intent.getStringExtra(Constants.OPERATOR2).equals("")) {
                            mOperatorNames[1] = intent.getStringExtra(Constants.OPERATOR2);
                            SIM2.setText(mOperatorNames[1]);
                        }else{
                            SIM2.setText(mOperatorNames[1]);
                        }

                    }

                    if (mSimQuantity == 3) {

                        if(RX3 != null)
                            RX3.setText(DataFormat.formatData(context,intent.getLongExtra(Constants.SIM3RX,0L)));

                        if(TX3 != null)
                            TX3.setText(DataFormat.formatData(context,intent.getLongExtra(Constants.SIM3TX,0L)));

                        TOT3.setText(DataFormat.formatData(context,intent.getLongExtra(Constants.TOTAL3,0L)));

                        if (intent.hasExtra(Constants.OPERATOR3) || !intent.getStringExtra(Constants.OPERATOR3).equals("")) {
                            mOperatorNames[2] = intent.getStringExtra(Constants.OPERATOR3);
                            SIM3.setText(mOperatorNames[2]);
                        }else{
                            SIM3.setText(mOperatorNames[2]);
                        }

                    }

                    String rxSpeed = DataFormat.formatData(context, intent.getLongExtra(Constants.SPEEDRX, 0L));
                    String txSpeed = DataFormat.formatData(context, intent.getLongExtra(Constants.SPEEDTX, 0L));
                    setLabelText(intent.getIntExtra(Constants.SIM_ACTIVE, 0), txSpeed, rxSpeed);

                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        };

        IntentFilter countServiceFilter = new IntentFilter(Constants.ACTION.TRAFFIC_BROADCAST_ACTION);
        mContext.registerReceiver(mTrafficDataReceiver, countServiceFilter);

        // for system bar in lollipop
        Tools.systemBarLolipop(this);

    }

    private void initComponents(){
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        RX1 = findViewById(R.id.RX1);
        TX1 = findViewById(R.id.TX1);
        RX2 = findViewById(R.id.RX2);
        TX2 = findViewById(R.id.TX2);
        RX3 = findViewById(R.id.RX3);
        TX3 = findViewById(R.id.TX3);

        TOT1 = findViewById(R.id.Tot1);
        TOT2 = findViewById(R.id.Tot2);
        TOT3 = findViewById(R.id.Tot3);
        SIM = findViewById(R.id.sim);
        SIM1 = findViewById(R.id.sim1_name);
        SIM2 = findViewById(R.id.sim2_name);
        SIM3 = findViewById(R.id.sim3_name);

        bSet = findViewById(R.id.settings);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {

            if (TX2 != null)
                TX2.setVisibility(View.GONE);
            if (RX2 != null)
                RX2.setVisibility(View.GONE);
            if (TX3 != null)
                TX3.setVisibility(View.GONE);
            if (RX3 != null)
                RX3.setVisibility(View.GONE);

            SIM2.setVisibility(View.GONE);
            TOT2.setVisibility(View.GONE);

            SIM3.setVisibility(View.GONE);
            TOT3.setVisibility(View.GONE);

        } else {
//            TODO: add landscape view
//            findViewById(R.id.sim2row).setVisibility(View.GONE);
//            findViewById(R.id.sim3row).setVisibility(View.GONE);
        }

        if(mSimQuantity >= 2) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {

                if (TX2 != null)
                    TX2.setVisibility(View.VISIBLE);
                if (RX2 != null)
                    RX2.setVisibility(View.VISIBLE);

                SIM2.setVisibility(View.VISIBLE);
                TOT2.setVisibility(View.VISIBLE);
            }
        }

        if (mSimQuantity == 3) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {

                if (TX3 != null)
                    TX3.setVisibility(View.VISIBLE);
                if (RX3 != null)
                    RX3.setVisibility(View.VISIBLE);

                SIM3.setVisibility(View.VISIBLE);
                TOT3.setVisibility(View.VISIBLE);
            }
        }
    }

    private void prepareActionBar(Toolbar toolbar){
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setSubtitle(group.getName());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setLabelText(int sim, String rx, String tx) {
        int swtch = MobileUtils.hasActiveNetworkInfo(mContext);
        switch (sim) {
            case Constants.DISABLED:
                if (swtch== 0)
                    SIM.setText(R.string.data_dis);
                else if (swtch == 1)
                    SIM.setText(R.string.other_network);
                else if (swtch == 2)
                    SIM.setText(R.string.not_supported);
                break;
            case Constants.SIM1:
                if (swtch == 2)
                    SIM.setText(String.format(getString(R.string.sim1_act), tx, rx));
                else if (swtch == 1)
                    SIM.setText(R.string.other_network);
                if (swtch == 0)
                    SIM.setText(R.string.data_dis);
                break;
            case Constants.SIM2:
                if (swtch == 2)
                    SIM.setText(String.format(getString(R.string.sim2_act), tx, rx));
                else if (swtch == 1)
                    SIM.setText(R.string.other_network);
                if (swtch == 0)
                    SIM.setText(R.string.data_dis);
                break;
            case Constants.SIM3:
                if (swtch == 2)
                    SIM.setText(String.format(getString(R.string.sim3_act), tx, rx));
                else if (swtch == 1)
                    SIM.setText(R.string.other_network);
                if (swtch == 0)
                    SIM.setText(R.string.data_dis);
                break;
        }
    }


    @Override
    public final void onResume() {
        super.onResume();
        readTrafficDataFromDatabase();

        if (RX1 != null) {
            RX1.setText(DataFormat.formatData(mContext, mTrafficData.sim1rx));
        }
        if (TX1 != null) {
            TX1.setText(DataFormat.formatData(mContext,mTrafficData.sim1tx));
        }
        if (mSimQuantity >= 2) {
            if (RX2 != null) {
                RX2.setText(DataFormat.formatData(mContext, mTrafficData.sim2rx));
            }
            if (TX2 != null) {
                TX2.setText(DataFormat.formatData(mContext, mTrafficData.sim2tx));
            }
        }
        if (mSimQuantity == 3) {
            if (RX3 != null) {
                RX3.setText(DataFormat.formatData(mContext,mTrafficData.sim3rx));
            }
            if (TX3 != null) {
                TX3.setText(DataFormat.formatData(mContext,mTrafficData.sim3tx));
            }
        }

        TOT1.setText(DataFormat.formatData(mContext,mTrafficData.total1));
        SIM1.setText(mOperatorNames[0]);

        if (mSimQuantity >= 2) {
            TOT2.setText(DataFormat.formatData(mContext, mTrafficData.total3));
            SIM2.setText(mOperatorNames[1]);
        }
        if (mSimQuantity == 3) {
            TOT3.setText(DataFormat.formatData(mContext,mTrafficData.total3));
            SIM3.setText(mOperatorNames[2]);
        }

        CustomApplication.resumeActivity();
    }

    @Override
    public final void onPause() {
        super.onPause();
        CustomApplication.pauseActivity();
    }

    @Override
    public final void onDestroy() {
        super.onDestroy();
//        if (EventBus.getDefault().isRegistered(this))
//            EventBus.getDefault().unregister(this);

        if(mTrafficDataReceiver != null)
            getApplicationContext().unregisterReceiver(mTrafficDataReceiver);
    }

    private void readTrafficDataFromDatabase() {
        Log.d(Constants.LOG, "Traffic Service readTrafficDataFromDatabase()");
        Log.d(Constants.LOG,"Traffic Service "+Constants.PREF_OTHER[2]+"= "+mPrefs.getBoolean(Constants.PREF_OTHER[2],true));
        if(mPrefs.getBoolean(Constants.PREF_OTHER[2], true)){
            Log.d(Constants.LOG,"Traffic Service read for all sims");
            if (mIMSI == null)
                mIMSI = MobileUtils.getSimIds(getApplicationContext());

            readSimTraffic(0,mIMSI.get(0));
            Log.d(Constants.LOG,"Traffic sim 1 read");

            if (mSimQuantity >= 2) {
                readSimTraffic(1, mIMSI.get(1));
                Log.d(Constants.LOG,"Traffic sim 2 read");
            }
            if (mSimQuantity == 3) {
                readSimTraffic(2, mIMSI.get(2));
                Log.d(Constants.LOG,"Traffic sim 3 read");
            }
        }else{
            Log.d(Constants.LOG,"Traffic Service readDB()");
            readDB();
        }
    }

    public void readDB(){
        new AsyncTask<Void, Void, Data>() {
            @Override
            protected Data doInBackground(Void... params) {
                Log.d(Constants.LOG,"Reading traffic from data table");
                return dataDAO.getData();
            }
            @Override
            protected void onPostExecute(Data data) {
                if (data != null) {
                    Log.d(Constants.LOG,"Traffic read from database");
                    mTrafficData = data;
                    if (mTrafficData.date.equals("")){
                        LocalDateTime dateTime = DateTime.now().toLocalDateTime();
                        mTrafficData.date = dateTime.toString(Constants.TIME_FORMATTER);
                        mTrafficData.time = dateTime.toString(Constants.TIME_FORMATTER);
                    }

                }else{
                    mTrafficData.sim1rx = 0L;
                    mTrafficData.sim1tx = 0L;
                    mTrafficData.total1 = 0L;
                    mTrafficData.sim1rx_n = 0L;
                    mTrafficData.sim1tx_n = 0L;
                    mTrafficData.period1 = 0;

                    mTrafficData.sim2rx = 0L;
                    mTrafficData.sim2tx = 0L;
                    mTrafficData.total2 = 0L;
                    mTrafficData.sim2rx_n = 0L;
                    mTrafficData.sim2tx_n = 0L;

                    mTrafficData.sim3rx = 0L;
                    mTrafficData.sim3tx = 0L;
                    mTrafficData.total3 = 0L;
                    mTrafficData.sim3rx_n = 0L;
                    mTrafficData.sim3tx_n = 0L;

                    mTrafficData.date = "";
                    mTrafficData.time = "";

                    Log.e(Constants.LOG,"Traffic from data table is null");
                }
            }
        }.execute();
    }

    private void readSimTraffic(int sim, String imsi) {
        new AsyncTask<Void, Void, Traffic>() {
            @Override
            protected Traffic doInBackground(Void... params) {
                Log.d(Constants.LOG,"Reading traffic from traffic table -> sim: "+ sim + " --> imsi: "+ imsi);
                return trafficDAO.getTrafficByImsi(imsi);
            }
            @Override
            protected void onPostExecute(Traffic traffic) {
                if (traffic != null) {
                    mTraffic = traffic;
                    Log.d(Constants.LOG,"Read Sim Result --> "+traffic);
                    mTraffic.imsi = imsi;
                    switch (sim) {
                        case 0: {
                            Log.d(Constants.LOG,"SIM 1 set results");
                            mTrafficData.sim1rx = traffic.rx;
                            mTrafficData.sim1tx = traffic.tx;
                            mTrafficData.total1 = traffic.total;
                            mTrafficData.sim1rx_n = traffic.rx_n;
                            mTrafficData.sim1tx_n = traffic.tx_n;
                            mTrafficData.period1 = traffic.period;

                            mTrafficData.sim2rx = 0L;
                            mTrafficData.sim2tx = 0L;
                            mTrafficData.total2 = 0L;
                            mTrafficData.sim2rx_n = 0L;
                            mTrafficData.sim2tx_n = 0L;

                            mTrafficData.sim3rx = 0L;
                            mTrafficData.sim3tx = 0L;
                            mTrafficData.total3 = 0L;
                            mTrafficData.sim3rx_n = 0L;
                            mTrafficData.sim3tx_n = 0L;

                            mTrafficData.date = traffic.date;
                            mTrafficData.time = traffic.time;
                            break;
                        }
                        case 1: {
                            Log.d(Constants.LOG,"SIM 2 set results");
                            mTrafficData.sim2rx = traffic.rx;
                            mTrafficData.sim2tx = traffic.tx;
                            mTrafficData.total2 = traffic.total;
                            mTrafficData.sim2rx_n = traffic.rx_n;
                            mTrafficData.sim2tx_n = traffic.tx_n;
                            mTrafficData.period2 = traffic.period;

                            mTrafficData.date = traffic.date;
                            mTrafficData.time = traffic.time;
                            break;
                        }
                        case 2: {
                            Log.d(Constants.LOG,"SIM 3 set results");
                            mTrafficData.sim3rx = traffic.rx;
                            mTrafficData.sim3tx = traffic.tx;
                            mTrafficData.total3 = traffic.total;
                            mTrafficData.sim3rx_n = traffic.rx_n;
                            mTrafficData.sim3tx_n = traffic.tx_n;
                            mTrafficData.period3 = traffic.period;

                            mTrafficData.date = traffic.date;
                            mTrafficData.time = traffic.time;
                            break;
                        }
                    }
                }else{

                    switch (sim) {
                        case 0: {
                            Log.d(Constants.LOG,"SIM 1 set zero results");
                            mTrafficData.sim1rx = 0l;
                            mTrafficData.sim1tx = 0L;
                            mTrafficData.total1 = 0L;
                            mTrafficData.sim1rx_n = 0L;
                            mTrafficData.sim1tx_n = 0L;
                            mTrafficData.period1 = 0;
                            break;
                        }
                        case 1: {
                            Log.d(Constants.LOG,"SIM 2 set zero results");
                            mTrafficData.sim2rx = 0L;
                            mTrafficData.sim2tx = 0L;
                            mTrafficData.total2 = 0L;
                            mTrafficData.sim2rx_n = 0L;
                            mTrafficData.sim2tx_n = 0L;
                            mTrafficData.period2 = 0;
                            break;
                        }
                        case 2: {
                            Log.d(Constants.LOG,"SIM 3 set zero results");
                            mTrafficData.sim3rx = 0L;
                            mTrafficData.sim3tx = 0L;
                            mTrafficData.total3 = 0L;
                            mTrafficData.sim3rx_n = 0L;
                            mTrafficData.sim3tx_n = 0L;
                            mTrafficData.period3 = 0;
                            break;
                        }
                    }

                    if(mTrafficData.date == null){
                        mTrafficData.date = "";
                    }

                    if(mTrafficData.time == null){
                        mTrafficData.time = "";
                    }

                    Log.e(Constants.LOG,"Traffic sim "+sim+ " is null : imsi => " + imsi);
                }
                Log.d(Constants.LOG,"State of data: "+mTrafficData);

            }
        }.execute();
    }
}
