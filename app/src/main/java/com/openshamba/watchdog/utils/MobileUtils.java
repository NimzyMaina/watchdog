package com.openshamba.watchdog.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Maina on 3/13/2018.
 */

public class MobileUtils {

    private static final String MEDIATEK = "com.mediatek.telephony.TelephonyManagerEx";
    private static final String GET_CALL = "getCallState";
    private static final String GET_SUBID = "getSubIdBySlot";
    private static final String GET_IMEI = "getDeviceId";

    private static Class<?> mTelephonyClass = null;

    private static Method mGetCallState = null;
    private static Method mGetSubIdBySlot = null;
    private static Method mGetITelephony = null;
    private static Method mFrom = null;
    private static Method mGetDeviceId = null;

    private static ArrayList<Long> mSubIds = null;

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
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
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

    private static Method getMethod (Class c, String name, int params) {
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

}
