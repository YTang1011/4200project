package com.example.a4200project;

public class PlaceInfo {
    private final String name;
    private final String address;
    private final String placeId;
    private String openingHours;

    public PlaceInfo(String name, String address, String placeId) {
        this.name = name;
        this.address = address;
        this.placeId = placeId;
    }

    // Getter methods
    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getPlaceId() {
        return placeId;
    }

    public String getOpeningHours() {
        return openingHours;
    }

    // Setter for opening hours
    public void setOpeningHours(String openingHours) {
        this.openingHours = openingHours;
    }
}