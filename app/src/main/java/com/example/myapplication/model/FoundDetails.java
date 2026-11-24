package com.example.myapplication.model;

public final class FoundDetails {
    private final String brand;
    private final String markers;

    public FoundDetails(String brand, String markers) {
        this.brand = brand;
        this.markers = markers;
    }

    public String brand() { return brand; }
    public String markers() { return markers; }
}
