package com.openshamba.watchdog.entities;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

/**
 * Created by Maina on 4/26/2018.
 */

@Entity
public class Data {
    @PrimaryKey(autoGenerate = true)
    public int id;
    @NonNull
    public String date;
    @NonNull
    public String time;
    public int last_sim;
    public long lastrx;
    public long lasttx;

    public long sim1rx;
    public long sim1tx;
    public long total1;

    public long sim2rx;
    public long sim2tx;
    public long total2;

    public long sim3rx;
    public long sim3tx;
    public long total3;

    public int period1;
    public int period2;
    public int period3;

    public long sim1rx_n;
    public long sim1tx_n;
    public long total1_n;

    public long sim2rx_n;
    public long sim2tx_n;
    public long total2_n;

    public long sim3rx_n;
    public long sim3tx_n;
    public long total3_n;

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

    public int getLast_sim() {
        return last_sim;
    }

    public void setLast_sim(int last_sim) {
        this.last_sim = last_sim;
    }

    public long getLastrx() {
        return lastrx;
    }

    public void setLastrx(long lastrx) {
        this.lastrx = lastrx;
    }

    public long getLasttx() {
        return lasttx;
    }

    public void setLasttx(long lasttx) {
        this.lasttx = lasttx;
    }

    public long getSim1rx() {
        return sim1rx;
    }

    public void setSim1rx(long sim1rx) {
        this.sim1rx = sim1rx;
    }

    public long getSim1tx() {
        return sim1tx;
    }

    public void setSim1tx(long sim1tx) {
        this.sim1tx = sim1tx;
    }

    public long getTotal1() {
        return total1;
    }

    public void setTotal1(long total1) {
        this.total1 = total1;
    }

    public long getSim2rx() {
        return sim2rx;
    }

    public void setSim2rx(long sim2rx) {
        this.sim2rx = sim2rx;
    }

    public long getSim2tx() {
        return sim2tx;
    }

    public void setSim2tx(long sim2tx) {
        this.sim2tx = sim2tx;
    }

    public long getTotal2() {
        return total2;
    }

    public void setTotal2(long total2) {
        this.total2 = total2;
    }

    public long getSim3rx() {
        return sim3rx;
    }

    public void setSim3rx(long sim3rx) {
        this.sim3rx = sim3rx;
    }

    public long getSim3tx() {
        return sim3tx;
    }

    public void setSim3tx(long sim3tx) {
        this.sim3tx = sim3tx;
    }

    public long getTotal3() {
        return total3;
    }

    public void setTotal3(long total3) {
        this.total3 = total3;
    }

    public int getPeriod1() {
        return period1;
    }

    public void setPeriod1(int period1) {
        this.period1 = period1;
    }

    public int getPeriod2() {
        return period2;
    }

    public void setPeriod2(int period2) {
        this.period2 = period2;
    }

    public int getPeriod3() {
        return period3;
    }

    public void setPeriod3(int period3) {
        this.period3 = period3;
    }

    public long getSim1rx_n() {
        return sim1rx_n;
    }

    public void setSim1rx_n(long sim1rx_n) {
        this.sim1rx_n = sim1rx_n;
    }

    public long getSim1tx_n() {
        return sim1tx_n;
    }

    public void setSim1tx_n(long sim1tx_n) {
        this.sim1tx_n = sim1tx_n;
    }

    public long getTotal1_n() {
        return total1_n;
    }

    public void setTotal1_n(long total1_n) {
        this.total1_n = total1_n;
    }

    public long getSim2rx_n() {
        return sim2rx_n;
    }

    public void setSim2rx_n(long sim2rx_n) {
        this.sim2rx_n = sim2rx_n;
    }

    public long getSim2tx_n() {
        return sim2tx_n;
    }

    public void setSim2tx_n(long sim2tx_n) {
        this.sim2tx_n = sim2tx_n;
    }

    public long getTotal2_n() {
        return total2_n;
    }

    public void setTotal2_n(long total2_n) {
        this.total2_n = total2_n;
    }

    public long getSim3rx_n() {
        return sim3rx_n;
    }

    public void setSim3rx_n(long sim3rx_n) {
        this.sim3rx_n = sim3rx_n;
    }

    public long getSim3tx_n() {
        return sim3tx_n;
    }

    public void setSim3tx_n(long sim3tx_n) {
        this.sim3tx_n = sim3tx_n;
    }

    public long getTotal3_n() {
        return total3_n;
    }

    public void setTotal3_n(long total3_n) {
        this.total3_n = total3_n;
    }

    @Override
    public String toString() {
        return "Data{" +
                "id=" + id +
                ", date='" + date + '\'' +
                ", time='" + time + '\'' +
                ", last_sim=" + last_sim +
                ", lastrx=" + lastrx +
                ", lasttx=" + lasttx +
                ", sim1rx=" + sim1rx +
                ", sim1tx=" + sim1tx +
                ", total1=" + total1 +
                ", sim2rx=" + sim2rx +
                ", sim2tx=" + sim2tx +
                ", total2=" + total2 +
                ", sim3rx=" + sim3rx +
                ", sim3tx=" + sim3tx +
                ", total3=" + total3 +
                ", period1=" + period1 +
                ", period2=" + period2 +
                ", period3=" + period3 +
                ", sim1rx_n=" + sim1rx_n +
                ", sim1tx_n=" + sim1tx_n +
                ", total1_n=" + total1_n +
                ", sim2rx_n=" + sim2rx_n +
                ", sim2tx_n=" + sim2tx_n +
                ", total2_n=" + total2_n +
                ", sim3rx_n=" + sim3rx_n +
                ", sim3tx_n=" + sim3tx_n +
                ", total3_n=" + total3_n +
                '}';
    }
}
