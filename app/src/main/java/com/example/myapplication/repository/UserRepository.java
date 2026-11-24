package com.example.myapplication.repository;

import com.example.myapplication.model.User;

public interface UserRepository {
    User profile(long id);
    String displayName(long id);
    Credentials credentials(String email);
    long register(String name, String email, String phone, String passwordHash);
    void updateProfile(long id, String name, String phone);
    void updatePassword(long id, String passwordHash);

    final class Credentials {
        public final long userId;
        public final String hash;

        public Credentials(long userId, String hash) {
            this.userId = userId;
            this.hash = hash;
        }
    }
}
