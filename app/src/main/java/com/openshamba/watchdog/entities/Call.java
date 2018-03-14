package com.openshamba.watchdog.entities;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import io.reactivex.annotations.NonNull;

/**
 * Created by Maina on 3/14/2018.
 */

@Entity
public class Call {
    @PrimaryKey
    private String reference;
    private String type;
    private String charge_code;
    private String phone;
    private String duration;
    private Boolean complete;
    private Boolean synced;

}
