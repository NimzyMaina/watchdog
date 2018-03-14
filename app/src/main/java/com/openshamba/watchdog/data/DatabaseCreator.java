package com.openshamba.watchdog.data;

import android.arch.persistence.room.Room;
import android.content.Context;

/**
 * Created by Maina on 3/14/2018.
 */

public class DatabaseCreator {

    private static WatchDogDatabase watchDogDatabase;
    private static final Object LOCK = new Object();

    public synchronized static WatchDogDatabase getWatchDogDatabase(Context context){
        if(watchDogDatabase == null){
            synchronized (LOCK){
                if(watchDogDatabase == null){
                    watchDogDatabase = Room.databaseBuilder(context,WatchDogDatabase.class,"watchdog db").build();
                }
            }
        }
        return watchDogDatabase;
    }
}
