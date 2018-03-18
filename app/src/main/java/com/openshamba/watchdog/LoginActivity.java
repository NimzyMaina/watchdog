package com.openshamba.watchdog;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.openshamba.watchdog.data.ServiceGenerator;
import com.openshamba.watchdog.data.responses.ApiResponse;
import com.openshamba.watchdog.data.responses.LoginResponse;
import com.openshamba.watchdog.utils.ErrorHandler;

import java.util.List;

import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends BaseActivity {

    private LoginButton loginButton;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        requestPerm();

        if(session.isLoggedIn()){
            startActivity(new Intent(LoginActivity.this,MainActivity.class));
        }

        // Find fb button
        loginButton = (LoginButton)findViewById(R.id.login_button);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginButton.setVisibility(View.INVISIBLE);
            }
        });

        // assign callback to button
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                login(loginResult.getAccessToken().getToken());
                //loginButton.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCancel() {
                loginButton.setVisibility(View.VISIBLE);
                showSnackBar("Login attempt canceled.","error");
            }

            @Override
            public void onError(FacebookException e) {
                loginButton.setVisibility(View.VISIBLE);
                showSnackBar("Login attempt Failed","error");
            }
        });
    }

    private void login(String auth_token) {

        showpDialog("Hang on! Just one more step...");

        retrofit2.Call<LoginResponse> call = ServiceGenerator.getClient().login(
                auth_token
        );

        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(retrofit2.Call<LoginResponse> call, Response<LoginResponse> response) {
                hidepDialog();
                if(response.isSuccessful()) {
                    session.setLogin(true,response.body().getData());
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                }else {
                    loginButton.setVisibility(View.VISIBLE);
                    fbLogout();
                    ApiResponse error = ErrorHandler.parseError(response);
                    showSnackBar(error.getMessage(),"error");
                }
            }

            @Override
            public void onFailure(retrofit2.Call<LoginResponse> call, Throwable t) {
                loginButton.setVisibility(View.VISIBLE);
                hidepDialog();
                fbLogout();
                showSnackBar("Please check your internet connection","error");
            }
        });

    }

    public void requestPerm(){
        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.READ_CALL_LOG,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.PROCESS_OUTGOING_CALLS,
                        Manifest.permission.READ_SMS,
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.RECEIVE_BOOT_COMPLETED)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        // check if all permissions are granted
                        if (report.areAllPermissionsGranted()) {
                            //Toast.makeText(getApplicationContext(), "All permissions are granted!", Toast.LENGTH_SHORT).show();
                        }

                        // check for permanent denial of any permission
                        if (report.isAnyPermissionPermanentlyDenied()) {
                            // show alert dialog navigating to Settings
                            showSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).
                withErrorListener(new PermissionRequestErrorListener() {
                    @Override
                    public void onError(DexterError error) {
                        Toast.makeText(getApplicationContext(), "Error occurred! ", Toast.LENGTH_SHORT).show();
                    }
                })
                .onSameThread()
                .check();

    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
        builder.setTitle("Need Permissions");
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.");
        builder.setPositiveButton("GOTO SETTINGS", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                openSettings();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    // navigating user to app settings
    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 101);
    }
}
