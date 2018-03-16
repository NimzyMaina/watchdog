package com.openshamba.watchdog.data.responses;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiResponse {
    private int status_code;

    private String message;

    private Map<String, List<String>> errors = new HashMap<>();

    public ApiResponse() {
    }

    public int getStatus_code() {
        return status_code;
    }

    public void setStatus_code(int status_code) {
        this.status_code = status_code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, List<String>> getErrors() {
        return errors;
    }

    public void setErrors(Map<String, List<String>> errors) {
        this.errors = errors;
    }
}