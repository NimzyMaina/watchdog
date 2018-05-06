package com.openshamba.watchdog.entities;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

/**
 * Created by Maina on 4/26/2018.
 */

@Entity
public class Traffic {
    @PrimaryKey(autoGenerate = true)
    public int id;
    @NonNull
    public String date;
    @NonNull
    public String time;

    public long rx;
    public long tx;
    public long total;
    public int period;
    public long rx_n;
    public long tx_n;
    public long total_n;
    @NonNull
    public String imsi;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @NonNull
    public String getDate() {
        return date;
    }

    public void setDate(@NonNull String date) {
        this.date = date;
    }

    @NonNull
    public String getTime() {
        return time;
    }

    public void setTime(@NonNull String time) {
        this.time = time;
    }

    public long getRx() {
        return rx;
    }

    public void setRx(long rx) {
        this.rx = rx;
    }

    public long getTx() {
        return tx;
    }

    public void setTx(long tx) {
        this.tx = tx;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public long getRx_n() {
        return rx_n;
    }

    public void setRx_n(long rx_n) {
        this.rx_n = rx_n;
    }

    public long getTx_n() {
        return tx_n;
    }

    public void setTx_n(long tx_n) {
        this.tx_n = tx_n;
    }

    public long getTotal_n() {
        return total_n;
    }

    public void setTotal_n(long total_n) {
        this.total_n = total_n;
    }

    @NonNull
    public String getImsi() {
        return imsi;
    }

    public void setImsi(@NonNull String imsi) {
        this.imsi = imsi;
    }

    @Override
    public String toString() {
        return "Traffic{" +
                "id=" + id +
                ", date='" + date + '\'' +
                ", time='" + time + '\'' +
                ", rx=" + rx +
                ", tx=" + tx +
                ", total=" + total +
                ", period=" + period +
                ", rx_n=" + rx_n +
                ", tx_n=" + tx_n +
                ", total_n=" + total_n +
                ", imsi='" + imsi + '\'' +
                '}';
    }
}
