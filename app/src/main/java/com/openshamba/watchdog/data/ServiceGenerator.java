package com.openshamba.watchdog.data;

import android.text.TextUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ServiceGenerator {
    private static final String API_BASE_URL = "";
    private static Retrofit retrofit;

    private static OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(40, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS);

    private static Retrofit.Builder builder = new Retrofit.Builder()
                    .baseUrl(API_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create());

    private static HttpLoggingInterceptor logging = new HttpLoggingInterceptor()
                    .setLevel(HttpLoggingInterceptor.Level.BODY);

    private static Interceptor mCache = new Interceptor() {
        @Override public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            request.newBuilder().addHeader("Cache-Control", "no-cache");
            return chain.proceed(request);
        }
    };

    public static <S> S createService(Class<S> serviceClass) {
            if (!httpClient.interceptors().contains(logging)) {
                httpClient.addInterceptor(logging);
                httpClient.addInterceptor(mCache);
                builder.client(httpClient.build());
                retrofit = builder.build();
            }

            return retrofit.create(serviceClass);
    }

    public static <S> S createService(Class<S> serviceClass, final String authToken) {
        if (!TextUtils.isEmpty(authToken)) {
            AuthenticationInterceptor interceptor = new AuthenticationInterceptor(authToken);

            httpClient.addInterceptor(mCache);

            if (!httpClient.interceptors().contains(interceptor)) {
                httpClient.addInterceptor(interceptor);
            }

            if(!httpClient.interceptors().contains(logging)){
                httpClient.addInterceptor(logging);
            }

            builder.client(httpClient.build());
            retrofit = builder.build();
        }

        return retrofit.create(serviceClass);
    }

    public static Retrofit retrofit() {
        OkHttpClient client = httpClient.build();
        return builder.client(client).build();
    }

    public static WatchDogClient getClient () {
        return  createService(WatchDogClient.class);
    }

    public static WatchDogClient getClient (String auth) {
        return  createService(WatchDogClient.class,auth);
    }
}
