package com.openshamba.watchdog.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.openshamba.watchdog.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Maina on 3/13/2018.
 */

public class MobileUtils {

    private static final String MEDIATEK = "com.mediatek.telephony.TelephonyManagerEx";
    private static final String GET_CALL = "getCallState";
    private static final String GET_DATA = "getDataState";
    private static final String GET_SUBID = "getSubIdBySlot";
    private static final String GET_IMEI = "getDeviceId";
    private static final String GET_IMSI = "getSubscriberId";
    private static final String GET_NAME = "getNetworkOperatorName";

    private static Class<?> mTelephonyClass = null;

    private static Method mGetCallState = null;
    private static Method mGetSubIdBySlot = null;
    private static Method mGetITelephony = null;
    private static Method mFrom = null;
    private static Method mGetDeviceId = null;
    private static Method mGetSubscriberId = null;
    private static Method mGetNetworkOperatorName = null;
    private static Method mGetDefaultDataSubscriptionInfo = null;
    private static Method mGetDefaultDataSubId = null;
    private static Method mGetSimId = null;
    private static Method mGetDataState = null;

    private static ArrayList<Long> mSubIds = null;
    private static int mLastActiveSIM;

    private static final int NT_WCDMA_PREFERRED = 0;             // GSM/WCDMA (WCDMA preferred) (2g/3g)
    private static final int NT_GSM_ONLY = 1;                    // GSM Only (2g)
    private static final int NT_WCDMA_ONLY = 2;                  // WCDMA ONLY (3g)
    private static final int NT_GSM_WCDMA_AUTO = 3;              // GSM/WCDMA Auto (2g/3g)
    private static final int NT_CDMA_EVDO = 4;                   // CDMA/EVDO Auto (2g/3g)
    private static final int NT_CDMA_ONLY = 5;                   // CDMA Only (2G)
    private static final int NT_EVDO_ONLY = 6;                   // Evdo Only (3G)
    private static final int NT_GLOBAL = 7;                      // GSM/WCDMA/CDMA Auto (2g/3g)
    private static final int NT_LTE_CDMA_EVDO = 8;
    private static final int NT_LTE_GSM_WCDMA = 9;
    private static final int NT_LTE_CMDA_EVDO_GSM_WCDMA = 10;
    private static final int NT_LTE_ONLY = 11;
    private static final int NT_LTE_WCDMA = 12;

