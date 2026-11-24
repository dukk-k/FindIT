package com.example.myapplication.repository;

public interface SessionStore {
    long read();
    void save(long userId);
    void clear();
}
