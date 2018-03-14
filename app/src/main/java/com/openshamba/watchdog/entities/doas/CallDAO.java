package com.openshamba.watchdog.entities.doas;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.openshamba.watchdog.entities.Call;

import java.util.List;

/**
 * Created by Maina on 3/14/2018.
 */

@Dao
public interface CallDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public long insertCall(Call call);

    @Update
    public void updateCall(Call call);

    @Delete
    public void deleteCall(Call call);

    @Query("SELECT * FROM call")
    public LiveData<List<Call>> getAllCalls();

    @Query("SELECT * FROM call where phone = :mobileIn and complete = 0")
    public LiveData<Call> getLastCall(String mobileIn);

    @Query("SELECT * FROM call where synced = 0 and complete = 1")
    public LiveData<List<Call>> getUnsyncedCalls();
}
