package com.openshamba.watchdog.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.openshamba.watchdog.data.responses.LoginResponse;

public class SessionManager {
    // LogCat tag
    private static String TAG = SessionManager.class.getSimpleName();

    // Shared Preferences
    private SharedPreferences pref;

    private SharedPreferences.Editor editor;
    private Context _context;

    // Shared pref mode
    private int PRIVATE_MODE = 0;

    private static final String KEY_API_KEY = "api_token";
    private static final String KEY_UID = "id";
    private static final String KEY_FNAME = "first_name";
    private static final String KEY_LNAME = "last_name";


    // Shared preferences file name
    private static final String PREF_NAME = "Mpesa";

    private static final String KEY_IS_LOGGEDIN = "isLoggedIn";

    public SessionManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    public void logout(){
        editor.clear();
        editor.commit();
    }

    public void setLogin(boolean isLoggedIn, LoginResponse.User data) {

        editor.putBoolean(KEY_IS_LOGGEDIN, isLoggedIn);
        editor.putString(KEY_API_KEY,data.getApi_token());
        editor.putString(KEY_FNAME,data.getFirst_name());
        editor.putString(KEY_LNAME,data.getLast_name());

        // commit changes
        editor.commit();

        Log.d(TAG, "User login session modified!");
    }

    public boolean isLoggedIn(){
        return pref.getBoolean(KEY_IS_LOGGEDIN, false);
    }

    public String getKeyApiKey(){
        return pref.getString(KEY_API_KEY,"");
    }

    public String getKeyFname(){
        return pref.getString(KEY_FNAME,"");
    }

    public String getKeyLname(){
        return pref.getString(KEY_LNAME,"");
    }
}