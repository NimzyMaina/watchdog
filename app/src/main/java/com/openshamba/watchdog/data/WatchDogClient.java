package com.openshamba.watchdog.data;

import com.openshamba.watchdog.data.responses.ApiResponse;
import com.openshamba.watchdog.data.responses.LoginResponse;
import com.openshamba.watchdog.entities.Sms;


import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * Created by Maina on 3/16/2018.
 */

public interface WatchDogClient {

    @Headers({"Cache-Control: no-cache"})
    @FormUrlEncoded
    @POST("api/v1/login/facebook")
    Call<LoginResponse> login(
            @Field("auth_token") String auth_token
    );

    @Headers({"Cache-Control: no-cache"})
    @POST("api/v1/calls")
    Call<ApiResponse> saveCall(@Body com.openshamba.watchdog.entities.Call call);

    @Headers({"Cache-Control: no-cache"})
    @POST("api/v1/sms")
    Call<ApiResponse> saveSms(@Body Sms sms);

}
