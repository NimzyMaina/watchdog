package com.openshamba.watchdog.entities;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

/**
 * Created by Maina on 3/17/2018.
 */

@Entity
public class Sms {
    @NonNull
    @PrimaryKey
    private int id;
    private String phone;
    private String contact;
    private String type;
    private String reference;
    private Boolean synced;
    private String time;
    private String charge_code;

    public Sms() {
    }

    @Ignore
    public Sms(@NonNull int id, String phone,String contact, String type, String reference, String time) {
        this.id = id;
        this.phone = phone;
        this.contact = contact;
        this.type = type;
        this.reference = reference;
        this.time = time;
        this.synced = false;
        this.charge_code = "";
    }

    @NonNull
    public int getId() {
        return id;
    }

    public void setId(@NonNull int id) {
        this.id = id;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public Boolean getSynced() {
        return synced;
    }

    public void setSynced(Boolean synced) {
        this.synced = synced;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getCharge_code() {
        return charge_code;
    }

    public void setCharge_code(String charge_code) {
        this.charge_code = charge_code;
    }

    @Override
    public String toString() {
        return "Sms{" +
                "id=" + id +
                ", phone='" + phone + '\'' +
                ", contact='" + contact + '\'' +
                ", type='" + type + '\'' +
                ", reference='" + reference + '\'' +
                ", synced=" + synced +
                ", time='" + time + '\'' +
                ", charge_code='" + charge_code + '\'' +
                '}';
    }
}
