package com.example.a4200project;

public class PlaceInfo {
    private final String name;
    private final String address;
    private String openingHours;

    public PlaceInfo(String name, String address) {
        this.name = name;
        this.address = address;
        this.openingHours = null;
    }

    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getOpeningHours() { return openingHours; }
    public void setOpeningHours(String openingHours) { this.openingHours = openingHours; }
}