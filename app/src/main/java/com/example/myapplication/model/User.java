package com.example.myapplication.model;

public final class User {
    private final long id;
    private final String name;
    private final String email;
    private final String phone;

    public User(long id, String name, String email, String phone) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    public long id() { return id; }
    public String name() { return name; }
    public String email() { return email; }
    public String phone() { return phone; }
}
