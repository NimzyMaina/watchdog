package com.openshamba.watchdog.data.viewmodels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import com.openshamba.watchdog.data.repositories.SmsRepository;
import com.openshamba.watchdog.entities.Sms;

import java.util.List;

/**
 * Created by Maina on 3/18/2018.
 */

public class SmsViewModel extends AndroidViewModel {

    private LiveData<List<Sms>> smsList;

    private SmsRepository repository = new SmsRepository(this.getApplication());

    public SmsViewModel(@NonNull Application application) {
        super(application);
        smsList = repository.all();
    }

    public LiveData<List<Sms>> all(){
        return smsList;
    }
}
