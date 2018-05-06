package com.openshamba.watchdog.adapters;

import java.io.Serializable;
import java.util.ArrayList;

public class Group implements Serializable {
    private long id;
    private String date;
    private String name;
    private String snippet;
    private int photo;

    public Group(long id, String date, String name, String snippet, int photo) {
        this.id = id;
        this.date = date;
        this.name = name;
        this.snippet = snippet;
        this.photo = photo;
    }

    public long getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public String getName() {
        return name;
    }

    public String getSnippet() {
        return snippet;
    }

    public int getPhoto() {
        return photo;
    }

}