package com.example.myapplication.service;

import com.example.myapplication.model.User;
import com.example.myapplication.repository.SessionStore;
import com.example.myapplication.repository.UserRepository;
import com.example.myapplication.repository.UserRepository.Credentials;
import com.example.myapplication.utils.PasswordHasher;
import java.util.Locale;
import static com.example.myapplication.service.Validation.*;

public final class AuthService {
    private final UserRepository users;
    private final SessionStore sessions;
    private final PasswordHasher passwords;

    public AuthService(UserRepository users, SessionStore sessions, PasswordHasher passwords) {
        this.users = users;
        this.sessions = sessions;
        this.passwords = passwords;
    }

    public User current() {
        long id = sessions.read();
        return id > 0 ? users.profile(id) : null;
    }

    public User register(String name, String email, String phone, String password, String confirmation) {
        String validEmail = email(email);
        validatePassword(password, confirmation);
        long id = users.register(required(name, 80, "Nama lengkap"), validEmail, phone(phone),
                passwords.hash(password));
        require(id > 0, "Email sudah terdaftar.");
        sessions.save(id);
        return users.profile(id);
    }

    public User login(String email, String password) {
        require(!password.isEmpty() && password.length() <= 128, "Masukkan kata sandi yang valid.");
        Credentials credentials = users.credentials(email(email));
        require(credentials != null && passwords.matches(password, credentials.hash), "Email atau kata sandi salah.");
        sessions.save(credentials.userId);
        return users.profile(credentials.userId);
    }

    public void logout() { sessions.clear(); }

    public User edit(long id, String name, String phone) {
        users.updateProfile(id, required(name, 80, "Nama lengkap"), phone(phone));
        return users.profile(id);
    }

    public void changePassword(User user, String current, String password, String confirmation) {
        validatePassword(password, confirmation);
        Credentials credentials = users.credentials(user.email());
        require(credentials != null && current.length() <= 128 && passwords.matches(current, credentials.hash),
                "Kata sandi saat ini salah.");
        users.updatePassword(user.id(), passwords.hash(password));
    }

    private static String email(String value) {
        String email = required(value, 254, "Email kampus").toLowerCase(Locale.ROOT);
        require(email.matches("[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)+"), "Format email tidak valid.");
        return email;
    }

    private static String phone(String value) {
        String number = required(value, 25, "Nomor kontak");
        require(number.matches("\\+?[0-9 ().-]+") && number.replaceAll("\\D", "").length() >= 8,
                "Nomor kontak minimal 8 digit.");
        return number;
    }

    private static void validatePassword(String password, String confirmation) {
        require(password.length() >= 8 && password.length() <= 128, "Kata sandi harus 8–128 karakter.");
        require(password.equals(confirmation), "Ulangan kata sandi tidak cocok.");
    }
}
