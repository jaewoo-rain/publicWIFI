package com.example.publicwifi.model;

public class WifiData {
    public int id;
    public String name;
    public String description;
    public double latitude;
    public double longitude;
    public String password;

    public WifiData(int id, String name, String description, double latitude, double longitude, String password) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
