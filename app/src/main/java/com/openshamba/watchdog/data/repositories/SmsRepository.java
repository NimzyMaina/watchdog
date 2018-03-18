package com.openshamba.watchdog.data.repositories;

import android.arch.lifecycle.LiveData;
import android.content.Context;

import com.openshamba.watchdog.data.DatabaseCreator;
import com.openshamba.watchdog.entities.Sms;
import com.openshamba.watchdog.entities.doas.SmsDAO;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by Maina on 3/18/2018.
 */

public class SmsRepository {

    private LiveData<List<Sms>> smsList;
    private Executor executor = Executors.newFixedThreadPool(2);
    private SmsDAO smsDAO;

    public SmsRepository(Context context) {
        smsDAO = DatabaseCreator.getWatchDogDatabase(context).SmsDatabase();
        smsList = smsDAO.getAllSms();
    }

    public LiveData<List<Sms>> all(){
        return smsList;
    }
}
