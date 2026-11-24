package com.example.myapplication.model;

public final class StatusHistory {
    private final ReportStatus status;
    private final long time;

    public StatusHistory(ReportStatus status, long time) {
        this.status = status;
        this.time = time;
    }

    public ReportStatus status() { return status; }
    public long time() { return time; }
}
