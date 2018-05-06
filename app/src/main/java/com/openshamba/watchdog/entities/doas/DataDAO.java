package com.openshamba.watchdog.entities.doas;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.openshamba.watchdog.entities.Data;

import java.util.List;

/**
 * Created by Maina on 4/26/2018.
 */

@Dao
public interface DataDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insert(Data... item);

    @Update
    public void updateData(Data data);

    @Delete
    public void deleteData(Data data);

    @Query("SELECT * FROM data")
    public LiveData<List<Data>> getAllData();

    @Query("SELECT * FROM data ORDER BY id DESC LIMIT 1")
    public Data getData();

    @Query("SELECT * FROM data")
    public List<Data> getCount();

}
