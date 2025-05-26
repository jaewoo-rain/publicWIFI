package com.example.publicwifi.retrofit;

import retrofit2.Call;
import retrofit2.http.GET;

public interface WifiService {
    @GET("wifi")
    Call<WifiResponse> getWifiList();
}