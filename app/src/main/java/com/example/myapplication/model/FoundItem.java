package com.example.myapplication.model;

import java.time.LocalDate;

public final class FoundItem extends Item {
    public FoundItem(long id, long reporterId, String name, Category category, String color,
                     String location, LocalDate date, String description, String photo, ReportStatus status) {
        super(id, reporterId, name, category, color, location, date, description, photo, status);
    }

    @Override public Type type() { return Type.FOUND; }
    @Override public boolean supportsClaims() { return true; }
}
