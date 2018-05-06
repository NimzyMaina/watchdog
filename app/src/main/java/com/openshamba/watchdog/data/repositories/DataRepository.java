package com.openshamba.watchdog.data.repositories;

import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.openshamba.watchdog.data.DatabaseCreator;
import com.openshamba.watchdog.entities.Call;
import com.openshamba.watchdog.entities.Data;
import com.openshamba.watchdog.entities.doas.DataDAO;
import com.openshamba.watchdog.utils.Constants;
import com.openshamba.watchdog.utils.CustomApplication;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DataRepository {

    private List<Data> datas = new ArrayList<>();

    private DataDAO dataDAO;

    private Executor executor = Executors.newFixedThreadPool(2);

    public DataRepository(Context context) {
        dataDAO = DatabaseCreator.getWatchDogDatabase(context).DataDatabase();
        getData();
    }

    public void insert(Data... data) {
        Log.d(Constants.LOG,"Inserting into data table");
        if (CustomApplication.isActivityVisible() && CustomApplication.isScreenOn()) {
            dataDAO.insert(data);
        }else{
            executor.execute(() -> {
                dataDAO.insert(data);
            });

        }
    }

    public boolean isEmpty(){
        Log.d(Constants.LOG,"Data is empty called");
        Log.d(Constants.LOG,"Data size: "+datas.size());
        return datas.isEmpty();
    }

    private void getData(){
        new AsyncTask<Void, Void, List<Data>>() {
            @Override
            protected List<Data> doInBackground(Void... params) {
                return dataDAO.getCount();
            }
            @Override
            protected void onPostExecute(List<Data> data) {
                datas = data;
            }
        }.execute();
    }
}
