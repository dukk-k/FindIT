package com.example.myapplication.service;

final class Validation {
    private Validation() {}

    static void require(boolean valid, String message) {
        if (!valid) throw new IllegalArgumentException(message);
    }

    static String text(String value, int limit, String label) {
        String clean = value == null ? "" : value.trim();
        require(clean.length() <= limit, label + " terlalu panjang.");
        return clean;
    }

    static String required(String value, int limit, String label) {
        String clean = text(value, limit, label);
        require(!clean.isEmpty(), label + " wajib diisi.");
        return clean;
    }
}
