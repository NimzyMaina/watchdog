package com.openshamba.watchdog.data;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import com.openshamba.watchdog.entities.Call;
import com.openshamba.watchdog.entities.Data;
import com.openshamba.watchdog.entities.Sms;
import com.openshamba.watchdog.entities.Traffic;
import com.openshamba.watchdog.entities.doas.CallDAO;
import com.openshamba.watchdog.entities.doas.DataDAO;
import com.openshamba.watchdog.entities.doas.SmsDAO;
import com.openshamba.watchdog.entities.doas.TrafficDAO;

/**
 * Created by Maina on 3/14/2018.
 */

@Database(entities = {Call.class, Sms.class, Data.class, Traffic.class},version = 1,exportSchema = false)
public abstract class WatchDogDatabase extends RoomDatabase {
    public abstract CallDAO CallDatabase();
    public abstract SmsDAO SmsDatabase();
    public abstract DataDAO DataDatabase();
    public abstract TrafficDAO TrafficDatabase();
}
