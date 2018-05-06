package com.openshamba.watchdog.data.repositories;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.openshamba.watchdog.data.DatabaseCreator;
import com.openshamba.watchdog.entities.Traffic;
import com.openshamba.watchdog.entities.doas.TrafficDAO;
import com.openshamba.watchdog.utils.Constants;
import com.openshamba.watchdog.utils.CustomApplication;
import com.openshamba.watchdog.utils.MobileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TrafficRepository {

    private TrafficDAO trafficDAO;
    private List<Traffic> traffics = new ArrayList<>();
    private List<Traffic> traffics2 = new ArrayList<>();
    private List<Traffic> traffics3 = new ArrayList<>();
    private Context mContext;

    private Executor executor = Executors.newFixedThreadPool(2);

    public TrafficRepository(Context context) {
        mContext = context;
        trafficDAO = DatabaseCreator.getWatchDogDatabase(context).TrafficDatabase();

        int sim = MobileUtils.isMultiSim(context);

        getEmptyState();

        if(sim >= 2)
            getEmptyState2();

        if(sim == 3)
            getEmptyState3();
    }

    public void insert(Traffic traffic) {
        if (CustomApplication.isActivityVisible() && CustomApplication.isScreenOn()) {
            Log.d(Constants.LOG,"Inserting into traffic table on UI");
            trafficDAO.insertTraffic(traffic);
        }else {
            executor.execute(() -> {
                Log.d(Constants.LOG,"Inserting into traffic table in executor");
                trafficDAO.insertTraffic(traffic);
            });
        }
    }

    public boolean isEmpty() {
        getEmptyState();
        Log.d(Constants.LOG,"Traffic is empty 1 called");
        Log.d(Constants.LOG,"Traffic 2 size: "+traffics.size());
        return  traffics.isEmpty();
    }

    public boolean isEmpty2() {
        getEmptyState2();
        Log.d(Constants.LOG,"Traffic is empty 2 called");
        Log.d(Constants.LOG,"Traffic 2 size: "+traffics2.size());
        return  traffics2.isEmpty();
    }

    public boolean isEmpty3() {
        getEmptyState3();
        Log.d(Constants.LOG,"Traffic is empty 3 called");
        Log.d(Constants.LOG,"Traffic 3 size: "+traffics3.size());
        return  traffics3.isEmpty();
    }

    private void getEmptyState() {
        new AsyncTask<Void, Void, List<Traffic>>() {
            @Override
            protected List<Traffic> doInBackground(Void... params) {
                return trafficDAO.getAllTrafficCount(MobileUtils.getSimIds(mContext).get(Constants.SIM1));
            }
            @Override
            protected void onPostExecute(List<Traffic> data) {
                traffics = data;
                Log.d(Constants.LOG,"getEmptyState()1 --> "+data);
            }
        }.execute();
    }

    private void getEmptyState2() {
        new AsyncTask<Void, Void, List<Traffic>>() {
            @Override
            protected List<Traffic> doInBackground(Void... params) {
                return trafficDAO.getAllTrafficCount(MobileUtils.getSimIds(mContext).get(Constants.SIM2));
            }
            @Override
            protected void onPostExecute(List<Traffic> data) {
                traffics2 = data;
                Log.d(Constants.LOG,"getEmptyState()2 --> "+data);
            }
        }.execute();
    }

    private void getEmptyState3() {
        new AsyncTask<Void, Void, List<Traffic>>() {
            @Override
            protected List<Traffic> doInBackground(Void... params) {
                return trafficDAO.getAllTrafficCount(MobileUtils.getSimIds(mContext).get(Constants.SIM3));
            }
            @Override
            protected void onPostExecute(List<Traffic> data) {
                traffics3 = data;
                Log.d(Constants.LOG,"getEmptyState()3 --> "+data);
            }
        }.execute();
    }

}
