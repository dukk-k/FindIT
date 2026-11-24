package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.room.Room;
import com.example.myapplication.database.FindItDatabase;
import com.example.myapplication.model.Item;
import com.example.myapplication.repository.SessionStore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.annotation.SQLiteMode;
import org.robolectric.shadows.ShadowDialog;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "w390dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
public final class FindItUiFlowTest {
    private static final String FINDER = "penemu@kampus.test";
    private static final String OWNER = "pemilik@kampus.test";
    private static final String PASSWORD = "sandi-aman-2026";
    private FindItApplication application;
    private MainActivity activity;

    @Test public void bothAccountsCanCompleteHandoverFromTheScreens() throws Exception {
        FindItDatabase database = connectTestDatabase();
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            activity = controller.get();
            assertEquals(View.VISIBLE, activity.findViewById(R.id.splash).getVisibility());
            capture("pembuka");
            waitForScreen();
            capture("masuk");
            verifyKeyboardInsets();
            click(R.id.switch_auth);
            capture("daftar-akun");
            activity.getOnBackPressedDispatcher().onBackPressed();
            register("Penemu Kampus", FINDER);
            capture("daftar-barang");
            click(R.id.nav_report);
            click(R.id.found);
            capture("lapor-ditemukan");
            enter(R.id.name, "Earbuds nirkabel");
            click(R.id.category);
            AlertDialog categories = lastDialog();
            categories.getListView().performItemClick(new View(activity), 0, 0);
            enter(R.id.color, "Hitam");
            enter(R.id.location, "Perpustakaan");
            enter(R.id.markers, "Stiker biru di dalam");
            click(R.id.submit);
            waitForScreen();
            assertEquals("Daftar Barang", displayed(R.id.title));

            logout();
            register("Pemilik Barang", OWNER);
            openFirstRow();
            assertFalse(displayed(R.id.detail_note).contains("Stiker biru"));
            enter(R.id.proof, "Kotaknya memiliki stiker biru di dalam");
            click(R.id.submit);
            waitForScreen();
            assertEquals("Menunggu Klaim", displayed(R.id.item_status));

            logout();
            login(FINDER);
            incomingClaim();
            assertTrue(displayed(R.id.secret).contains("Stiker biru"));
            CheckBox verify = activity.findViewById(R.id.verify_proof);
            verify.setChecked(true);
            click(R.id.approve);
            confirmDialog();
            waitForScreen();
            assertEquals("Klaim Disetujui", displayed(R.id.item_status));
            assertNull(activity.findViewById(R.id.pickup_code));

            logout();
            login(OWNER);
            click(R.id.nav_claims);
            click(R.id.mine);
            waitForScreen();
            capture("klaim-disetujui");
            String code = displayed(R.id.pickup_code);
            assertEquals(6, code.length());

            logout();
            login(FINDER);
            incomingClaim();
            enter(R.id.pickup_entry, code);
            click(R.id.handover);
            waitForScreen();
            assertEquals("Sudah Dikembalikan", displayed(R.id.item_status));

            logout();
            login(OWNER);
            click(R.id.nav_claims);
            click(R.id.mine);
            waitForScreen();
            assertEquals("Sudah Dikembalikan", displayed(R.id.item_status));
            assertNull(activity.findViewById(R.id.pickup_code));
            logout();
        } finally {
            database.close();
        }
    }

    @Test public void lookupAndActivityRecreationKeepReportDraft() throws Exception {
        FindItDatabase database = connectTestDatabase();
        try {
            application.worker.submit(() -> {
                long user = application.auth.register("Penemu Uji", FINDER, "08123456789", PASSWORD, PASSWORD).id();
                application.items.report(user, Item.Type.FOUND, "Earbuds lama", Item.Category.ELECTRONICS,
                        "Hitam", "Perpustakaan", LocalDate.now(), "", "", "Merek", "Stiker lama");
            }).get(15, TimeUnit.SECONDS);
            try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
                activity = controller.get();
                waitForScreen();
                assertEquals("Daftar Barang", displayed(R.id.title));
                click(R.id.nav_report);
                click(R.id.found);
                enter(R.id.name, "Draf baru");
                enter(R.id.location, "Laboratorium");
                enter(R.id.markers, "Nomor seri rahasia");
                click(R.id.category);
                chooseFirstOption();
                String date = displayed(R.id.date);

                click(R.id.photo);
                Intent pick = shadowOf(activity).getNextStartedActivity();
                assertEquals(Intent.ACTION_OPEN_DOCUMENT, pick.getAction());
                Uri uri = Uri.parse("content://findit.test/draft-photo");
                shadowOf(activity).receiveResult(pick, Activity.RESULT_OK, new Intent().setData(uri));
                assertEquals("Foto dipilih · Ganti foto", displayed(R.id.photo));

                click(R.id.report_search);
                enter(R.id.search, "Earbuds");
                click(R.id.found);
                waitForScreen();
                assertEquals("Cari Barang", displayed(R.id.title));
                openFirstRow();
                assertEquals("Earbuds lama", displayed(R.id.item_name));
                click(R.id.back);
                waitForScreen();
                assertEquals("Earbuds", displayed(R.id.search));
                click(R.id.back);

                click(R.id.report_filter);
                AlertDialog menu = lastDialog();
                assertTrue(menu.isShowing());
                chooseFirstOption();
                lastDialog().getListView().performItemClick(new View(activity), 1, 1);
                assertEquals("Filter aktif", displayed(R.id.filter));

                controller.recreate();
                activity = controller.get();
                waitForScreen();
                assertEquals("Cari Barang", displayed(R.id.title));
                assertEquals("Filter aktif", displayed(R.id.filter));
                click(R.id.back);
                assertEquals("Lapor Barang", displayed(R.id.title));
                assertEquals("Draf baru", displayed(R.id.name));
                assertEquals("Laboratorium", displayed(R.id.location));
                assertEquals("Nomor seri rahasia", displayed(R.id.markers));
                assertEquals("Elektronik", displayed(R.id.category));
                assertEquals(date, displayed(R.id.date));
                assertEquals("Foto dipilih · Ganti foto", displayed(R.id.photo));
                assertTrue(activity.findViewById(R.id.found).isSelected());
            }
        } finally {
            database.close();
        }
    }

    private FindItDatabase connectTestDatabase() {
        application = (FindItApplication) RuntimeEnvironment.getApplication();
        FindItDatabase database = Room.inMemoryDatabaseBuilder(application, FindItDatabase.class).build();
        application.connect(database, new SessionStore() {
            private long account;
            @Override public long read() { return account; }
            @Override public void save(long userId) { account = userId; }
            @Override public void clear() { account = 0; }
        });
        return database;
    }

    private void chooseFirstOption() {
        lastDialog().getListView().performItemClick(new View(activity), 0, 0);
    }

    private void register(String name, String email) throws Exception {
        click(R.id.switch_auth);
        enter(R.id.name, name);
        enter(R.id.email, email);
        enter(R.id.phone, "08123456789");
        enter(R.id.password, PASSWORD);
        enter(R.id.confirm, PASSWORD);
        click(R.id.submit);
        waitForScreen();
        assertEquals("Daftar Barang", displayed(R.id.title));
    }

    private void login(String email) throws Exception {
        enter(R.id.email, email);
        enter(R.id.password, PASSWORD);
        click(R.id.submit);
        waitForScreen();
        assertEquals("Daftar Barang", displayed(R.id.title));
    }

    private void logout() throws Exception {
        click(R.id.nav_account);
        click(R.id.logout);
        confirmDialog();
        waitForScreen();
        assertEquals("FindIt Campus", activity.findViewById(R.id.wordmark).getContentDescription().toString());
    }

    private void incomingClaim() throws Exception {
        click(R.id.nav_claims);
        click(R.id.incoming);
        waitForScreen();
        openFirstRow();
    }

    private void openFirstRow() throws Exception {
        LinearLayout results = activity.findViewById(R.id.results);
        assertNotNull(results.getChildAt(0));
        assertTrue(results.getChildAt(0).performClick());
        waitForScreen();
    }

    private AlertDialog lastDialog() {
        return (AlertDialog) ShadowDialog.getLatestDialog();
    }

    private void confirmDialog() {
        assertTrue(lastDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick());
        shadowOf(Looper.getMainLooper()).idle();
    }

    private void click(int id) {
        View view = activity.findViewById(id);
        assertNotNull("Missing view " + activity.getResources().getResourceEntryName(id), view);
        assertTrue(view.performClick());
    }

    private void enter(int id, String value) {
        EditText field = activity.findViewById(id);
        assertNotNull(field);
        field.setText(value);
    }

    private String displayed(int id) {
        TextView view = activity.findViewById(id);
        assertNotNull("Missing text " + activity.getResources().getResourceEntryName(id), view);
        return view.getText().toString();
    }

    private void verifyKeyboardInsets() {
        View root = activity.findViewById(R.id.main);
        WindowInsetsCompat original = ViewCompat.getRootWindowInsets(root);
        WindowInsetsCompat.Builder builder = original == null
                ? new WindowInsetsCompat.Builder() : new WindowInsetsCompat.Builder(original);
        int ime = WindowInsetsCompat.Type.ime();
        WindowInsetsCompat keyboard = builder.setInsets(ime, Insets.of(0, 0, 0, 288))
                .setVisible(ime, true).build();
        ViewCompat.dispatchApplyWindowInsets(root, keyboard);
        assertEquals(288, root.getPaddingBottom());
        WindowInsetsCompat closed = original == null ? new WindowInsetsCompat.Builder().build() : original;
        ViewCompat.dispatchApplyWindowInsets(root, closed);
    }

    private void capture(String name) throws IOException {
        File directory = new File("build/reports/findit-ui");
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Unable to create layout screenshot directory");
        }
        View screen = activity.findViewById(R.id.main);
        screen.measure(View.MeasureSpec.makeMeasureSpec(390, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY));
        screen.layout(0, 0, 390, 800);
        Bitmap bitmap = Bitmap.createBitmap(390, 800, Bitmap.Config.ARGB_8888);
        try (FileOutputStream output = new FileOutputStream(new File(directory, name + ".png"))) {
            screen.draw(new Canvas(bitmap));
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output));
        } finally {
            bitmap.recycle();
        }
    }

    private void waitForScreen() throws Exception {
        for (int i = 0; i < 3; i++) {
            application.worker.submit(() -> {}).get(15, TimeUnit.SECONDS);
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(450));
        }
    }
}
