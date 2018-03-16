package com.openshamba.watchdog.data.viewmodels;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;


import com.openshamba.watchdog.data.repositories.CallRepository;
import com.openshamba.watchdog.entities.Call;

import java.util.List;

/**
 * Created by Maina on 3/16/2018.
 */

public class CallViewModel extends AndroidViewModel {

    private LiveData<List<Call>> callsList;

    private CallRepository repository = new CallRepository(this.getApplication());

    public CallViewModel(@NonNull Application application) {
        super(application);
        callsList = repository.all();
    }

    public LiveData<List<Call>> all(){
        return callsList;
    }
}
