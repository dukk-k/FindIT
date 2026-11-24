package com.example.myapplication.model;

import java.time.LocalDate;

public final class LostItem extends Item {
    public LostItem(long id, long reporterId, String name, Category category, String color,
                     String location, LocalDate date, String description, String photo, ReportStatus status) {
        super(id, reporterId, name, category, color, location, date, description, photo, status);
    }

    @Override public Type type() { return Type.LOST; }
    @Override public boolean supportsClaims() { return false; }
}
