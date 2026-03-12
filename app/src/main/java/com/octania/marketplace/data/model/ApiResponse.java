package com.octania.marketplace.data.model;

public class ApiResponse<T> {
    private String status;
    private String message;
    private T data;
    private java.util.Map<String, Integer> counts;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public java.util.Map<String, Integer> getCounts() {
        return counts;
    }

    public void setCounts(java.util.Map<String, Integer> counts) {
        this.counts = counts;
    }
}
