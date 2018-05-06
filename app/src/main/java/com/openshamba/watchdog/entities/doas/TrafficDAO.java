package com.openshamba.watchdog.entities.doas;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.openshamba.watchdog.entities.Traffic;

import java.util.List;

/**
 * Created by Maina on 4/26/2018.
 */

@Dao
public interface TrafficDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertTraffic(Traffic... traffic);

    @Update
    public void updateTraffic(Traffic traffic);

    @Delete
    public void deleteTraffic(Traffic traffic);

    @Query("SELECT * FROM traffic")
    public LiveData<List<Traffic>> getAllTraffic();

    @Query("SELECT * FROM traffic WHERE imsi = :imsi")
    public List<Traffic> getAllTrafficCount( String imsi);

    @Query("SELECT * FROM traffic WHERE imsi = :imsi ORDER BY id DESC LIMIT 1")
    public Traffic getTrafficByImsi(String imsi);


}
