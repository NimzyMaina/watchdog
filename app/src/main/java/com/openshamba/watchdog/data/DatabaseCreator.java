package com.openshamba.watchdog.data;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;

/**
 * Created by Maina on 3/14/2018.
 */

public class DatabaseCreator {

    private static WatchDogDatabase watchDogDatabase;
    private static final Object LOCK = new Object();

//    static final Migration MIGRATION_1_1 = new Migration(1, 2) {
//        @Override
//        public void migrate(SupportSQLiteDatabase database) {
//        }
//    };

    public synchronized static WatchDogDatabase getWatchDogDatabase(Context context){
        if(watchDogDatabase == null){
            synchronized (LOCK){
                if(watchDogDatabase == null){
                    watchDogDatabase = Room.databaseBuilder(context,WatchDogDatabase.class,"watchdog db")
                            //.addMigrations(MIGRATION_1_1)
                            .build();
                }
            }
        }
        return watchDogDatabase;
    }
}
