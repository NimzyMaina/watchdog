package com.openshamba.watchdog.utils;

/**
 * Created by Maina on 3/13/2018.
 */

public class Constants {

    public static final int SIM1 = 0;
    public static final int SIM2 = 1;
    public static final int SIM3 = 2;
    public static final int DISABLED = -1;

    public static final String[] PREF_OTHER = {
            "sim_quantity", "subinfo"
    };

    public static final String OUTGOING_CALL_STARTED = "com.openshamba.watchdog.CALL_STARTED";

    public interface ACTION {
        public static String MAIN_ACTION = "com.openshamba.watchdog.action.main";
        public static String STARTFOREGROUND_ACTION = "com.openshamba.watchdog.action.startforeground";
        public static String STOPFOREGROUND_ACTION = "com.openshamba.watchdog.action.stopforeground";
        public static String RESTART_SERVICE = "com.openshamba.watchdog.action.restartforeground";
        public static String SHOW_HUD = "com.openshamba.watchdog.action.showhud";
    }

    public interface NOTIFICATION_ID {
        public static int FOREGROUND_SERVICE = 101;
    }
}
