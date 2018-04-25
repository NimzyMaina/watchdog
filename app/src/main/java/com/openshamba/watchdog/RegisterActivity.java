package com.openshamba.watchdog;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.openshamba.watchdog.data.ServiceGenerator;
import com.openshamba.watchdog.data.responses.ApiResponse;
import com.openshamba.watchdog.utils.ErrorHandler;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends BaseActivity {

    EditText username,password,password_confirm;
    Button register;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        username = (EditText) findViewById(R.id.edit_username);
        password = (EditText) findViewById(R.id.edit_password);
        password_confirm = (EditText) findViewById(R.id.edit_password_confirm);

        register = (Button) findViewById(R.id.register_button);

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username_s,password_s,password_confirm_s;
                username_s = username.getText().toString();
                password_s  = password.getText().toString();
                password_confirm_s = password_confirm.getText().toString();

                if(username_s.isEmpty() || password_s.isEmpty() || password_confirm_s.isEmpty()){
                    showSnackBar("Fill in your details","error");
                    return;
                }

                if(!password_s.equals(password_confirm_s)){
                    showSnackBar("Passwords do not match","error");
                    return;
                }

                register(username_s,password_s,password_confirm_s);
            }
        });
    }

    protected  void register(String username,String password,String password_confirm){
        showpDialog();
        Call<ApiResponse> call = ServiceGenerator.getClient().register(username,password,password_confirm);

        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                hidepDialog();
                if(response.isSuccessful()){
                    showSnackBar(response.body().getMessage(),"success");
//                    startActivity(new Intent(RegisterActivity.this,LoginActivity.class));
//                    finish();
                }else{
                    ApiResponse error = ErrorHandler.parseError(response);
                    showSnackBar(error.getMessage(),"error");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                hidepDialog();
                showSnackBar("Please check your internet connection","error");
            }
        });
    }
}
