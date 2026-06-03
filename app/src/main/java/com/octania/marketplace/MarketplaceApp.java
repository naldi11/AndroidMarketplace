package com.octania.marketplace;

import android.app.Application;
import com.octania.marketplace.data.remote.ApiClient;

public class MarketplaceApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ApiClient.init(this);
    }
}
