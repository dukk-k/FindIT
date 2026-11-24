package com.example.myapplication.ui;

import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import com.example.myapplication.MainActivity;
import com.example.myapplication.MainActivity.Page;
import com.example.myapplication.R;
import com.example.myapplication.model.User;

public final class AccountScreens extends Screen {
    public AccountScreens(MainActivity host) { super(host); }

    public void account() {
        View root = host.inflateScreen(R.layout.screen_account, "Akun", false, true, false);
        User user = host.user();
        text(root, R.id.name, user.name());
        text(root, R.id.contact, user.email() + "\n" + user.phone());
        button(root, R.id.edit_profile, v -> host.go(Page.PROFILE));
        button(root, R.id.change_password, v -> host.go(Page.PASSWORD));
        button(root, R.id.view_history, v -> host.go(Page.HISTORY));
        button(root, R.id.logout, v -> new AlertDialog.Builder(host).setTitle("Keluar dari akun?")
                .setNegativeButton("Batal", null).setPositiveButton("Keluar", (dialog, which) ->
                        host.submit(v, () -> {
                            host.services().auth.logout();
                            return null;
                        }, ignored -> host.signedOut())).show());
    }

    public void editProfile() {
        View root = host.inflateScreen(R.layout.screen_edit, "Ubah Profil", true, false, false);
        LinearLayout fields = root.findViewById(R.id.fields);
        User account = host.user();
        EditText name = field(fields, "Nama lengkap", account.name(), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        EditText phone = field(fields, "Nomor kontak", account.phone(), InputType.TYPE_CLASS_PHONE);
        button(root, R.id.submit, v -> {
            String newName = name.getText().toString();
            String newPhone = phone.getText().toString();
            host.submit(v, () -> host.services().auth.edit(account.id(), newName, newPhone), updated -> {
                host.updateUser(updated);
                host.toast("Profil disimpan.");
                host.go(Page.ACCOUNT);
            });
        });
    }

    public void changePassword() {
        View root = host.inflateScreen(R.layout.screen_edit, "Ubah Kata Sandi", true, false, false);
        LinearLayout fields = root.findViewById(R.id.fields);
        int passwordType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
        EditText previous = field(fields, "Kata sandi saat ini", "", passwordType);
        EditText next = field(fields, "Kata sandi baru", "", passwordType);
        EditText confirmation = field(fields, "Ulangi kata sandi baru", "", passwordType);
        User account = host.user();
        button(root, R.id.submit, v -> {
            String current = previous.getText().toString();
            String password = next.getText().toString();
            String repeated = confirmation.getText().toString();
            host.submit(v, () -> {
                host.services().auth.changePassword(account, current, password, repeated);
                return null;
            }, ignored -> {
                host.toast("Kata sandi diubah.");
                host.go(Page.ACCOUNT);
            });
        });
    }

    private EditText field(LinearLayout parent, String hint, String value, int inputType) {
        EditText field = (EditText) host.getLayoutInflater().inflate(R.layout.field_input, parent, false);
        field.setInputType(inputType);
        field.setHint(hint);
        field.setText(value);
        parent.addView(field);
        return field;
    }
}
