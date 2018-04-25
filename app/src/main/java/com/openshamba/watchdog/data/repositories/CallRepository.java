package com.openshamba.watchdog.data.repositories;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.os.AsyncTask;

import com.openshamba.watchdog.data.DatabaseCreator;
import com.openshamba.watchdog.entities.Call;
import com.openshamba.watchdog.entities.doas.CallDAO;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by Maina on 3/16/2018.
 */

public class CallRepository {

    private final LiveData<List<Call>> callsList;
    private CallDAO callDAO;
    private Executor executor = Executors.newFixedThreadPool(2);

    public CallRepository(Context context) {
        callDAO = DatabaseCreator.getWatchDogDatabase(context).CallDatabase();
        callsList = callDAO.getAllCalls();
    }

    public LiveData<List<Call>> all(){
        return callsList;
    }
}
