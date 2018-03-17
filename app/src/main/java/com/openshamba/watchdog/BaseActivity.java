package com.openshamba.watchdog;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.openshamba.watchdog.utils.Notice;
import com.openshamba.watchdog.utils.SessionManager;

public abstract class BaseActivity extends AppCompatActivity {

    public SessionManager session;

    public CallbackManager callbackManager;

    private ProgressDialog pDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Instantiate FB SDK
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);
        session = new SessionManager(getApplicationContext());

        pDialog = new ProgressDialog(this);
        pDialog.setMessage("Please wait...");
        pDialog.setCancelable(false);

        // Create Callback Manager
        callbackManager = CallbackManager.Factory.create();
    }

    protected void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    protected void showSnackBar(String message,String key) {
        Notice.displayMessage(findViewById(android.R.id.content),message,key);
    }

    protected void showpDialog() {
        if (!pDialog.isShowing())
            pDialog.show();
    }

    protected void showpDialog(String message) {
        if (!pDialog.isShowing())
            pDialog.setMessage(message);
        pDialog.show();
    }

    protected void hidepDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }

    public void logoutUser(){
        // session wipe
        session.logout();
        // Facebook Logout
        LoginManager.getInstance().logOut();
        // Redirect User
        startActivity(new Intent(getApplicationContext(), LoginActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        );
        finish();
    }

    public void fbLogout()
    {
        LoginManager.getInstance().logOut();
    }

    public void checkAuth(){
        boolean loggedIn = AccessToken.getCurrentAccessToken() == null;
        if (!session.isLoggedIn() || loggedIn) {
            logoutUser();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }
}