    @TargetApi(Build.VERSION_CODES.M)
    public static int getActiveSimForCallM(final Context context, int simQuantity, ArrayList<String> list) {
        final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int sim = -1;
        if (mTelephonyClass == null)
            try {
                if (tm != null) {
                    mTelephonyClass = Class.forName(tm.getClass().getName());
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        if (mTelephonyClass != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (mGetCallState == null)
                mGetCallState = getMethod(mTelephonyClass, GET_CALL, 1);
            if (mGetCallState != null)
                for (int i = 0; i < simQuantity; i++) {
                    try {
                        int state = (int) mGetCallState.invoke(mTelephonyClass.getConstructor(Context.class).newInstance(context), Integer.valueOf(list.get(i)));
                        if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                            sim = i;
                            break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
        }
        return sim;
    }

    public static int getActiveSimForCall(Context context, int simQuantity) {
        final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (mTelephonyClass == null)
            try {
                if (tm != null) {
                    mTelephonyClass = Class.forName(tm.getClass().getName());
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        if (simQuantity > 1) {
            int sim = Constants.DISABLED;
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
                if (CustomApplication.isOldMtkDevice()) {
                    try {
                        Class<?> c = Class.forName(MEDIATEK);
                        if (mGetCallState == null)
                            mGetCallState = getMethod(c, GET_CALL, 1);
                        if (mGetCallState != null)
                            for (int i = 0; i < simQuantity; i++) {
                                try {
                                    int state = (int) mGetCallState.invoke(c.getConstructor(Context.class).newInstance(context), i);
                                    if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                                        sim = i;
                                        break;
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (sim == Constants.DISABLED) {
                        try {
                            Class<?> c = Class.forName(MEDIATEK);
                            if (mGetCallState == null)
                                mGetCallState = getMethod(c, GET_CALL, 1);
                            if (mSubIds == null)
                                mSubIds = getSubIds(mTelephonyClass, simQuantity, context);
                            if (mGetCallState != null && mSubIds != null)
                                for (int i = 0; i < simQuantity; i++) {
                                    try {
                                        int state = (int) mGetCallState.invoke(c.getConstructor(Context.class).newInstance(context), mSubIds.get(i));
                                        if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                                            sim = i;
                                            break;
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    try {
                        if (mGetCallState == null)
                            mGetCallState = getMethod(mTelephonyClass, GET_CALL, 1);
                        int state = -1;
                        if (mGetCallState != null)
                            for (int i = 0; i < simQuantity; i++) {
                                if (mGetCallState.getParameterTypes()[0].equals(int.class)) {
                                    state = (int) mGetCallState.invoke(mTelephonyClass.getConstructor(Context.class).newInstance(context), i);
                                } else if (mGetCallState.getParameterTypes()[0].equals(long.class)) {
                                    if (mSubIds == null)
                                        mSubIds = getSubIds(mTelephonyClass, simQuantity, context);
                                    state = (int) mGetCallState.invoke(mTelephonyClass.getConstructor(Context.class).newInstance(context), mSubIds.get(i));
                                }
                                if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                                    sim = i;
                                    break;
                                }

                            }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (sim == Constants.DISABLED) {
                        if (mGetCallState == null)
                            mGetCallState = getMethod(mTelephonyClass, GET_CALL + "Ext", 1);
                        if (mGetCallState != null)
                            for (int i = 0; i < simQuantity; i++) {
                                try {
                                    int state = (int) mGetCallState.invoke(mTelephonyClass.getConstructor(Context.class).newInstance(context), i);
                                    if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                                        sim = i;
                                        break;
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                    }
                    if (sim == Constants.DISABLED) {
                        if (mGetITelephony == null)
                            mGetITelephony = getMethod(mTelephonyClass, "getITelephony", 1);
                        if (mGetCallState != null)
                            for (int i = 0; i < simQuantity; i++) {
                                try {
                                    Object mTelephonyStub = null;
                                    if (mGetITelephony != null) {
                                        mTelephonyStub = mGetITelephony.invoke(tm, i);
                                    }
                                    Class<?> mTelephonyStubClass = null;
                                    if (mTelephonyStub != null) {
                                        mTelephonyStubClass = Class.forName(mTelephonyStub.getClass().getName());
                                    }
                                    Class<?> mClass = null;
                                    if (mTelephonyStubClass != null) {
                                        mClass = mTelephonyStubClass.getDeclaringClass();
                                    }
                                    Method getState = null;
                                    if (mClass != null) {
                                        getState = mClass.getDeclaredMethod(GET_CALL);
                                    }
                                    int state = 0;
                                    if (getState != null) {
                                        state = (int) getState.invoke(mClass);
                                    }
                                    if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                                        sim = i;
                                        break;
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                    }
                    if (sim == Constants.DISABLED) {
                        try {
                            if (mFrom == null)
                                mFrom = getMethod(mTelephonyClass, "from", 2);
                            if (mFrom != null)
                                for (int i = 0; i < simQuantity; i++) {
                                    final Object[] params = {context, i};
                                    final TelephonyManager mTelephonyStub = (TelephonyManager) mFrom.invoke(tm, params);
                                    int state = mTelephonyStub.getCallState();
                                    if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                                        sim = i;
                                        break;
                                    }
                                }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            return sim;
        } else
            return Constants.SIM1;
    }

    private static Method getMethod(Class c, String name, int params) {
        Method[] cm = c.getDeclaredMethods();
        for (Method m : cm) {
            if (m.getName().equalsIgnoreCase(name)) {
                m.setAccessible(true);
                int length = m.getParameterTypes().length;
                if (length == params)
                    return m;
            }
        }
        return null;
    }

    public static int isMultiSim(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            SubscriptionManager sm = SubscriptionManager.from(context);
            List<SubscriptionInfo> sl = sm.getActiveSubscriptionInfoList();
            if (sl != null)
                return sl.size();
            else
                return 0;
        } else {
            int simQuantity = 1;
            final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (mTelephonyClass == null)
                try {
                    if (tm != null) {
                        mTelephonyClass = Class.forName(tm.getClass().getName());
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            if (CustomApplication.isOldMtkDevice()) {
                try {
                    Class<?> c = Class.forName(MEDIATEK);
                    if (mGetDeviceId == null)
                        mGetDeviceId = getMethod(c, GET_IMEI, 1);
                    for (int i = 0; i < 2; i++) {
                        String id = (String) mGetDeviceId.invoke(c.getConstructor(android.content.Context.class).newInstance(context), i);
                        String idNext = (String) mGetDeviceId.invoke(c.getConstructor(android.content.Context.class).newInstance(context), i + 1);
                        if (idNext != null && !id.equals(idNext))
                            simQuantity++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (simQuantity == 1)
                    try {
                        Class<?> c = Class.forName(MEDIATEK);
                        if (mGetDeviceId == null) {
                            mGetDeviceId = getMethod(c, GET_IMEI, 1);
                        }
                        if (mSubIds == null)
                            mSubIds = getSubIds(mTelephonyClass, simQuantity, context);
                        for (int i = 0; i < 2; i++) {
                            String id = (String) mGetDeviceId.invoke(c.getConstructor(android.content.Context.class).newInstance(context), mSubIds.get(i));
                            String idNext = (String) mGetDeviceId.invoke(c.getConstructor(android.content.Context.class).newInstance(context), mSubIds.get(i + 1));
                            if (idNext != null && !id.equals(idNext))
                                simQuantity++;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
            } else {
                try {
                    if (mGetSubIdBySlot == null)
                        mGetSubIdBySlot = getMethod(mTelephonyClass, GET_SUBID, 1);
                    for (int i = 0; i < 2; i++) {
                        long id = (long) mGetSubIdBySlot.invoke(mTelephonyClass.getConstructor(android.content.Context.class).newInstance(context), i);
                        long idNext = (long) mGetSubIdBySlot.invoke(mTelephonyClass.getConstructor(android.content.Context.class).newInstance(context), i + 1);
                        if (idNext != 0 && id != idNext)
                            simQuantity++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (simQuantity == 1)
                    try {
                        if (mGetDeviceId == null)
                            mGetDeviceId = getMethod(mTelephonyClass, GET_IMEI + "Ext", 1);
                        for (int i = 0; i < 2; i++) {
                            String id = (String) mGetDeviceId.invoke(mTelephonyClass.getConstructor(android.content.Context.class).newInstance(context), i);
                            String idNext = (String) mGetDeviceId.invoke(mTelephonyClass.getConstructor(android.content.Context.class).newInstance(context), i + 1);
                            if (idNext != null && !id.equals(idNext))
                                simQuantity++;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                if (simQuantity == 1)
                    try {
                        if (mGetDeviceId == null)
                            mGetDeviceId = getMethod(mTelephonyClass, GET_IMEI, 1);
                        if (mGetDeviceId.getParameterTypes()[0].equals(int.class)) {
                            for (int i = 0; i < 2; i++) {
                                String id = (String) mGetDeviceId.invoke(mTelephonyClass.getConstructor(android.content.Context.class).newInstance(context), i);
                                String idNext = (String) mGetDeviceId.invoke(mTelephonyClass.getConstructor(android.content.Context.class).newInstance(context), i + 1);
                                if (idNext != null && !id.equals(idNext))
                                    simQuantity++;
                            }
                        } else if (mGetDeviceId.getParameterTypes()[0].equals(long.class)) {
                            if (mSubIds == null)
                                mSubIds = getSubIds(mTelephonyClass, simQuantity, context);
                            for (int i = 0; i < 2; i++) {
                                String id = (String) mGetDeviceId.invoke(mTelephonyClass.getConstructor(android.content.Context.class).newInstance(context), mSubIds.get(i));
                                String idNext = (String) mGetDeviceId.invoke(mTelephonyClass.getConstructor(android.content.Context.class).newInstance(context), mSubIds.get(i + 1));
                                if (idNext != null && !id.equals(idNext))
                                    simQuantity++;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                if (simQuantity == 1)
                    try {
                        if (mGetITelephony == null)
                            mGetITelephony = getMethod(mTelephonyClass, "getITelephony", 1);
                        for (int i = 0; i < 2; i++) {
                            Object mTelephonyStub = null;
                            if (mGetITelephony != null) {
                                mTelephonyStub = mGetITelephony.invoke(tm, i);
                            }
                            if (mTelephonyStub != null)
                                simQuantity++;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                if (simQuantity == 1)
                    try {
                        if (mFrom == null)
                            mFrom = getMethod(mTelephonyClass, "from", 2);
                        for (int i = 0; i < 2; i++) {
                            final Object[] params = {context, i};
                            Object mTelephonyStub = null;
                            if (mFrom != null) {
                                mTelephonyStub = mFrom.invoke(tm, params);
                            }
                            if (mTelephonyStub != null)
                                simQuantity++;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
            }
            return simQuantity;
        }
    }

    private static ArrayList<Long> getSubIds(Class<?> telephonyClass, int simQuantity, Context context) {
        ArrayList<Long> subIds = new ArrayList<>();
        try {
            if (mGetSubIdBySlot == null)
                mGetSubIdBySlot = getMethod(telephonyClass, GET_SUBID, 1);
            for (int i = 0; i < simQuantity; i++) {
                try {
                    subIds.add(i, (long) mGetSubIdBySlot.invoke(telephonyClass.getConstructor(Context.class).newInstance(context), i));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return subIds;
    }

    @SuppressLint("HardwareIds")
    public static ArrayList<String> getSimIds(Context context) {
        ArrayList<String> imsi = new ArrayList<>();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int simQuantity = prefs.getInt(Constants.PREF_OTHER[0], 1);
        final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm != null) {
            if (simQuantity > 1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    SubscriptionManager sm = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                    List<SubscriptionInfo> sl;
                    if (sm != null) {
                        sl = sm.getActiveSubscriptionInfoList();
                        if (mGetSubscriberId == null)
                            mGetSubscriberId = getMethod(tm.getClass(), GET_IMSI, 1);
                        if (sl != null)
                            for (SubscriptionInfo si : sl) {
                                try {
                                    imsi.add((String) mGetSubscriberId.invoke(tm.getClass().getConstructor(Context.class).newInstance(context), si.getSubscriptionId()));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                    }
                } else {
                    if (CustomApplication.isOldMtkDevice()) {
                        try {
                            Class<?> c = Class.forName(MEDIATEK);
                            if (mGetSubscriberId == null)
                                mGetSubscriberId = getMethod(c, GET_IMSI, 1);
                            for (int i = 0; i < simQuantity; i++) {
                                imsi.add(i, (String) mGetSubscriberId.invoke(c.getConstructor(Context.class).newInstance(context), i));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (imsi.size() == 0) {
                            try {
                                Class<?> c = Class.forName(MEDIATEK);
                                if (mGetSubscriberId == null)
                                    mGetSubscriberId = getMethod(c, GET_IMSI, 1);
                                if (mSubIds == null)
                                    mSubIds = getSubIds(mTelephonyClass, simQuantity, context);
                                for (int i = 0; i < simQuantity; i++) {
                                    imsi.add(i, (String) mGetSubscriberId.invoke(c.getConstructor(Context.class).newInstance(context), mSubIds.get(i)));
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        if (mTelephonyClass == null)
                            try {
                                mTelephonyClass = Class.forName(tm.getClass().getName());
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                        if (imsi.size() == 0) {
                            try {
                                if (mGetSubscriberId == null)
                                    mGetSubscriberId = getMethod(mTelephonyClass, GET_IMSI, 1);
                                if (mGetSubscriberId != null) {
                                    if (mGetSubscriberId.getParameterTypes()[0].equals(int.class)) {
                                        for (int i = 0; i < simQuantity; i++) {
                                            imsi.add(i, (String) mGetSubscriberId.invoke(mTelephonyClass.getConstructor(Context.class).newInstance(context), i));
                                        }
                                    } else if (mGetDeviceId.getParameterTypes()[0].equals(long.class)) {
                                        if (mSubIds == null)
                                            mSubIds = getSubIds(mTelephonyClass, simQuantity, context);
                                        for (int i = 0; i < simQuantity; i++) {
                                            imsi.add(i, (String) mGetSubscriberId.invoke(mTelephonyClass.getConstructor(Context.class).newInstance(context), mSubIds.get(i)));
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        if (imsi.size() == 0) {
                            try {
                                if (mGetSubscriberId == null)
                                    mGetSubscriberId = getMethod(mTelephonyClass, GET_IMSI + "Ext", 1);
                                for (int i = 0; i < simQuantity; i++) {
                                    imsi.add(i, (String) mGetSubscriberId.invoke(mTelephonyClass.getConstructor(Context.class).newInstance(context), i));
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        if (imsi.size() == 0) {
                            try {
                                if (mFrom == null)
                                    mFrom = getMethod(mTelephonyClass, "from", 2);
                                for (int i = 0; i < simQuantity; i++) {
                                    final Object[] params = {context, i};
                                    TelephonyManager mTelephonyStub = null;
                                    if (mFrom != null) {
                                        mTelephonyStub = (TelephonyManager) mFrom.invoke(tm, params);
                                    }
                                    if (mTelephonyStub != null) {
                                        if (mGetSubscriberId == null)
                                            mGetSubscriberId = getMethod(mTelephonyStub.getClass(), GET_IMSI, 1);
                                        imsi.add(i, (String) mGetSubscriberId.invoke(mTelephonyStub.getClass().getConstructor(Context.class).newInstance(context), i));
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } else {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    return imsi;
                }
                imsi.add(tm.getDeviceId());
            }
        }
        return imsi;
    }

    public static boolean isMobileDataActive(Context context) {
        return hasActiveNetworkInfo(context) == 2 && (isMobileDataEnabledFromSettings(context)
                || isMobileDataEnabledFromConnectivityManager(context) == 1);
    }

    private static boolean isMobileDataEnabledFromSettings(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // The row has been moved to 'global' table in API level 17
            return Settings.Global.getInt(context.getContentResolver(), "mobile_data", 0) != 0;
        }
        try {
            // It was in 'secure' table before
            return Settings.Secure.getInt(context.getContentResolver(), "mobile_data") != 0;
        } catch (Settings.SettingNotFoundException e) {
            // It was in 'system' table originally, but I don't remember when that was the case.
            // So, probably, you won't need all these try/catches.
            // But, hey, it is better to be safe than sorry :)
            return Settings.System.getInt(context.getContentResolver(), "mobile_data", 0) != 0;
        }
    }

    private static int isMobileDataEnabledFromConnectivityManager(Context context) {
        final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            Class<?> c = null;
            if (cm != null) {
                c = Class.forName(cm.getClass().getName());
            }
            Method m;
            if (c != null) {
                m = getMethod(c, "getMobileDataEnabled", 0);
                if (m != null)
                    return (boolean) m.invoke(cm) ? 1 : 0;
                else
                    return 2;
            } else
                return 2;
        } catch (Exception e) {
            e.printStackTrace();
            return 2;
        }
    }

    public static int hasActiveNetworkInfo(Context context) {
        int state = 0; // Assume disabled
        final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mActiveNetworkInfo = null;
        if (cm != null) {
            mActiveNetworkInfo = cm.getActiveNetworkInfo();
        }
        if (mActiveNetworkInfo != null) {
            String typeName = mActiveNetworkInfo.getTypeName().toLowerCase();
            boolean isConnected = mActiveNetworkInfo.isConnectedOrConnecting();
            int type = mActiveNetworkInfo.getType();
            if ((isNetworkTypeMobile(type)) && (typeName.contains("mobile")) && isConnected)
                state = 2;
            else if ((!isNetworkTypeMobile(type)) && (!typeName.contains("mobile")) && isConnected)
                state = 1;
        }
        return state;
    }

    private static boolean isNetworkTypeMobile(int networkType) {
        switch (networkType) {
            case NT_WCDMA_PREFERRED:
            case NT_GSM_ONLY:
            case NT_WCDMA_ONLY:
            case NT_GSM_WCDMA_AUTO:
            case NT_CDMA_EVDO:
            case NT_CDMA_ONLY:
            case NT_EVDO_ONLY:
            case NT_GLOBAL:
            case NT_LTE_CDMA_EVDO:
            case NT_LTE_GSM_WCDMA:
            case NT_LTE_CMDA_EVDO_GSM_WCDMA:
            case NT_LTE_ONLY:
            case NT_LTE_WCDMA:
            case 14:
            case 15:
                return true;
            default:
                return false;
        }
    }

    public static String getName(Context context, String key1, String key2, int sim) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean(key1, true)) {
            ArrayList<String> names = getOperatorNames(context);
            return (names.size() > sim && names.get(sim) != null) ? names.get(sim) : context.getResources().getString(R.string.not_available);
        } else
            return prefs.getString(key2, "");
    }

    public static ArrayList<String> getOperatorNames(Context context) {
        String out = "";
        ArrayList<String> name = new ArrayList<>();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int simQuantity = prefs.getInt(Constants.PREF_OTHER[0], 1);
        final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (simQuantity > 1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                SubscriptionManager sm = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                List<SubscriptionInfo> sl = null;
                if (sm != null) {
                    sl = sm.getActiveSubscriptionInfoList();
                }
                if (sl != null)
                    for (SubscriptionInfo si : sl) {
                        name.add((String) si.getCarrierName());
                    }
                if (name.size() > 0)
                    out = "Subscription " + name.size();
            } else {
                if (mTelephonyClass == null)
                    try {
                        if (tm != null) {
                            mTelephonyClass = Class.forName(tm.getClass().getName());
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                if (CustomApplication.isOldMtkDevice()) {
                    try {
                        Class<?> c = Class.forName(MEDIATEK);
                        if (mGetNetworkOperatorName == null)
                            mGetNetworkOperatorName = getMethod(c, GET_NAME, 1);
                        for (int i = 0; i < simQuantity; i++) {
                            name.add(i, (String) mGetNetworkOperatorName.invoke(c.getConstructor(Context.class).newInstance(context), i));
                        }
                        if (name.size() > 0)
                            out = GET_NAME + "GeminiInt " + name.size();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (name.size() == 0) {
                        try {
                            Class<?> c = Class.forName(MEDIATEK);
                            if (mGetNetworkOperatorName == null)
                                mGetNetworkOperatorName = getMethod(c, GET_NAME, 1);
                            if (mSubIds == null)
                                mSubIds = getSubIds(mTelephonyClass, simQuantity, context);
                            for (int i = 0; i < simQuantity; i++) {
                                name.add(i, (String) mGetNetworkOperatorName.invoke(c.getConstructor(Context.class).newInstance(context), mSubIds.get(i)));
                            }
                            if (name.size() > 0)
                                out = GET_NAME + "GeminiLong " + name.size();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    if (name.size() == 0) {
                        try {
                            if (mSubIds == null)
                                mSubIds = getSubIds(mTelephonyClass, simQuantity, context);
                            if (mGetNetworkOperatorName == null)
                                mGetNetworkOperatorName = getMethod(mTelephonyClass, GET_NAME, 1);
                            for (long subId : mSubIds) {
                                String nameCurr = (String) mGetNetworkOperatorName.invoke(mTelephonyClass.getConstructor(Context.class).newInstance(context), subId);
                                if (!nameCurr.equals(""))
                                    name.add(nameCurr);
                            }
                            if (name.size() > 0)
                                out = GET_NAME + " " + name.size();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (name.size() == 0) {
                        try {
                            if (mGetNetworkOperatorName == null)
                                mGetNetworkOperatorName = getMethod(mTelephonyClass, GET_NAME + "Ext", 1);
                            for (int i = 0; i < simQuantity; i++) {
                                name.add(i, (String) mGetNetworkOperatorName.invoke(mTelephonyClass.getConstructor(Context.class).newInstance(context), i));
                            }
                            if (name.size() > 0)
                                out = GET_NAME + "Ext " + name.size();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (name.size() == 0) {
                        try {
                            if (mFrom == null)
                                mFrom = getMethod(mTelephonyClass, "from", 2);
                            for (int i = 0; i < simQuantity; i++) {
                                final Object[] params = {context, i};
                                TelephonyManager mTelephonyStub = null;
                                if (mFrom != null) {
                                    mTelephonyStub = (TelephonyManager) mFrom.invoke(tm, params);
                                }
                                if (mTelephonyStub != null)
                                    name.add(i, mTelephonyStub.getNetworkOperatorName());
                            }
                            if (name.size() > 0)
                                out = "from " + name.size();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } else if (tm != null) {
            name.add(tm.getNetworkOperatorName());
        }
        try {
            File dir = new File(String.valueOf(context.getFilesDir()));
            // create the file in which we will write the contents
            String fileName = "name_log.txt";
            File file = new File(dir, fileName);
            FileOutputStream os = new FileOutputStream(file);
            os.write(out.getBytes());
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return name;
    }

    public static int getActiveSimForData(Context context) {
        String out = " ";
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int sim = Constants.DISABLED;
        int simQuantity = prefs.getInt(Constants.PREF_OTHER[0], 1);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            SubscriptionManager sm = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            if (sm != null) {
                try {
                    int id = Settings.Global.getInt(context.getContentResolver(), "multi_sim_data_call");
                    SubscriptionInfo si = sm.getActiveSubscriptionInfo(id);
                    if (si != null) {
                        sim = si.getSimSlotIndex();
                        out = "getFromSettingsGlobal " + sim;
                    }
                } catch (Settings.SettingNotFoundException e) {
                    e.printStackTrace();
                }
                if (sim == Constants.DISABLED) {
                    try {
                        if (mGetDefaultDataSubscriptionInfo == null) {
                            mGetDefaultDataSubscriptionInfo = getMethod(sm.getClass(), "getDefaultDataSubscriptionInfo", 0);
                            if (mGetDefaultDataSubscriptionInfo != null) {
                                SubscriptionInfo si = (SubscriptionInfo) mGetDefaultDataSubscriptionInfo.invoke(sm);
                                if (si != null) {
                                    sim = si.getSimSlotIndex();
                                    out = "getDefaultDataSubscriptionInfo " + sim;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (sim == Constants.DISABLED) {
                    try {
                        if (mGetDefaultDataSubId == null) {
                            mGetDefaultDataSubId = getMethod(sm.getClass(), "getDefaultDataSubscriptionId", 0);
                            if (mGetDefaultDataSubId != null) {
                                int id = (int) mGetDefaultDataSubId.invoke(sm);
                                SubscriptionInfo si = sm.getActiveSubscriptionInfo(id);
                                if (si != null) {
                                    sim = si.getSimSlotIndex();
                                    out = "getDefaultDataSubId " + sim;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (sim == Constants.DISABLED) {
                    final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo activeNetworkInfo = null;
                    if (cm != null) {
                        activeNetworkInfo = cm.getActiveNetworkInfo();
                    }
                    if (activeNetworkInfo != null) {
                        List<SubscriptionInfo> sl = sm.getActiveSubscriptionInfoList();
                        if (sl != null)
                            for (int i = 0; i < sl.size(); i++) {
                                if (getNetworkFromApnsFile(String.valueOf(sl.get(i).getMcc()) + String.valueOf(sl.get(i).getMnc()), activeNetworkInfo.getExtraInfo())) {
                                    sim = sl.get(i).getSimSlotIndex();
                                    out = "getNetworkFromApnsFile " + sim;
                                    break;
                                }
                            }
                    }
                }
            }
        } else {
            if (simQuantity > 1) {
                final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetworkInfo = null;
                if (cm != null) {
                    activeNetworkInfo = cm.getActiveNetworkInfo();
                }
                if (activeNetworkInfo != null) {
                    Class c = null;
                    try {
                        c = Class.forName(activeNetworkInfo.getClass().getName());
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    if (c != null)
                        try {
                            if (mGetSimId == null)
                                mGetSimId = getMethod(c, "getSimId", 0);
                            if (mGetSimId != null) {
                                sim = (int) mGetSimId.invoke(activeNetworkInfo);
                            }
                            out = "getSimId " + sim;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                }
                if (sim == Constants.DISABLED) {
                    if (CustomApplication.isOldMtkDevice()) {
                        for (int i = 0; i < simQuantity; i++) {
                            int state = Constants.DISABLED;
                            try {
                                Class<?> c = Class.forName(MEDIATEK);
                                if (mGetDataState == null)
                                    mGetDataState = getMethod(c, GET_DATA, 1);
                                state = (int) mGetDataState.invoke(c.getConstructor(android.content.Context.class).newInstance(context), i);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (state == TelephonyManager.DATA_CONNECTED
                                    || state == TelephonyManager.DATA_CONNECTING
                                    || state == TelephonyManager.DATA_SUSPENDED) {
                                sim = i;
                                out = "getDataStateExInt " + sim;
                                break;
                            }
                        }
                        if (sim == Constants.DISABLED) {
                            for (int i = 0; i < simQuantity; i++) {
                                int state = Constants.DISABLED;
                                try {
                                    Class<?> c = Class.forName(MEDIATEK);
                                    if (mGetDataState == null)
                                        mGetDataState = getMethod(c, GET_DATA, 1);
                                    if (mSubIds == null)
                                        mSubIds = getSubIds(mTelephonyClass, simQuantity, context);
                                    state = (int) mGetDataState.invoke(c.getConstructor(android.content.Context.class).newInstance(context), mSubIds.get(i));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                if (state == TelephonyManager.DATA_CONNECTED
                                        || state == TelephonyManager.DATA_CONNECTING
                                        || state == TelephonyManager.DATA_SUSPENDED) {
                                    sim = i;
                                    out = "getDataStateExLong " + sim;
                                    break;
                                }
                            }
                        }
                    } else {
                        final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                        if (mTelephonyClass == null)
                            try {
                                if (tm != null) {
                                    mTelephonyClass = Class.forName(tm.getClass().getName());
                                }
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                        if (mTelephonyClass != null) {
                            for (int i = 0; i < simQuantity; i++) {
                                int state = Constants.DISABLED;
                                try {
                                    if (mGetDataState == null)
                                        mGetDataState = getMethod(mTelephonyClass, GET_DATA, 1);
                                    if (mGetDataState != null) {
                                        if (mGetDataState.getParameterTypes()[0].equals(int.class)) {
                                            state = (int) mGetDataState.invoke(mTelephonyClass.getConstructor(Context.class).newInstance(context), i);
                                        } else if (mGetDataState.getParameterTypes()[0].equals(long.class)) {
                                            if (mSubIds == null)
                                                mSubIds = getSubIds(mTelephonyClass, simQuantity, context);
                                            state = (int) mGetDataState.invoke(mTelephonyClass.getConstructor(Context.class).newInstance(context), mSubIds.get(i));
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                if (state == TelephonyManager.DATA_CONNECTED
                                        || state == TelephonyManager.DATA_CONNECTING
                                        || state == TelephonyManager.DATA_SUSPENDED) {
                                    sim = i;
                                    out = "getDataStateSubId " + sim;
                                    break;
                                }
                            }
                        }
                        if (sim == Constants.DISABLED) {
                            for (int i = 0; i < simQuantity; i++) {
                                int state = Constants.DISABLED;
                                try {
                                    if (mGetDataState == null)
                                        mGetDataState = getMethod(mTelephonyClass, GET_DATA + "Ext", 1);
                                    state = (int) mGetDataState.invoke(mTelephonyClass.getConstructor(android.content.Context.class).newInstance(context), i);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                if (state == TelephonyManager.DATA_CONNECTED
                                        || state == TelephonyManager.DATA_CONNECTING
                                        || state == TelephonyManager.DATA_SUSPENDED) {
                                    sim = i;
                                    out = "getDataStateExt " + sim;
                                    break;
                                }
                            }
                        }
                        if (sim == Constants.DISABLED) {
                            for (int i = 0; i < simQuantity; i++) {
                                int state = Constants.DISABLED;
                                try {
                                    if (mGetITelephony == null)
                                        mGetITelephony = getMethod(mTelephonyClass, "getITelephony", 1);
                                    Object mTelephonyStub = null;
                                    if (mGetITelephony != null) {
                                        mTelephonyStub = mGetITelephony.invoke(tm, i);
                                    }
                                    Class<?> mTelephonyStubClass = null;
                                    if (mTelephonyStub != null) {
                                        mTelephonyStubClass = Class.forName(mTelephonyStub.getClass().getName());
                                    }
                                    Class<?> mClass = null;
                                    if (mTelephonyStubClass != null) {
                                        mClass = mTelephonyStubClass.getDeclaringClass();
                                    }
                                    Method getState = null;
                                    if (mClass != null) {
                                        getState = mClass.getDeclaredMethod(GET_DATA);
                                    }
                                    if (getState != null) {
                                        state = (int) getState.invoke(mClass);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                if (state == TelephonyManager.DATA_CONNECTED
                                        || state == TelephonyManager.DATA_CONNECTING
                                        || state == TelephonyManager.DATA_SUSPENDED) {
                                    sim = i;
                                    out = "getITelephony " + sim;
                                    break;
                                }
                            }
                        }
                        if (sim == Constants.DISABLED) {
                            try {
                                if (mFrom == null)
                                    mFrom = getMethod(mTelephonyClass, "from", 2);
                                for (int i = 0; i < simQuantity; i++) {
                                    int state = Constants.DISABLED;
                                    final Object[] params = {context, i};
                                    TelephonyManager mTelephonyStub = null;
                                    if (mFrom != null) {
                                        mTelephonyStub = (TelephonyManager) mFrom.invoke(tm, params);
                                    }
                                    if (mTelephonyStub != null)
                                        state = mTelephonyStub.getDataState();
                                    if (state == TelephonyManager.DATA_CONNECTED
                                            || state == TelephonyManager.DATA_CONNECTING
                                            || state == TelephonyManager.DATA_SUSPENDED) {
                                        sim = i;
                                        out = "TelephonyManager.from " + sim;
                                        break;
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        if (sim == Constants.DISABLED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            try {
                                long id = Settings.Global.getLong(context.getContentResolver(), "multi_sim_data_call");
                                Class c = Class.forName(" android.telephony.SubscriptionManager");
                                Method m = getMethod(c, "getSlotId", 1);
                                if (m != null) {
                                    sim = (int) m.invoke(c.getConstructor(android.content.Context.class).newInstance(context), id);
                                }
                                out = "getFromSettingsGlobal " + sim;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } else
                sim = Constants.SIM1;
        }
        try {
            // to this path add a new directory path
            File dir = new File(String.valueOf(context.getFilesDir()));
            // create the file in which we will write the contents
            String fileName = "sim_log.txt";
            File file = new File(dir, fileName);
            FileOutputStream os = new FileOutputStream(file);
            os.write(out.getBytes());
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sim;
    }

    private static boolean getNetworkFromApnsFile(String code, String apn) {
        FileReader reader = null;
        boolean operatorFound = false;
        try {
            reader = new FileReader("/etc/apns-conf.xml");
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(reader);
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("apn")) {
                    HashMap<String, String> attributes = new HashMap<>();
                    for (int i = 0; i < xpp.getAttributeCount(); i++) {
                        attributes.put(xpp.getAttributeName(i), xpp.getAttributeValue(i));
                    }
                    if (attributes.containsKey("mcc") && attributes.containsKey("mnc") && code.equals(attributes.get("mcc")+attributes.get("mnc"))) {
                        if (!TextUtils.isEmpty(apn) && apn.equals(attributes.get("apn"))) {
                            operatorFound = true;
                            break;
                        }
                    }
                }
                eventType = xpp.next();
            }
        } catch (FileNotFoundException e) {
            return false;
        } catch (XmlPullParserException e) {
            return false;
        } catch (IOException e) {
            return false;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return operatorFound;
    }
}
