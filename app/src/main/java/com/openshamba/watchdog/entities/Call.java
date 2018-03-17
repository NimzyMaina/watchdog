package com.openshamba.watchdog.entities;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import java.util.UUID;

//import io.reactivex.annotations.NonNull;

/**
 * Created by Maina on 3/14/2018.
 */

@Entity
public class Call {
    @PrimaryKey
    @NonNull
    private String reference;
    private String type;
    private String charge_code;
    private String phone;
    private String contact;
    private String duration;
    private String start;
    private String end;
    private Boolean complete;
    private Boolean synced;

    public Call(){}

    @Ignore
    public Call(String phone,String contact,String type) {
        this.reference = UUID.randomUUID().toString();
        this.type = type;
        this.phone = phone;
        this.contact = contact;
        this.charge_code = "";
        this.duration = "";
        this.complete = false;
        this.synced = false;
    }

    @Ignore
    public Call(String phone,String contact,String type,String charge_code) {
        this.reference = UUID.randomUUID().toString();
        this.type = type;
        this.phone = phone;
        this.contact = contact;
        this.charge_code = charge_code;
        this.duration = "";
        this.complete = false;
        this.synced = false;
    }

    @NonNull
    public String getReference() {
        return reference;
    }

    public void setReference(@NonNull String reference) {
        this.reference = reference;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCharge_code() {
        return charge_code;
    }

    public void setCharge_code(String charge_code) {
        this.charge_code = charge_code;
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

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public Boolean getComplete() {
        return complete;
    }

    public void setComplete(Boolean complete) {
        this.complete = complete;
    }

    public Boolean getSynced() {
        return synced;
    }

    public void setSynced(Boolean synced) {
        this.synced = synced;
    }

    @Override
    public String toString() {
        return "Call{" +
                "reference='" + reference + '\'' +
                ", type='" + type + '\'' +
                ", charge_code='" + charge_code + '\'' +
                ", phone='" + phone + '\'' +
                ", contact='" + contact + '\'' +
                ", duration='" + duration + '\'' +
                ", start='" + start + '\'' +
                ", end='" + end + '\'' +
                ", complete=" + complete +
                ", synced=" + synced +
                '}';
    }
}
