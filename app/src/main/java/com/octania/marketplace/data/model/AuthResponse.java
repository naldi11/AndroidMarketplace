package com.octania.marketplace.data.model;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {
    private String status;
    private String message;
    private User data;

    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("token_type")
    private String tokenType;

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public User getData() {
        return data;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }
}
