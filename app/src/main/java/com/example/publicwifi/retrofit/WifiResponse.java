package com.example.publicwifi.retrofit;

import java.util.List;

public class WifiResponse {
    private List<WifiItem> wifi;

    public List<WifiItem> getWifi() {
        return wifi;
    }

    public void setWifi(List<WifiItem> wifi) {
        this.wifi = wifi;
    }
}
