package com.octania.marketplace.data.model.response;

import com.google.gson.annotations.SerializedName;
import com.octania.marketplace.data.model.ad.AdBanner;

import java.util.List;

public class AdBannerResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private List<AdBanner> data;

    public boolean isSuccess() {
        return success;
    }

    public List<AdBanner> getData() {
        return data;
    }
}
