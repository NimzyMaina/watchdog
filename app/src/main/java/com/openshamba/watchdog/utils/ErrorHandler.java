package com.openshamba.watchdog.utils;

import com.openshamba.watchdog.data.ServiceGenerator;
import com.openshamba.watchdog.data.responses.ApiResponse;

import java.io.IOException;
import java.lang.annotation.Annotation;

import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Response;

public class ErrorHandler {
    public static ApiResponse parseError(Response<?> response) {
        Converter<ResponseBody, ApiResponse> converter = ServiceGenerator.retrofit().responseBodyConverter(ApiResponse.class, new Annotation[0]);

        ApiResponse error;

        try {
            error = converter.convert(response.errorBody());
        } catch (IOException e) {
            return new ApiResponse();
        }

        return error;
    }
}