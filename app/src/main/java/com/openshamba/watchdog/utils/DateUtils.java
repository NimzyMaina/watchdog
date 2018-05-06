package com.openshamba.watchdog.utils;

import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;

import static java.lang.Integer.parseInt;

/**
 * Created by Maina on 4/26/2018.
 */

public class DateUtils {
    public static boolean isNextDayOrMonth(LocalDateTime date, String period) {
        LocalDateTime now = new DateTime().withTimeAtStartOfDay().toLocalDateTime();
        switch (period) {
            case "0":
                return now.getDayOfYear() != date.getDayOfYear();
            case "1":
                return now.getMonthOfYear() != date.getMonthOfYear();
            default:
                return false;
        }
    }

    @Nullable
    public static LocalDateTime setResetDate(SharedPreferences preferences, String[] simPref) {
        String time = preferences.getString(simPref[1], "00:00");
        LocalDateTime now = new LocalDateTime()
                .withHourOfDay(Integer.valueOf(time.split(":")[0]))
                .withMinuteOfHour(Integer.valueOf(time.split(":")[1]));
        int delta = parseInt(preferences.getString(simPref[2], "1"));
        switch (preferences.getString(simPref[0], "")) {
            case "0":
                delta = 1;
                return now.plusDays(delta);
            case "1":
                if (delta >= now.getDayOfMonth())
                    return now.withDayOfMonth(delta);
                else
                    return now.plusMonths(1).withDayOfMonth(delta);
            case "2":
                return now.plusDays(delta);
        }
        return null;
    }
}
