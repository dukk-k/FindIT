package com.example.myapplication.ui;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;

import com.example.myapplication.MainActivity;
import com.example.myapplication.MainActivity.Page;
import com.example.myapplication.R;

public final class AuthScreens extends Screen {
    public AuthScreens(MainActivity host) { super(host); }

    public void login() {
        View root = host.inflateScreen(R.layout.screen_login, "", false, false, false);
        markLink(button(root, R.id.switch_auth, v -> host.go(Page.REGISTER)), R.string.register);
        button(root, R.id.submit, v -> {
            String email = value(root, R.id.email);
            String password = value(root, R.id.password);
            host.submit(v, () -> host.services().auth.login(email, password), host::signedIn);
        });
    }

    public void register() {
        View root = host.inflateScreen(R.layout.screen_register, "Daftar Akun", true, false, false);
        markLink(button(root, R.id.switch_auth, v -> host.go(Page.LOGIN)), R.string.login);
        button(root, R.id.submit, v -> {
            String name = value(root, R.id.name);
            String email = value(root, R.id.email);
            String phone = value(root, R.id.phone);
            String password = value(root, R.id.password);
            String confirmation = value(root, R.id.confirm);
            host.submit(v, () -> host.services().auth.register(name, email, phone, password, confirmation),
                    host::signedIn);
        });
    }
    private void markLink(Button button, int action) {
        SpannableString copy = new SpannableString(button.getText());
        int start = copy.toString().lastIndexOf(host.getString(action));
        if (start >= 0) copy.setSpan(new ForegroundColorSpan(host.getColor(R.color.teal)), start,
                copy.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        button.setText(copy);
    }
}
