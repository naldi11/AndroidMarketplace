package com.octania.marketplace.data.remote;

import android.content.Context;
import android.content.Intent;

import com.octania.marketplace.utils.SessionManager;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    public static final String BASE_URL = "http://192.168.1.4:8000/api/";
    //public static final String BASE_URL = "http://172.20.10.10:8000/api/";

    private static Retrofit retrofit = null;
    private static Context appContext = null;

    /** Panggil sekali di Application.onCreate() atau sebelum getClient() pertama kali. */
    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .addInterceptor(chain -> {
                        okhttp3.Request request = chain.request().newBuilder()
                                .addHeader("Accept", "application/json")
                                .build();
                        okhttp3.Response response = chain.proceed(request);

                        // Auto-logout jika token tidak valid / kadaluarsa
                        if (response.code() == 401 && appContext != null) {
                            SessionManager session = new SessionManager(appContext);
                            session.logoutUser();

                            Intent intent = new Intent(appContext,
                                    com.octania.marketplace.ui.auth.LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            appContext.startActivity(intent);
                        }

                        return response;
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
