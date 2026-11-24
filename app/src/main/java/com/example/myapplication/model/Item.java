package com.example.myapplication.model;

import java.time.LocalDate;

public abstract class Item {
    public enum Type {
        LOST("Hilang"), FOUND("Ditemukan");

        public final String label;

        Type(String label) {
            this.label = label;
        }
    }

    public enum Category {
        ELECTRONICS("Elektronik"), ACCESSORIES("Aksesori"), DOCUMENTS("Dokumen"),
        CLOTHING("Pakaian"), SUPPLIES("Perlengkapan"), OTHER("Lainnya");

        public final String label;

        Category(String label) {
            this.label = label;
        }
    }

    private final long id;
    private final long reporterId;
    private final String name;
    private final Category category;
    private final String color;
    private final String location;
    private final LocalDate date;
    private final String description;
    private final String photo;
    private final ReportStatus status;

    protected Item(long id, long reporterId, String name, Category category, String color,
                   String location, LocalDate date, String description, String photo, ReportStatus status) {
        this.id = id;
        this.reporterId = reporterId;
        this.name = name;
        this.category = category;
        this.color = color;
        this.location = location;
        this.date = date;
        this.description = description;
        this.photo = photo;
        this.status = status;
    }

    public long id() { return id; }
    public long reporterId() { return reporterId; }
    public String name() { return name; }
    public Category category() { return category; }
    public String color() { return color; }
    public String location() { return location; }
    public LocalDate date() { return date; }
    public String description() { return description; }
    public String photo() { return photo; }
    public ReportStatus status() { return status; }
    public abstract Type type();
    public abstract boolean supportsClaims();
}
