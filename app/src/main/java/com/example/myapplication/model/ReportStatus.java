package com.example.myapplication.model;

public enum ReportStatus {
    LOST("Hilang"), FOUND("Ditemukan"), PENDING("Menunggu Klaim"),
    APPROVED("Klaim Disetujui"), PICKUP("Menunggu Pengambilan"), RETURNED("Sudah Dikembalikan");

    public final String label;

    ReportStatus(String label) {
        this.label = label;
    }
}
