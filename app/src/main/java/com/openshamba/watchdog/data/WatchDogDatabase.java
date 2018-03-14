package com.openshamba.watchdog.data;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import com.openshamba.watchdog.entities.Call;
import com.openshamba.watchdog.entities.doas.CallDAO;

/**
 * Created by Maina on 3/14/2018.
 */

@Database(entities = {Call.class},version = 1)
public abstract class WatchDogDatabase extends RoomDatabase {
    public abstract CallDAO CallDatabase();
}
