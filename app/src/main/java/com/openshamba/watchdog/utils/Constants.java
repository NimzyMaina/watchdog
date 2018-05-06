package com.openshamba.watchdog.utils;

import android.content.Context;
import android.content.res.TypedArray;

import com.openshamba.watchdog.R;
import com.openshamba.watchdog.adapters.Group;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Maina on 3/13/2018.
 */

@SuppressWarnings("ResourceType")
public class Constants {

    public static final int SIM1 = 0;
    public static final int SIM2 = 1;
    public static final int SIM3 = 2;
    public static final int DISABLED = -1;
    public static final String TRAFFIC = "data";
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String TIME_FORMAT = "HH:mm";
    public static final String LOG = "NIMZYMAINA";

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormat.forPattern(DATE_FORMAT);
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormat.forPattern(TIME_FORMAT + ":ss");
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern(DATE_FORMAT + " " + TIME_FORMAT);
    public static final DateTimeFormatter DATE_TIME_FORMATTER_SECONDS = DateTimeFormat.forPattern(DATE_FORMAT + " " + TIME_FORMAT + ":ss");

    public static final String[] PREF_OTHER = {
            "sim_quantity",//55-0
            "subinfo",// 56-1
            "save_profiles_traffic",//44-2
            "count_stopped",//5-3
            "watchdog",//4-4
            "last_sim",//46-5
            "traffic_running",//4-6
            "auto_sim",//13-7
            "user_sim",//14-8
            "watchdog_stopped",//6-9
    };

    public static final String OUTGOING_CALL_STARTED = "com.openshamba.watchdog.CALL_STARTED";

    public interface ACTION {
        public static String MAIN_ACTION = "com.openshamba.watchdog.action.main";
        public static String STARTFOREGROUND_ACTION = "com.openshamba.watchdog.action.startforeground";
        public static String STOPFOREGROUND_ACTION = "com.openshamba.watchdog.action.stopforeground";
        public static String RESTART_SERVICE = "com.openshamba.watchdog.action.restartforeground";
        public static String SHOW_HUD = "com.openshamba.watchdog.action.showhud";
        public static String TRAFFIC_BROADCAST_ACTION = "com.openshamba.watchdog.action.DATA_BROADCAST";
        public static String DATA_DEFAULT_SIM = "android.intent.action.DATA_DEFAULT_SIM";
    }

    public interface NOTIFICATION_ID {
        public static int FOREGROUND_SERVICE = 101;
    }

    public static final String SIM1RX = "sim1rx";
    public static final String SIM2RX = "sim2rx";
    public static final String SIM3RX = "sim3rx";
    public static final String SIM1TX = "sim1tx";
    public static final String SIM2TX = "sim2tx";
    public static final String SIM3TX = "sim3tx";
    public static final String TOTAL1 = "total1";
    public static final String TOTAL2 = "total2";
    public static final String TOTAL3 = "total3";
    public static final String SIM1RX_N = "sim1rx_n";
    public static final String SIM2RX_N = "sim2rx_n";
    public static final String SIM3RX_N = "sim3rx_n";
    public static final String SIM1TX_N = "sim1tx_n";
    public static final String SIM2TX_N = "sim2tx_n";
    public static final String SIM3TX_N = "sim3tx_n";
    public static final String TOTAL1_N = "total1_n";
    public static final String TOTAL2_N = "total2_n";
    public static final String TOTAL3_N = "total3_n";
    public static final String SIM_ACTIVE = "active_sim";
    public static final String OPERATOR1 = "operator1";
    public static final String OPERATOR2 = "operator2";
    public static final String OPERATOR3 = "operator3";

    public static final String SPEEDRX = "rx_speed";
    public static final String SPEEDTX = "tx_speed";
    public static final String PERIOD3 = "period3";
    public static final String PERIOD2 = "period2";
    public static final String PERIOD1 = "period1";

    public static final int COUNT = 1001;
    public static final int CHECK = 1002;
    public static final int MINUTE = 60 * 1000;
    public static final int SECOND = 1000;
    public static final long NOTIFY_INTERVAL = 1000; // 1 second

    public static final String[] PREF_SIM1 = {"stub1", "limit1", "value1", "period1", "round1", "auto1", //5
            "name1", "autooff1", "prefer1", "time1", "day1", "everydayonoff1", "timeoff1", "timeon1", //13
            "op_round1", "op_limit1", "op_value1", "usenight1", "limitnight1", //18
            "valuenight1", "nighton1", "nightoff1", "nightround1", "operator_logo1", "reset1", "needsreset1", //25
            "nextreset1", "overlimit1", "action_chosen1", "prelimit1", "prelimitpercent1", "autoenable1", //31
            "onlyreceived1", "use_uid1"}; //33
    public static final String[] PREF_SIM2 = {"stub2", "limit2", "value2", "period2", "round2", "auto2", //5
            "name2", "autooff2", "prefer2", "time2", "day2", "everydayonoff2", "timeoff2", "timeon2", //13
            "op_round2", "op_limit2", "op_value2", "usenight2", "limitnight2", //18
            "valuenight2", "nighton2", "nightoff2", "nightround2", "operator_logo2", "reset2", "needsreset2", //25
            "nextreset2", "overlimit2", "action_chosen2", "prelimit2", "prelimitpercent2", "autoenable2", //31
            "onlyreceived2", "use_uid2"}; //33
    public static final String[] PREF_SIM3 = {"stub3", "limit3", "value3", "period3", "round3", "auto3", //5
            "name3", "autooff3", "prefer3", "time3", "day3", "everydayonoff3", "timeoff3", "timeon3", //13
            "op_round3", "op_limit31", "op_value3", "usenight3", "limitnight3", //18
            "valuenight3", "nighton3", "nightoff3", "nightround3", "operator_logo3", "reset3", "needsreset3", //25
            "nextreset3", "overlimit3", "action_chosen3", "prelimit3", "prelimitpercent3", "autoenable3", //31
            "onlyreceived3", "use_uid3"}; //33

    public static final String[] PREF_SIM_DATA = {"stub", "limit", "value", "period", "round", "auto", //5
            "name", "autooff", "prefer", "time", "day", "everydayonoff", "timeoff", "timeon", //13
            "op_round", "op_limit", "op_value", "usenight", "limitnight", //18
            "valuenight", "nighton", "nightoff", "nightround", "operator_logo", "reset", "needsreset", //25
            "nextreset", "overlimit", "action_chosen", "prelimit", "prelimitpercent", "autoenable", //31
            "onlyreceived", "use_uid"}; //33

    public static List<Group> getGroupData(Context ctx)  {
        List<Group> items = new ArrayList<>();
        String s_name[] = ctx.getResources().getStringArray(R.array.groups_name);
        String s_date[] = ctx.getResources().getStringArray(R.array.groups_date);
        TypedArray drw_arr = ctx.getResources().obtainTypedArray(R.array.groups_photos);

        items.add(new Group(0, s_date[0], s_name[0], "", drw_arr.getResourceId(0,-1)));
        items.add(new Group(1, s_date[1], s_name[1], "", drw_arr.getResourceId(1,-1)));
        items.add(new Group(2, s_date[2], s_name[2], "", drw_arr.getResourceId(2,-1)));

        return items;
    }
}
