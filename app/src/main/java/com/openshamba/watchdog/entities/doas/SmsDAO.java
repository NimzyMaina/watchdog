package com.openshamba.watchdog.entities.doas;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.openshamba.watchdog.entities.Sms;

import java.util.List;

/**
 * Created by Maina on 3/17/2018.
 */

@Dao
public interface SmsDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public long insertSms(Sms sms);

    @Update
    public void updateSms(Sms sms);

    @Delete
    public void deleteSms(Sms sms);

    @Query("SELECT * FROM sms ORDER BY time DESC")
    public LiveData<List<Sms>> getAllSms();

    @Query("SELECT * FROM sms where synced = 0")
    public LiveData<List<Sms>> getUnsyncedSms();
}
