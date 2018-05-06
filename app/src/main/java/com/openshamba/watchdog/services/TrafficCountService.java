package com.openshamba.watchdog.services;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.TrafficStats;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.openshamba.watchdog.data.DatabaseCreator;
import com.openshamba.watchdog.data.repositories.DataRepository;
import com.openshamba.watchdog.data.repositories.TrafficRepository;
import com.openshamba.watchdog.entities.Data;
import com.openshamba.watchdog.entities.Traffic;
import com.openshamba.watchdog.entities.doas.DataDAO;
import com.openshamba.watchdog.entities.doas.TrafficDAO;
import com.openshamba.watchdog.events.MobileConnectionEvent;
import com.openshamba.watchdog.events.NoConnectivityEvent;
import com.openshamba.watchdog.events.RefreshData;
import com.openshamba.watchdog.events.SetSimEvent;
import com.openshamba.watchdog.events.TipTrafficEvent;
import com.openshamba.watchdog.utils.Constants;
import com.openshamba.watchdog.utils.CustomApplication;
import com.openshamba.watchdog.utils.DataFormat;
import com.openshamba.watchdog.utils.DateUtils;
import com.openshamba.watchdog.utils.MobileUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.LocalDateTime;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TrafficCountService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static int mSimQuantity;
    private static int mActiveSIM = Constants.DISABLED;
    private static int mLastActiveSIM = Constants.DISABLED;
    private static Service mService = null;
    private static WeakReference<Context> mContext;
    private static SharedPreferences mPrefs;
    private static ArrayList<String> mIMSI = null;
    private static String[] mOperatorNames = new String[3];
    private static boolean mDoNotStopService = false;

    private static long mStartRX = 0;
    private static long mStartTX = 0;
    private static long mReceived1 = 0;
    private static long mTransmitted1 = 0;
    private static long mReceived2 = 0;
    private static long mTransmitted2 = 0;
    private static long mReceived3 = 0;
    private static long mTransmitted3 = 0;
    private static long mLastUpdateTime;


    private static ScheduledExecutorService mTaskExecutor = null;
    private static ScheduledFuture<?> mTaskResult = null;

    private static Handler mHandler;
    private static CountDownTimer mTimer = null;
    private static LocalDateTime mNowDate;

    private static Data mTrafficData;
    private static Traffic mTraffic = new Traffic();
    private final Executor executor = Executors.newFixedThreadPool(2);
    private DataDAO dataDAO = null;
    private TrafficDAO trafficDAO = null;
    private static DataRepository dataRepository = null;
    private static TrafficRepository trafficRepository = null;

    private static boolean mIsResetNeeded3, mIsResetNeeded2, mIsResetNeeded1;

    public TrafficCountService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public static int getActiveSIM() {
        return mActiveSIM;
    }

    @Override
    public final void onCreate() {
        super.onCreate();
        Log.d(Constants.LOG,"Traffic service onCreate()");

        mService = this;
        Context ctx = CustomApplication.getAppContext();
        mContext = new WeakReference<Context>(ctx);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        dataDAO = DatabaseCreator.getWatchDogDatabase(ctx).DataDatabase();
        trafficDAO = DatabaseCreator.getWatchDogDatabase(ctx).TrafficDatabase();
        dataRepository = new DataRepository(ctx);
        trafficRepository  = new TrafficRepository(ctx);

        mSimQuantity = mPrefs.getInt(Constants.PREF_OTHER[0], 1);
        Log.d(Constants.LOG,"Traffic service SIM QTY : "+mSimQuantity);

        if (mPrefs.getBoolean(Constants.PREF_OTHER[2], false)) {
            mIMSI = MobileUtils.getSimIds(ctx);
            Log.d(Constants.LOG,"mIMSI: "+mIMSI);
            CustomApplication.loadTrafficPreferences(mIMSI);
            mPrefs = null;
            mPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        }

        mPrefs.registerOnSharedPreferenceChangeListener(this);

        // cancel if already existed
        if (mTaskExecutor != null) {
            if (mTaskResult != null)
                mTaskResult.cancel(true);
            mTaskExecutor.shutdown();
            mTaskExecutor = Executors.newSingleThreadScheduledExecutor();
        } else {
            // recreate new
            mTaskExecutor = Executors.newSingleThreadScheduledExecutor();
        }
    }

    @Override
    public final int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(Constants.LOG,"Traffic service onStartCommand()");

        final Context ctx = mContext.get();
        boolean mobile = MobileUtils.isMobileDataActive(ctx);

        // register event bus
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);

        if(!mobile || mPrefs.getBoolean(Constants.PREF_OTHER[3], false)){
            mService.stopSelf();
            Log.d(Constants.LOG,"Traffic Service : Data off or count stopped");
        }else {
            Log.d(Constants.LOG,"Traffic Service is beginning");
            mHandler = new Handler();

            mTimer = new CountDownTimer(10000, 10000) {
                @Override
                public void onTick(long l) {

                }

                @Override
                public void onFinish() {
                    //stop WatchDogService
                    Log.d(Constants.LOG,"Traffic Service & Watchdog service stopping");
                    if (mPrefs.getBoolean(Constants.PREF_OTHER[4], true))
                        ctx.stopService(new Intent(ctx, WatchDogService.class));
                    mService.stopSelf();
                }
            };

            // TODO - add uid observer

            mTrafficData = new Data();
            Log.d(Constants.LOG,"Traffic Service beginning of read from DB with continue start command enabled");
            // put the values from the data table into the mTrafficData variable
            readTrafficDataFromDatabase(true);


        }

        return START_STICKY;
    }

    private static void startNewTimerTask(int task) {
        Log.d(Constants.LOG, "Tasktimer starting...");
        TimerTask tTask = null;
        if (task == Constants.COUNT) {
            Log.d(Constants.LOG,"Timer task COUNT");
            mDoNotStopService = false;
            mStartRX = TrafficStats.getMobileRxBytes();
            Log.d(Constants.LOG,"mStartRX: "+mStartRX);
            mStartTX = TrafficStats.getMobileTxBytes();
            Log.d(Constants.LOG,"mStartTX: "+mStartTX);
            String[] prefs = new String[Constants.PREF_SIM_DATA.length];

            switch (mActiveSIM) {
                case Constants.SIM1:
                    Log.d(Constants.LOG, "CountTimerTask1()");
                    tTask = new CountTimerTask1();
                    prefs = Constants.PREF_SIM1;
                    break;
                case Constants.SIM2:
                    Log.d(Constants.LOG, "CountTimerTask2()");
                    tTask = new CountTimerTask2();
                    prefs = Constants.PREF_SIM2;
                    break;
                case Constants.SIM3:
                    Log.d(Constants.LOG, "CountTimerTask3()");
                    tTask = new CountTimerTask3();
                    prefs = Constants.PREF_SIM3;
                    break;
            }

            mPrefs.edit()
                    .putInt(Constants.PREF_OTHER[5]/*last_sim*/, mActiveSIM)
                    .apply();

        }else {
            Log.d(Constants.LOG, "CheckTimerTask()");
            tTask = new CheckTimerTask();
        }

        if (mTaskResult == null || mTaskResult.isCancelled())
            mTaskExecutor = Executors.newSingleThreadScheduledExecutor();
        if (tTask != null) {
            Log.d(Constants.LOG, "Selected timer started");
            mTaskResult = mTaskExecutor.scheduleAtFixedRate(tTask, 0, Constants.NOTIFY_INTERVAL, TimeUnit.MILLISECONDS);
        }else {
            Log.d(Constants.LOG, "No task to start");
        }

    }

    private static void sendDataBroadcast(long speedRX, long speedTX) {
        int sim;
        Context ctx = mContext.get();
        if (mActiveSIM == Constants.DISABLED)
            sim = mLastActiveSIM;
        else
            sim = mActiveSIM;

        Log.d(Constants.LOG,"Send broadcast SIM: "+sim);

        if (sim >= 0) {
            Intent intent = new Intent(Constants.ACTION.TRAFFIC_BROADCAST_ACTION);
            intent.putExtra(Constants.SPEEDRX, speedRX);
            intent.putExtra(Constants.SPEEDTX, speedTX);
            intent.putExtra(Constants.SIM1RX,mTrafficData.sim1rx);
            intent.putExtra(Constants.SIM1TX,mTrafficData.sim1tx);
            intent.putExtra(Constants.TOTAL1,mTrafficData.total1);
            intent.putExtra(Constants.SIM1RX_N,mTrafficData.sim1rx_n);
            intent.putExtra(Constants.SIM1TX_N,mTrafficData.sim1tx_n);

            if(mSimQuantity >= 2){
                intent.putExtra(Constants.SIM2RX,mTrafficData.sim2rx);
                intent.putExtra(Constants.SIM2TX,mTrafficData.sim2tx);
                intent.putExtra(Constants.TOTAL2,mTrafficData.total2);
                intent.putExtra(Constants.SIM2RX_N,mTrafficData.sim2rx_n);
                intent.putExtra(Constants.SIM2TX_N,mTrafficData.sim2tx_n);
            }

            if(mSimQuantity == 3){
                intent.putExtra(Constants.SIM3RX,mTrafficData.sim3rx);
                intent.putExtra(Constants.SIM3TX,mTrafficData.sim3tx);
                intent.putExtra(Constants.TOTAL3,mTrafficData.total3);
                intent.putExtra(Constants.SIM3RX_N,mTrafficData.sim3rx_n);
                intent.putExtra(Constants.SIM3TX_N,mTrafficData.sim3tx_n);
            }

            intent.putExtra(Constants.SIM_ACTIVE, sim);
            intent.putExtra(Constants.OPERATOR1, mOperatorNames[0]);
            intent.putExtra(Constants.OPERATOR2, mOperatorNames[1]);
            intent.putExtra(Constants.OPERATOR3, mOperatorNames[2]);
            Log.d(Constants.LOG,"Data in Broadcast: "+ mTrafficData);
            ctx.sendBroadcast(intent);

            // TODO - Add hud usage statistics
        }
    }

    private void readTrafficDataFromDatabase(boolean cont) {
        Log.d(Constants.LOG, "Traffic Service readTrafficDataFromDatabase()");
        Log.d(Constants.LOG,"Traffic Service "+Constants.PREF_OTHER[2]+"= "+mPrefs.getBoolean(Constants.PREF_OTHER[2],true));
        if(mPrefs.getBoolean(Constants.PREF_OTHER[2], true)){
            Log.d(Constants.LOG,"Traffic Service read for all sims");
            if (mIMSI == null)
                mIMSI = MobileUtils.getSimIds(mContext.get());

                readSimTraffic(0,mIMSI.get(0),cont);
            Log.d(Constants.LOG,"Traffic sim 1 read");

            if (mSimQuantity >= 2) {
                readSimTraffic(1, mIMSI.get(1),cont);
                Log.d(Constants.LOG,"Traffic sim 2 read");
            }
            if (mSimQuantity == 3) {
                readSimTraffic(2, mIMSI.get(2),cont);
                Log.d(Constants.LOG,"Traffic sim 3 read");
            }
        }else{
            Log.d(Constants.LOG,"Traffic Service readDB()");
            readDB(cont);
        }
    }

    public void readDB(boolean cont){
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
                    if(cont){
                        continueStartCommand();
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

    public void continueStartCommand(){
        Log.d(Constants.LOG,"Start command continued!!");
        final Context ctx = mContext.get();
        boolean mobile = MobileUtils.isMobileDataActive(ctx);

        Log.d(Constants.LOG, "Traffic service read traffic");

        //CustomApplication.sleep(3000);
        if (mTrafficData.date.equals("")) {
            LocalDateTime dateTime = DateTime.now().toLocalDateTime();
            mTrafficData.date = dateTime.toString(Constants.DATE_FORMATTER);
            mTrafficData.time = dateTime.toString(Constants.TIME_FORMATTER);
        }

        mActiveSIM = Constants.DISABLED;
        mLastActiveSIM = mPrefs.getInt(Constants.PREF_OTHER[5], Constants.DISABLED);

        // get operator names for the sim cards
        mOperatorNames = new String[]{MobileUtils.getName(ctx, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1),
                MobileUtils.getName(ctx, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2),
                MobileUtils.getName(ctx, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3)};
        Log.d(Constants.LOG, "Operator names: "+mOperatorNames);

        // send out broadcast intent & data to floating window service
        sendDataBroadcast(0L, 0L);
        Log.d(Constants.LOG, "Send Broadcast");

        // TODO: Build notification at this point


        if (mobile) {
            mIsResetNeeded1 = mPrefs.getBoolean(Constants.PREF_SIM1[25], false);
            Log.d(Constants.LOG,"SIM 1 need reset: "+mIsResetNeeded1);

            mIsResetNeeded2 = mPrefs.getBoolean(Constants.PREF_SIM2[25], false);
            Log.d(Constants.LOG,"SIM 2 need reset: "+mIsResetNeeded2);

            mIsResetNeeded3 = mPrefs.getBoolean(Constants.PREF_SIM3[25], false);
            Log.d(Constants.LOG,"SIM 3 need reset: "+mIsResetNeeded3);

            if (mSimQuantity == 1) {
                mActiveSIM = Constants.SIM1;
                startNewTimerTask(Constants.COUNT);
            } else {
                mActiveSIM = MobileUtils.getActiveSimForData(ctx);
                if (mActiveSIM != Constants.DISABLED)
                    startNewTimerTask(Constants.COUNT);
            }
            Log.d(Constants.LOG, "Traffic Service startNewTimerTask(Constants.COUNT)");

        }

        mPrefs.edit()
                .putBoolean(Constants.PREF_OTHER[6]/*traffic_running*/, true)
                .apply();
    }

    private void readSimTraffic(int sim, String imsi,boolean cont) {
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

                if(cont && (sim+1) == mSimQuantity){
                    Log.d(Constants.LOG,"Traffic Service continueStartCommand()");
                    continueStartCommand();
                }

                Log.d(Constants.LOG,"State of data: "+mTrafficData);

            }
        }.execute();
    }

    private static class CheckTimerTask extends TimerTask{

        @Override
        public void run() {
            Log.d(Constants.LOG, "Started Checktimer");
            final Context ctx = mContext.get();
            EventBus.getDefault().post(new TipTrafficEvent());
            LocalDateTime dt = Constants.DATE_FORMATTER.parseLocalDateTime((String) mTrafficData.date);

            long tot1 = mTrafficData.total1;
            long tot2 = mTrafficData.total2;
            long tot3 = mTrafficData.total3;

            try{

                if(mPrefs.getBoolean(Constants.PREF_SIM1[31]/*autoenable1*/, false) && // if autoenabled is true for sim 1
                    (
                        DateUtils.isNextDayOrMonth(dt, mPrefs.getString(Constants.PREF_SIM1[3]/*period1*/, ""))|| /* is next day*/
                            (
                                mPrefs.getBoolean(Constants.PREF_SIM1[8], false) || // sim is preferred
                                    ( // all sims are not prefered
                                        !mPrefs.getBoolean(Constants.PREF_SIM1[8]/*prefer1*/, false) &&
                                        !mPrefs.getBoolean(Constants.PREF_SIM2[8], false) &&
                                        !mPrefs.getBoolean(Constants.PREF_SIM3[8], false)
                                    )
                            )
                    )
                 ){
//                    MobileUtils.toggleMobileDataConnection(true, ctx, Constants.SIM1);
                    if (mTaskResult != null) {
                        mTaskResult.cancel(false);
                        mTaskExecutor.shutdown();
                    }
                    startNewTimerTask(Constants.COUNT);
                }

                if(mPrefs.getBoolean(Constants.PREF_SIM2[31]/*autoenable1*/, false) && // if autoenabled is true for sim 2
                        (
                                DateUtils.isNextDayOrMonth(dt, mPrefs.getString(Constants.PREF_SIM2[3]/*period1*/, ""))|| /* is next day*/
                                        (
                                                mPrefs.getBoolean(Constants.PREF_SIM2[8], false) || // sim is preferred
                                                        ( // all sims are not prefered
                                                                !mPrefs.getBoolean(Constants.PREF_SIM1[8]/*prefer1*/, false) &&
                                                                        !mPrefs.getBoolean(Constants.PREF_SIM2[8], false) &&
                                                                        !mPrefs.getBoolean(Constants.PREF_SIM3[8], false)
                                                        )
                                        )
                        )
                        ){
//                    MobileUtils.toggleMobileDataConnection(true, ctx, Constants.SIM1);
                    if (mTaskResult != null) {
                        mTaskResult.cancel(false);
                        mTaskExecutor.shutdown();
                    }
                    startNewTimerTask(Constants.COUNT);
                }

                if(mPrefs.getBoolean(Constants.PREF_SIM3[31]/*autoenable1*/, false) && // if autoenabled is true for sim 1
                        (
                                DateUtils.isNextDayOrMonth(dt, mPrefs.getString(Constants.PREF_SIM3[3]/*period1*/, ""))|| /* is next day*/
                                        (
                                                mPrefs.getBoolean(Constants.PREF_SIM3[8], false) || // sim is preferred
                                                        ( // all sims are not prefered
                                                                !mPrefs.getBoolean(Constants.PREF_SIM1[8]/*prefer1*/, false) &&
                                                                        !mPrefs.getBoolean(Constants.PREF_SIM2[8], false) &&
                                                                        !mPrefs.getBoolean(Constants.PREF_SIM3[8], false)
                                                        )
                                        )
                        )
                        ){
//                    MobileUtils.toggleMobileDataConnection(true, ctx, Constants.SIM1);
                    if (mTaskResult != null) {
                        mTaskResult.cancel(false);
                        mTaskExecutor.shutdown();
                    }
                    startNewTimerTask(Constants.COUNT);
                }

            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }

    private static class CountTimerTask1 extends TimerTask {

        @Override
        public void run() {
            try {

                if (MobileUtils.isMobileDataActive(mContext.get()) && (mTaskResult != null && !mTaskResult.isCancelled())) {

                    Log.d(Constants.LOG, "Starting Tasktimer 1 try");
                    long speedRX;
                    long speedTX;

//                    EventBus.getDefault().post(new RefreshData());
//                    Log.d(Constants.LOG, "Refresh Data Event sent");
//                    Log.d(Constants.LOG,"Last date: "+mTrafficData.date);

                    long timeDelta = SystemClock.elapsedRealtime() - mLastUpdateTime;
                    if (timeDelta < 1) {
                        // Can't div by 0 so make sure the value displayed is minimal
                        timeDelta = Long.MAX_VALUE;
                    }
                    mLastUpdateTime = SystemClock.elapsedRealtime();

                    long rx = 0;
                    long tx = 0;
                    long tot = 0;

                    if (mTrafficData.date.equals("")) {
                        LocalDateTime dateTime = DateTime.now().toLocalDateTime();
                        mTrafficData.date = dateTime.toString(Constants.DATE_FORMATTER);
                        mTrafficData.time = dateTime.toString(Constants.TIME_FORMATTER);
                    }

                    Log.d(Constants.LOG,"Data @ checkIfResetNeeded() : "+mTrafficData);

                    LocalDateTime lastDate = Constants.DATE_FORMATTER.parseLocalDateTime(mTrafficData.date);
                    mNowDate = DateTime.now().toLocalDateTime();
                    if (mNowDate.getDayOfYear() != lastDate.getDayOfYear())
                        checkIfResetNeeded(false);

                    Log.d(Constants.LOG,"mNowDate.getDayOfYear() --> "+mNowDate.getDayOfYear());
                    Log.d(Constants.LOG,"lastDate.getDayOfYear() --> "+lastDate.getDayOfYear());

                    boolean emptyDB = (mPrefs.getBoolean(Constants.PREF_OTHER[2], true) && mIMSI != null)?
                            trafficRepository.isEmpty() :
                            dataRepository.isEmpty();

                    Log.d(Constants.LOG,"Traffic empty: "+trafficRepository.isEmpty());
                    Log.d(Constants.LOG,"Data empty: "+dataRepository.isEmpty());

                    if(emptyDB){
                        Log.d(Constants.LOG,"DB is empty");
                        if(!mTrafficData.date.equals("")) {
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

                            mTrafficData.lastrx = 0L;
                            mTrafficData.lasttx = 0L;

                            mTrafficData.date = "";
                            mTrafficData.time = "";
                        }

                        Log.d(Constants.LOG,"Calling insert empty");
                        writeTrafficDataToDatabase(Constants.DISABLED);
                    }
                    else if(mIsResetNeeded1 || mIsResetNeeded2 || mIsResetNeeded3) {
                        Log.d(Constants.LOG,"DB needs reset");
                        mTrafficData.time =  mNowDate.toString(Constants.TIME_FORMATTER);
                        mTrafficData.date = mNowDate.toString(Constants.DATE_FORMATTER);

                        if(mIsResetNeeded1) {
                            mTraffic.rx = 0L;
                            mTraffic.tx = 0L;
                            mTraffic.total = 0L;
                            mTraffic.rx_n = 0L;
                            mTraffic.tx_n = 0L;
                            mTraffic.total_n = 0L;

                            rx = tx = mReceived1 = mTransmitted1 = 0;
                            mIsResetNeeded1 = false;

                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM1[25], mIsResetNeeded1)
                                    .putString(Constants.PREF_SIM1[24], mNowDate.toString(Constants.DATE_TIME_FORMATTER))
                                    .apply();

                            Log.d(Constants.LOG,"Calling insert reset sim 1");
                            writeTrafficDataToDatabase(Constants.SIM1);
                        }

                        if(mIsResetNeeded2) {
                            mTrafficData.sim2rx = 0L;
                            mTrafficData.sim2tx = 0L;
                            mTrafficData.total2 = 0L;
                            mTrafficData.sim2rx_n = 0L;
                            mTrafficData.sim2tx_n = 0L;
                            mTrafficData.total2_n = 0L;

                            rx = mTrafficData.sim1rx;
                            tx = mTrafficData.sim1tx;
                            tot = mTrafficData.total1;

                            mReceived2 = mTransmitted2 = 0;
                            mIsResetNeeded2 = false;

                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM2[25], mIsResetNeeded2)
                                    .putString(Constants.PREF_SIM2[24], mNowDate.toString(Constants.DATE_TIME_FORMATTER))
                                    .apply();

                            Log.d(Constants.LOG,"Calling insert reset sim 2");
                            writeTrafficDataToDatabase(Constants.SIM2);
                        }

                        if(mIsResetNeeded3) {
                            mTrafficData.sim3rx = 0L;
                            mTrafficData.sim3tx = 0L;
                            mTrafficData.total3 = 0L;
                            mTrafficData.sim3rx_n = 0L;
                            mTrafficData.sim3tx_n = 0L;
                            mTrafficData.total3_n = 0L;

                            rx = mTrafficData.sim1rx;
                            tx = mTrafficData.sim1tx;
                            tot = mTrafficData.total1;

                            mReceived3 = mTransmitted3 = 0;
                            mIsResetNeeded3 = false;

                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM3[25], mIsResetNeeded3)
                                    .putString(Constants.PREF_SIM3[24], mNowDate.toString(Constants.DATE_TIME_FORMATTER))
                                    .apply();

                            Log.d(Constants.LOG,"Calling insert reset sim 3");
                            writeTrafficDataToDatabase(Constants.SIM3);
                        }
                    }else {
                        rx = mTrafficData.sim1rx;
                        tx = mTrafficData.sim1tx;
                        tot = mTrafficData.total1;
                    }

                    long uidRx = 0;
                    long uidTx = 0;

                    long diffrx = TrafficStats.getMobileRxBytes() - mStartRX - uidRx;
                    Log.d(Constants.LOG,"diffrx: "+diffrx);
                    long difftx = TrafficStats.getMobileTxBytes() - mStartTX - uidTx;
                    Log.d(Constants.LOG,"difftx: "+difftx);

                    if (diffrx < 0)
                        diffrx = 0;
                    if (difftx < 0)
                        difftx = 0;

                    speedRX = (long) (diffrx / (timeDelta / 1000F));

                    speedTX = (long) (difftx / (timeDelta / 1000F));

                    mStartRX = TrafficStats.getMobileRxBytes();
                    mStartTX = TrafficStats.getMobileTxBytes();

                    rx += diffrx;
                    tx += difftx;
                    tot = tx + rx;

                    mTrafficData.sim1rx = rx;
                    mTrafficData.sim1tx = tx;
                    mTrafficData.total1 = tot;

                    postDataChanges(mActiveSIM, diffrx, difftx, emptyDB);

                    // TODO -  send broadcast if UI is visible

                    if (CustomApplication.isActivityVisible() && CustomApplication.isScreenOn()) {
                        Log.d(Constants.LOG,"Sending broadcast from timer");
                        sendDataBroadcast(speedRX, speedTX);
                    }


                }else{
                    Log.d(Constants.LOG,"Data counter 1 is off");
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private static class CountTimerTask2 extends TimerTask {

        @Override
        public void run() {
            try {

                if (MobileUtils.isMobileDataActive(mContext.get()) && (mTaskResult != null && !mTaskResult.isCancelled())) {

                    Log.d(Constants.LOG, "Starting Tasktimer 2 try");
                    long speedRX;
                    long speedTX;

//                    EventBus.getDefault().post(new RefreshData());
//                    Log.d(Constants.LOG, "Refresh Data Event sent");
//                    Log.d(Constants.LOG,"Last date: "+mTrafficData.date);

                    long timeDelta = SystemClock.elapsedRealtime() - mLastUpdateTime;
                    if (timeDelta < 1) {
                        // Can't div by 0 so make sure the value displayed is minimal
                        timeDelta = Long.MAX_VALUE;
                    }
                    mLastUpdateTime = SystemClock.elapsedRealtime();

                    long rx = 0;
                    long tx = 0;
                    long tot = 0;

                    if (mTrafficData.date.equals("")) {
                        LocalDateTime dateTime = DateTime.now().toLocalDateTime();
                        mTrafficData.date = dateTime.toString(Constants.DATE_FORMATTER);
                        mTrafficData.time = dateTime.toString(Constants.TIME_FORMATTER);
                    }

                    Log.d(Constants.LOG,"Data @ checkIfResetNeeded() : "+mTrafficData);

                    LocalDateTime lastDate = Constants.DATE_FORMATTER.parseLocalDateTime(mTrafficData.date);
                    mNowDate = DateTime.now().toLocalDateTime();
                    if (mNowDate.getDayOfYear() != lastDate.getDayOfYear())
                        checkIfResetNeeded(false);

                    Log.d(Constants.LOG,"mNowDate.getDayOfYear() --> "+mNowDate.getDayOfYear());
                    Log.d(Constants.LOG,"lastDate.getDayOfYear() --> "+lastDate.getDayOfYear());

                    boolean emptyDB = (mPrefs.getBoolean(Constants.PREF_OTHER[2], true) && mIMSI != null)?
                            trafficRepository.isEmpty2() :
                            dataRepository.isEmpty();

                    Log.d(Constants.LOG,"Traffic empty: "+trafficRepository.isEmpty2());
                    Log.d(Constants.LOG,"Data empty: "+dataRepository.isEmpty());

                    if(emptyDB){
                        Log.d(Constants.LOG,"DB is empty");
                        if(!mTrafficData.date.equals("")) {
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

                            mTrafficData.lastrx = 0L;
                            mTrafficData.lasttx = 0L;

                            mTrafficData.date = "";
                            mTrafficData.time = "";
                        }

                        Log.d(Constants.LOG,"Calling insert empty");
                        writeTrafficDataToDatabase(Constants.DISABLED);
                    }
                    else if(mIsResetNeeded1 || mIsResetNeeded2 || mIsResetNeeded3) {
                        Log.d(Constants.LOG,"DB needs reset");
                        mTrafficData.time =  mNowDate.toString(Constants.TIME_FORMATTER);
                        mTrafficData.date = mNowDate.toString(Constants.DATE_FORMATTER);

                        if(mIsResetNeeded1) {
                            mTraffic.rx = 0L;
                            mTraffic.tx = 0L;
                            mTraffic.total = 0L;
                            mTraffic.rx_n = 0L;
                            mTraffic.tx_n = 0L;
                            mTraffic.total_n = 0L;

                            rx = mTrafficData.sim2rx;
                            tx = mTrafficData.sim2tx;
                            tot = mTrafficData.total2;

                            mReceived1 = mTransmitted1 = 0;
                            mIsResetNeeded1 = false;

                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM1[25], mIsResetNeeded1)
                                    .putString(Constants.PREF_SIM1[24], mNowDate.toString(Constants.DATE_TIME_FORMATTER))
                                    .apply();

                            Log.d(Constants.LOG,"Calling insert reset sim 1");
                            writeTrafficDataToDatabase(Constants.SIM1);
                        }

                        if(mIsResetNeeded2) {
                            mTrafficData.sim2rx = 0L;
                            mTrafficData.sim2tx = 0L;
                            mTrafficData.total2 = 0L;
                            mTrafficData.sim2rx_n = 0L;
                            mTrafficData.sim2tx_n = 0L;
                            mTrafficData.total2_n = 0L;

                            rx = tx = mReceived2 = mTransmitted2 = 0;
                            mIsResetNeeded2 = false;

                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM2[25], mIsResetNeeded2)
                                    .putString(Constants.PREF_SIM2[24], mNowDate.toString(Constants.DATE_TIME_FORMATTER))
                                    .apply();

                            Log.d(Constants.LOG,"Calling insert reset sim 2");
                            writeTrafficDataToDatabase(Constants.SIM2);
                        }

                        if(mIsResetNeeded3) {
                            mTrafficData.sim3rx = 0L;
                            mTrafficData.sim3tx = 0L;
                            mTrafficData.total3 = 0L;
                            mTrafficData.sim3rx_n = 0L;
                            mTrafficData.sim3tx_n = 0L;
                            mTrafficData.total3_n = 0L;

                            rx = mTrafficData.sim2rx;
                            tx = mTrafficData.sim2tx;
                            tot = mTrafficData.total2;

                            mReceived3 = mTransmitted3 = 0;
                            mIsResetNeeded3 = false;

                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM3[25], mIsResetNeeded3)
                                    .putString(Constants.PREF_SIM3[24], mNowDate.toString(Constants.DATE_TIME_FORMATTER))
                                    .apply();

                            Log.d(Constants.LOG,"Calling insert reset sim 3");
                            writeTrafficDataToDatabase(Constants.SIM3);
                        }
                    }else {
                        rx = mTrafficData.sim2rx;
                        tx = mTrafficData.sim2tx;
                        tot = mTrafficData.total2;
                    }

                    long uidRx = 0;
                    long uidTx = 0;

                    long diffrx = TrafficStats.getMobileRxBytes() - mStartRX - uidRx;
                    Log.d(Constants.LOG,"diffrx: "+diffrx);
                    long difftx = TrafficStats.getMobileTxBytes() - mStartTX - uidTx;
                    Log.d(Constants.LOG,"difftx: "+difftx);

                    if (diffrx < 0)
                        diffrx = 0;
                    if (difftx < 0)
                        difftx = 0;

                    speedRX = (long) (diffrx / (timeDelta / 1000F));

                    speedTX = (long) (difftx / (timeDelta / 1000F));

                    mStartRX = TrafficStats.getMobileRxBytes();
                    mStartTX = TrafficStats.getMobileTxBytes();

                    rx += diffrx;
                    tx += difftx;
                    tot = tx + rx;

                    mTrafficData.sim2rx = rx;
                    mTrafficData.sim2tx = tx;
                    mTrafficData.total2 = tot;

                    postDataChanges(mActiveSIM, diffrx, difftx, emptyDB);

                    // TODO -  send broadcast if UI is visible

                    if (CustomApplication.isActivityVisible() && CustomApplication.isScreenOn()) {
                        Log.d(Constants.LOG,"Sending broadcast from timer");
                        sendDataBroadcast(speedRX, speedTX);
                    }


                }else{
                    Log.d(Constants.LOG,"Data counter 2 is off");
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private static class CountTimerTask4 extends TimerTask {

        @Override
        public void run() {
            try {

                Log.d(Constants.LOG, "Starting Tasktimer 2 try");
                if (MobileUtils.isMobileDataActive(mContext.get()) && (mTaskResult != null && !mTaskResult.isCancelled())) {

                    long speedRX;
                    long speedTX;

                    EventBus.getDefault().post(new RefreshData());

                    long timeDelta = SystemClock.elapsedRealtime() - mLastUpdateTime;
                    if (timeDelta < 1) {
                        // Can't div by 0 so make sure the value displayed is minimal
                        timeDelta = Long.MAX_VALUE;
                    }
                    mLastUpdateTime = SystemClock.elapsedRealtime();

                    long rx = 0;
                    long tx = 0;
                    long tot = 0;

                    if (mTrafficData.date.equals("")) {
                        LocalDateTime dateTime = DateTime.now().toLocalDateTime();
                        mTrafficData.date = dateTime.toString(Constants.DATE_FORMATTER);
                        mTrafficData.time = dateTime.toString(Constants.TIME_FORMATTER);
                    }

                    LocalDateTime lastDate = Constants.DATE_FORMATTER.parseLocalDateTime((String) mTrafficData.date);
                    mNowDate = DateTime.now().toLocalDateTime();
                    if (mNowDate.getDayOfYear() != lastDate.getDayOfYear())
                        checkIfResetNeeded(false);

                    boolean emptyDB = (mPrefs.getBoolean(Constants.PREF_OTHER[2], false) && mIMSI != null)?
                            trafficRepository.isEmpty() :
                            dataRepository.isEmpty();

                    if(emptyDB){
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

                        mTrafficData.lastrx = 0L;
                        mTrafficData.lasttx = 0L;

                        mTrafficData.date = "";
                        mTrafficData.time = "";

                        writeTrafficDataToDatabase(Constants.DISABLED);
                    }
                    else if(mIsResetNeeded1 || mIsResetNeeded2 || mIsResetNeeded3) {
                        mTrafficData.time =  mNowDate.toString(Constants.TIME_FORMATTER);
                        mTrafficData.date = mNowDate.toString(Constants.DATE_FORMATTER);

                        if(mIsResetNeeded1) {
                            mTraffic.rx = 0L;
                            mTraffic.tx = 0L;
                            mTraffic.total = 0L;
                            mTraffic.rx_n = 0L;
                            mTraffic.tx_n = 0L;
                            mTraffic.total_n = 0L;

                            rx = tx = mReceived1 = mTransmitted1 = 0;
                            mIsResetNeeded1 = false;

                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM1[25], mIsResetNeeded1)
                                    .putString(Constants.PREF_SIM1[24], mNowDate.toString(Constants.DATE_TIME_FORMATTER))
                                    .apply();

                            writeTrafficDataToDatabase(Constants.SIM1);
                        }

                        if(mIsResetNeeded2) {
                            rx = mTraffic.rx = 0L;
                            tx = mTraffic.tx = 0L;
                            tot = mTraffic.total = 0L;
                            mTraffic.rx_n = 0L;
                            mTraffic.tx_n = 0L;
                            mTraffic.total_n = 0L;

                            mReceived2 = mTransmitted2 = 0;
                            mIsResetNeeded2 = false;

                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM2[25], mIsResetNeeded2)
                                    .putString(Constants.PREF_SIM2[24], mNowDate.toString(Constants.DATE_TIME_FORMATTER))
                                    .apply();

                            writeTrafficDataToDatabase(Constants.SIM2);
                        }

                        if(mIsResetNeeded3) {
                            rx = mTraffic.rx = 0L;
                            tx = mTraffic.tx = 0L;
                            tot = mTraffic.total = 0L;
                            mTraffic.rx_n = 0L;
                            mTraffic.tx_n = 0L;
                            mTraffic.total_n = 0L;

                            mReceived3 = mTransmitted3 = 0;
                            mIsResetNeeded3 = false;

                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM3[25], mIsResetNeeded3)
                                    .putString(Constants.PREF_SIM3[24], mNowDate.toString(Constants.DATE_TIME_FORMATTER))
                                    .apply();

                            writeTrafficDataToDatabase(Constants.SIM3);
                        }
                    }

                    long uidRx = 0;
                    long uidTx = 0;

                    long diffrx = TrafficStats.getMobileRxBytes() - mStartRX - uidRx;
                    long difftx = TrafficStats.getMobileTxBytes() - mStartTX - uidTx;

                    if (diffrx < 0)
                        diffrx = 0;
                    if (difftx < 0)
                        difftx = 0;

                    speedRX = (long) (diffrx / (timeDelta / 1000F));
                    speedTX = (long) (difftx / (timeDelta / 1000F));

                    mStartRX = TrafficStats.getMobileRxBytes();
                    mStartTX = TrafficStats.getMobileTxBytes();

                    rx += diffrx;
                    tx += difftx;
                    tot = tx + rx;

                    mTrafficData.sim2rx = rx;
                    mTrafficData.sim2tx = tx;
                    mTrafficData.total2 = tot;

                    postDataChanges(mActiveSIM, diffrx, difftx, emptyDB);

                    // TODO -  send broadcast if UI is visible


                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private static class CountTimerTask3 extends TimerTask {

        @Override
        public void run() {
            try {

                Log.d(Constants.LOG, "Starting Tasktimer 3 try");

                if (MobileUtils.isMobileDataActive(mContext.get()) && (mTaskResult != null && !mTaskResult.isCancelled())) {

                    long speedRX;
                    long speedTX;

                    EventBus.getDefault().post(new RefreshData());

                    long timeDelta = SystemClock.elapsedRealtime() - mLastUpdateTime;
                    if (timeDelta < 1) {
                        // Can't div by 0 so make sure the value displayed is minimal
                        timeDelta = Long.MAX_VALUE;
                    }
                    mLastUpdateTime = SystemClock.elapsedRealtime();

                    long rx = 0;
                    long tx = 0;
                    long tot = 0;

                    if (mTrafficData.date.equals("")) {
                        LocalDateTime dateTime = DateTime.now().toLocalDateTime();
                        mTrafficData.date = dateTime.toString(Constants.DATE_FORMATTER);
                        mTrafficData.time = dateTime.toString(Constants.TIME_FORMATTER);
                    }

                    LocalDateTime lastDate = Constants.DATE_FORMATTER.parseLocalDateTime((String) mTrafficData.date);
                    mNowDate = DateTime.now().toLocalDateTime();
                    if (mNowDate.getDayOfYear() != lastDate.getDayOfYear())
                        checkIfResetNeeded(false);

                    boolean emptyDB = (mPrefs.getBoolean(Constants.PREF_OTHER[2], false) && mIMSI != null)?
                            trafficRepository.isEmpty() :
                            dataRepository.isEmpty();

                    if(emptyDB){
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

                        mTrafficData.lastrx = 0L;
                        mTrafficData.lasttx = 0L;

                        mTrafficData.date = "";
                        mTrafficData.time = "";

                        writeTrafficDataToDatabase(Constants.DISABLED);
                    }
                    else if(mIsResetNeeded1 || mIsResetNeeded2 || mIsResetNeeded3) {
                        mTrafficData.time =  mNowDate.toString(Constants.TIME_FORMATTER);
                        mTrafficData.date = mNowDate.toString(Constants.DATE_FORMATTER);

                        if(mIsResetNeeded1) {
                            mTraffic.rx = 0L;
                            mTraffic.tx = 0L;
                            mTraffic.total = 0L;
                            mTraffic.rx_n = 0L;
                            mTraffic.tx_n = 0L;
                            mTraffic.total_n = 0L;

                            rx = tx = mReceived1 = mTransmitted1 = 0;
                            mIsResetNeeded1 = false;

                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM1[25], mIsResetNeeded1)
                                    .putString(Constants.PREF_SIM1[24], mNowDate.toString(Constants.DATE_TIME_FORMATTER))
                                    .apply();

                            writeTrafficDataToDatabase(Constants.SIM1);
                        }

                        if(mIsResetNeeded2) {
                            rx = mTraffic.rx = 0L;
                            tx = mTraffic.tx = 0L;
                            tot = mTraffic.total = 0L;
                            mTraffic.rx_n = 0L;
                            mTraffic.tx_n = 0L;
                            mTraffic.total_n = 0L;

                            mReceived2 = mTransmitted2 = 0;
                            mIsResetNeeded2 = false;

                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM2[25], mIsResetNeeded2)
                                    .putString(Constants.PREF_SIM2[24], mNowDate.toString(Constants.DATE_TIME_FORMATTER))
                                    .apply();

                            writeTrafficDataToDatabase(Constants.SIM2);
                        }

                        if(mIsResetNeeded3) {
                            rx = mTraffic.rx = 0L;
                            tx = mTraffic.tx = 0L;
                            tot = mTraffic.total = 0L;
                            mTraffic.rx_n = 0L;
                            mTraffic.tx_n = 0L;
                            mTraffic.total_n = 0L;

                            mReceived3 = mTransmitted3 = 0;
                            mIsResetNeeded3 = false;

                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM3[25], mIsResetNeeded3)
                                    .putString(Constants.PREF_SIM3[24], mNowDate.toString(Constants.DATE_TIME_FORMATTER))
                                    .apply();

                            writeTrafficDataToDatabase(Constants.SIM3);
                        }
                    }

                    long uidRx = 0;
                    long uidTx = 0;

                    long diffrx = TrafficStats.getMobileRxBytes() - mStartRX - uidRx;
                    long difftx = TrafficStats.getMobileTxBytes() - mStartTX - uidTx;

                    if (diffrx < 0)
                        diffrx = 0;
                    if (difftx < 0)
                        difftx = 0;

                    speedRX = (long) (diffrx / (timeDelta / 1000F));
                    speedTX = (long) (difftx / (timeDelta / 1000F));

                    mStartRX = TrafficStats.getMobileRxBytes();
                    mStartTX = TrafficStats.getMobileTxBytes();

                    rx += diffrx;
                    tx += difftx;
                    tot = tx + rx;

                    mTrafficData.sim3rx = rx;
                    mTrafficData.sim3tx = tx;
                    mTrafficData.total3 = tot;

                    postDataChanges(mActiveSIM, diffrx, difftx, emptyDB);

                    // TODO -  send broadcast if UI is visible


                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private static void checkIfResetNeeded(boolean settingsChanged) {
        String[] simPref = new String[]{Constants.PREF_SIM1[3], Constants.PREF_SIM1[9], Constants.PREF_SIM1[10]};
        LocalDateTime resetTime1 = Constants.DATE_TIME_FORMATTER.parseLocalDateTime(mPrefs.getString(Constants.PREF_SIM1[26], "1970-01-01 00:00"));
        if (mNowDate == null)
            mNowDate = DateTime.now().toLocalDateTime();
        if (mNowDate.compareTo(resetTime1) >= 0 || settingsChanged) {
            resetTime1 = DateUtils.setResetDate(mPrefs, simPref);
            if (resetTime1 != null) {
                mPrefs.edit()
                        .putString(Constants.PREF_SIM1[26], resetTime1.toString(Constants.DATE_TIME_FORMATTER))
                        .apply();
                if (!settingsChanged) {
                    mIsResetNeeded1 = true;
                    mPrefs.edit()
                            .putBoolean(Constants.PREF_SIM1[25], mIsResetNeeded1)
                            .apply();
                }
            }
        }
        if (mSimQuantity >= 2) {
            simPref = new String[]{Constants.PREF_SIM2[3], Constants.PREF_SIM2[9], Constants.PREF_SIM2[10]};
            LocalDateTime resetTime2 = Constants.DATE_TIME_FORMATTER.parseLocalDateTime(mPrefs.getString(Constants.PREF_SIM2[26], "1970-01-01 00:00"));
            if (mNowDate.compareTo(resetTime2) >= 0 || settingsChanged) {
                resetTime2 = DateUtils.setResetDate(mPrefs, simPref);
                if (resetTime2 != null) {
                    mPrefs.edit()
                            .putString(Constants.PREF_SIM2[26], resetTime2.toString(Constants.DATE_TIME_FORMATTER))
                            .apply();
                    if (!settingsChanged) {
                        mIsResetNeeded2 = true;
                        mPrefs.edit()
                                .putBoolean(Constants.PREF_SIM2[25], mIsResetNeeded2)
                                .apply();
                    }
                }
            }
        }
        if (mSimQuantity == 3) {
            simPref = new String[]{Constants.PREF_SIM3[3], Constants.PREF_SIM3[9], Constants.PREF_SIM3[10]};
            LocalDateTime resetTime3 = Constants.DATE_TIME_FORMATTER.parseLocalDateTime(mPrefs.getString(Constants.PREF_SIM3[26], "1970-01-01 00:00"));
            if (mNowDate.compareTo(resetTime3) >= 0 || settingsChanged) {
                resetTime3 = DateUtils.setResetDate(mPrefs, simPref);
                if (resetTime3 != null) {
                    mPrefs.edit()
                            .putString(Constants.PREF_SIM3[26], resetTime3.toString(Constants.DATE_TIME_FORMATTER))
                            .apply();
                    if (!settingsChanged) {
                        mIsResetNeeded3 = true;
                        mPrefs.edit()
                                .putBoolean(Constants.PREF_SIM3[25], mIsResetNeeded3)
                                .apply();
                    }
                }
            }
        }
    }

    private static void writeTrafficDataToDatabase(int sim) {
        if(mPrefs.getBoolean(Constants.PREF_OTHER[2],true)) {
            if (mIMSI == null)
                mIMSI = MobileUtils.getSimIds(mContext.get());

            switch (sim){
                case Constants.SIM1:
                    putDataForSim(Constants.SIM1);
                    Log.d(Constants.LOG,"Insert 1: "+mTraffic);
                    trafficRepository.insert(mTraffic);
                    break;
                case Constants.SIM2:
                    putDataForSim(Constants.SIM2);
                    Log.d(Constants.LOG,"Insert 2: "+mTraffic);
                    trafficRepository.insert(mTraffic);
                    break;
                case Constants.SIM3:
                    putDataForSim(Constants.SIM3);
                    Log.d(Constants.LOG,"Insert 3: "+mTraffic);
                    trafficRepository.insert(mTraffic);
                    break;
                case Constants.DISABLED:
                    for (int i = 0; i < mIMSI.size(); i++) {
                        putDataForSim(i);
                        trafficRepository.insert(mTraffic);
                    }
            }
        }else{
            dataRepository.insert(mTrafficData);
        }
    }

    private static void putDataForSim( int sim) {
        switch (sim) {
            case Constants.SIM1:
                mTraffic.imsi = mIMSI.get(Constants.SIM1);
                mTraffic.rx = mTrafficData.sim1rx;
                mTraffic.tx = mTrafficData.sim1tx;
                mTraffic.total = mTrafficData.total1;
                mTraffic.rx_n = mTrafficData.sim1rx_n;
                mTraffic.tx_n = mTrafficData.sim1tx_n;
                mTraffic.total_n = mTrafficData.total1_n;
                mTraffic.date = mTrafficData.date;
                mTraffic.time = mTrafficData.time;
                break;
            case Constants.SIM2:
                mTraffic.imsi = mIMSI.get(Constants.SIM2);
                mTraffic.rx = mTrafficData.sim2rx;
                mTraffic.tx = mTrafficData.sim2tx;
                mTraffic.total = mTrafficData.total2;
                mTraffic.rx_n = mTrafficData.sim2rx_n;
                mTraffic.tx_n = mTrafficData.sim2tx_n;
                mTraffic.total_n = mTrafficData.total2_n;
                mTraffic.date = mTrafficData.date;
                mTraffic.time = mTrafficData.time;
                break;
            case Constants.SIM3:
                mTraffic.imsi = mIMSI.get(Constants.SIM3);
                mTraffic.rx = mTrafficData.sim3rx;
                mTraffic.tx = mTrafficData.sim3tx;
                mTraffic.total = mTrafficData.total3;
                mTraffic.rx_n = mTrafficData.sim3rx_n;
                mTraffic.tx_n = mTrafficData.sim3tx_n;
                mTraffic.total_n = mTrafficData.total3_n;
                mTraffic.date = mTrafficData.date;
                mTraffic.time = mTrafficData.time;
                break;
        }
    }

    private static void postDataChanges(int sim, long diffrx, long difftx, boolean emptyDB) {
        mTrafficData.lastrx = TrafficStats.getMobileRxBytes();
        mTrafficData.lasttx = TrafficStats.getMobileTxBytes();
        LocalDateTime dateTime = DateTime.now().toLocalDateTime();
        final long MB = 1024 * 1024;

        if ((diffrx + difftx > MB) || dateTime.get(DateTimeFieldType.secondOfMinute()) == 59 || emptyDB) {
            mTrafficData.date = dateTime.toString(Constants.DATE_FORMATTER);
            mTrafficData.time = dateTime.toString(Constants.TIME_FORMATTER);
            writeTrafficDataToDatabase(sim);

            // TODO - update notification
        }
    }

    @Override
    public final void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        final Context ctx = mContext.get();
        if (key.equals(Constants.PREF_SIM_DATA[33]) || key.equals(Constants.PREF_SIM1[33]) ||
                key.equals(Constants.PREF_SIM2[33]) || key.equals(Constants.PREF_SIM3[33])) {
            if (mTaskResult != null) {
                mTaskResult.cancel(false);
                mTaskExecutor.shutdown();
            }
            startNewTimerTask(Constants.COUNT);
        }

        if (key.equals(Constants.PREF_OTHER[2])) {
            writeTrafficDataToDatabase(mActiveSIM);
            readTrafficDataFromDatabase(false);
        }

        if (key.equals(Constants.PREF_SIM1[3]) || key.equals(Constants.PREF_SIM1[9]) || key.equals(Constants.PREF_SIM1[10]) ||
                key.equals(Constants.PREF_SIM2[3]) || key.equals(Constants.PREF_SIM2[9]) || key.equals(Constants.PREF_SIM2[10]) ||
                key.equals(Constants.PREF_SIM3[3]) || key.equals(Constants.PREF_SIM3[9]) || key.equals(Constants.PREF_SIM3[10])) {
            checkIfResetNeeded(true);
        }

        if (key.equals(Constants.PREF_SIM1[5]) || key.equals(Constants.PREF_SIM1[6]))
            mOperatorNames[0] = MobileUtils.getName(ctx, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1);
        if (key.equals(Constants.PREF_SIM2[5]) || key.equals(Constants.PREF_SIM2[6]))
            mOperatorNames[1] = MobileUtils.getName(ctx, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2);
        if (key.equals(Constants.PREF_SIM3[5]) || key.equals(Constants.PREF_SIM3[6]))
            mOperatorNames[2] = MobileUtils.getName(ctx, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3);

        if (sharedPreferences.getBoolean(Constants.PREF_OTHER[2], false)) {
            int sim = Constants.DISABLED;
            if (new ArrayList<>(Arrays.asList(Constants.PREF_SIM1)).contains(key))
                sim = Constants.SIM1;
            else if (new ArrayList<>(Arrays.asList(Constants.PREF_SIM2)).contains(key))
                sim = Constants.SIM2;
            else if (new ArrayList<>(Arrays.asList(Constants.PREF_SIM3)).contains(key))
                sim = Constants.SIM3;
            if (sim >= 0 && sim < mIMSI.size()) {
                Map prefs = sharedPreferences.getAll();
                Object o = prefs.get(key);
                key = key.substring(0, key.length() - 1);
                SharedPreferences.Editor editor = getSharedPreferences(Constants.TRAFFIC + "_" + mIMSI.get(sim), Context.MODE_PRIVATE).edit();
                boolean operator = key.equals(Constants.PREF_SIM_DATA[5]) ||
                        key.equals(Constants.PREF_SIM_DATA[6]);
                if (!operator)
                    CustomApplication.putObject(editor, key, o);
                editor.apply();
            }
        }
    }

    @Subscribe
    public final void OnMessageEvent(RefreshData event) {
        Log.d(Constants.LOG, "Refreshing data");
        readTrafficDataFromDatabase(false);
    }

    @Subscribe
    public final void onMessageEvent(MobileConnectionEvent event) {
        Context ctx = mContext.get();
        if (mTaskResult != null) {
            mTaskResult.cancel(false);
            mTaskExecutor.shutdown();
        }
        if (mTimer != null)
            mTimer.cancel();
        if (mSimQuantity == 1) {
            mActiveSIM = Constants.SIM1;
            startNewTimerTask(Constants.COUNT);
        }else{
            mActiveSIM = MobileUtils.getActiveSimForData(ctx);
            if (mActiveSIM != Constants.DISABLED)
                startNewTimerTask(Constants.COUNT);
        }
    }

    @Subscribe
    public final void onMessageEvent(SetSimEvent event) {
        if (event.action != null) {
            if (mTaskResult != null) {
                mTaskResult.cancel(false);
                mTaskExecutor.shutdown();
            }
            mActiveSIM = event.sim;
            startNewTimerTask(Constants.COUNT);
        }
    }

    @Subscribe
    public final void onMessageEvent(NoConnectivityEvent event) {
        Log.d(Constants.LOG,"NO Connectivity event ()");
        if (mTaskResult != null) {
            mTaskResult.cancel(false);
            mTaskExecutor.shutdown();
        }

        mLastActiveSIM = mActiveSIM;

        Log.d(Constants.LOG,"Send Dbroadcast");
        sendDataBroadcast(0L, 0L);

        if (mPrefs.getBoolean(Constants.PREF_SIM1[14], true) && mLastActiveSIM == Constants.SIM1)
            mTrafficData.total1 = DataFormat.getRoundLong( mTrafficData.total1,
                    mPrefs.getString(Constants.PREF_SIM1[15], "1"), mPrefs.getString(Constants.PREF_SIM1[16], "0"));

        if (mPrefs.getBoolean(Constants.PREF_SIM2[14], true) && mLastActiveSIM == Constants.SIM2)
            mTrafficData.total2 =  DataFormat.getRoundLong( mTrafficData.total2,
                    mPrefs.getString(Constants.PREF_SIM2[15], "1"), mPrefs.getString(Constants.PREF_SIM2[16], "0"));

        if (mPrefs.getBoolean(Constants.PREF_SIM3[14], true) && mLastActiveSIM == Constants.SIM3) {
            mTrafficData.total3 = DataFormat.getRoundLong(mTrafficData.total3,
                    mPrefs.getString(Constants.PREF_SIM3[15], "1"), mPrefs.getString(Constants.PREF_SIM3[16], "0"));
        }

        writeTrafficDataToDatabase(mLastActiveSIM);

        if (!mDoNotStopService ) {
            Log.d(Constants.LOG,"MTimer start");
            mTimer.start();
        }

    }

    @Override
    public final void onDestroy() {

        Log.d(Constants.LOG,"Traffic Counter on destroy");

        mPrefs.edit()
            .putBoolean(Constants.PREF_OTHER[6], false)
            .apply();

        if (mActiveSIM != -1)
                mPrefs.edit()
                    .putInt(Constants.PREF_OTHER[5], mActiveSIM)
                    .apply();

        if (mTaskResult != null) {
            mTaskResult.cancel(false);
            mTaskExecutor.shutdown();
        }

        if (mTrafficData != null)
            writeTrafficDataToDatabase(mActiveSIM);

        mPrefs.unregisterOnSharedPreferenceChangeListener(this);

        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
    }

}
