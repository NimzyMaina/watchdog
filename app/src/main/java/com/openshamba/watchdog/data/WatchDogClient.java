package com.openshamba.watchdog.data;

import com.openshamba.watchdog.data.responses.ApiResponse;


import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

/**
 * Created by Maina on 3/16/2018.
 */

public interface WatchDogClient {

    @FormUrlEncoded
    @POST("api/v1/calls")
    Call<ApiResponse> saveCall(@Body com.openshamba.watchdog.entities.Call call);

}
