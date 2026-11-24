package com.example.myapplication;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.os.BundleCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.model.User;
import com.example.myapplication.ui.*;
import com.example.myapplication.utils.PhotoStore;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

public final class MainActivity extends AppCompatActivity {
    public enum Page { LOGIN, REGISTER, HOME, REPORT, REPORT_LOOKUP, DETAIL, CLAIMS, CLAIM, HISTORY, ACCOUNT, PROFILE, PASSWORD }

    private FindItApplication services;
    private PhotoStore photos;
    private User user;
    private Page page;
    private Page returnPage = Page.HOME;
    private long recordId;
    private int revision;
    private boolean saving;
    private int homeScroll;
    private FrameLayout holder;
    private ScrollView scroll;
    private AuthScreens authScreens;
    private ReportScreens reportScreens;
    private ClaimScreens claimScreens;
    private AccountScreens accountScreens;
    private Uri selectedPhoto;
    private Uri reportPhoto;
    private Boolean launchLookupWithFilter;
    private Uri pendingPhoto;
    private final ActivityResultLauncher<String[]> picker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), this::photoSelected);

    @Override protected void onCreate(Bundle saved) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(saved);
        if (Build.VERSION.SDK_INT >= 33) setRecentsScreenshotEnabled(false);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_main);
        View root = findViewById(R.id.main);
        View splash = findViewById(R.id.splash);
        long splashUntil = SystemClock.uptimeMillis() + (saved == null ? 380 : 0);
        splash.setVisibility(saved == null ? View.VISIBLE : View.GONE);
        root.setBackgroundColor(getColor(saved == null ? R.color.teal : R.color.paper));
        WindowCompat.getInsetsController(getWindow(), root).setAppearanceLightNavigationBars(saved != null);
        setStatusSurface(saved == null);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            int types = WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
                    | WindowInsetsCompat.Type.ime();
            Insets bars = insets.getInsets(types);
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            View scrim = findViewById(R.id.status_scrim);
            if (scrim.getLayoutParams().height != bars.top) {
                scrim.getLayoutParams().height = bars.top;
                scrim.requestLayout();
            }
            scrim.setTranslationY(-bars.top);
            return insets;
        });
        services = (FindItApplication) getApplication();
        photos = new PhotoStore(this);
        holder = findViewById(R.id.screen_holder);
        scroll = findViewById(R.id.scroll);
        authScreens = new AuthScreens(this);
        reportScreens = new ReportScreens(this);
        claimScreens = new ClaimScreens(this);
        accountScreens = new AccountScreens(this);
        if (saved != null) {
            returnPage = Page.valueOf(saved.getString("returnPage", Page.HOME.name()));
            homeScroll = saved.getInt("homeScroll");
            reportScreens.restore(saved);
            reportPhoto = BundleCompat.getParcelable(saved, "reportPhoto", Uri.class);
            claimScreens.restore(saved);
        }
        findViewById(R.id.back).setOnClickListener(view -> back());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { back(); }
        });
        int[] navIds = {R.id.nav_home, R.id.nav_report, R.id.nav_claims, R.id.nav_history, R.id.nav_account};
        Page[] tabs = {Page.HOME, Page.REPORT, Page.CLAIMS, Page.HISTORY, Page.ACCOUNT};
        for (int i = 0; i < tabs.length; i++) {
            Page destination = tabs[i];
            findViewById(navIds[i]).setOnClickListener(view -> go(destination));
        }

        run(services.auth::current, account -> {
            user = account;
            Page initial = account == null ? Page.LOGIN : Page.HOME;
            if (saved != null && saved.getString("page") != null) {
                Page previous = Page.valueOf(saved.getString("page"));
                boolean authPage = previous == Page.LOGIN || previous == Page.REGISTER;
                if ((account == null) == authPage) initial = previous;
                else if (account == null) {
                    reportScreens.resetSearch();
                    reportPhoto = null;
                }
            }
            Page destination = initial;
            finishStartup(splashUntil, () -> {
                show(destination, saved == null ? 0 : saved.getLong("record"));
                if (page == Page.DETAIL) {
                    Uri restoredPhoto = saved == null ? null : BundleCompat.getParcelable(saved, "photo", Uri.class);
                    Uri photo = pendingPhoto == null ? restoredPhoto : pendingPhoto;
                    if (photo != null) setPhoto(photo);
                } else if (page == Page.REPORT && pendingPhoto != null) {
                    setPhoto(pendingPhoto);
                }
                pendingPhoto = null;
            });
        }, message -> finishStartup(splashUntil, () -> {
            show(Page.LOGIN, 0);
            toast(message);
        }));
    }

    private void finishStartup(long splashUntil, Runnable displayScreen) {
        View root = findViewById(R.id.main);
        Runnable reveal = () -> {
            if (isDestroyed() || isFinishing()) return;
            findViewById(R.id.splash).setVisibility(View.GONE);
            root.setBackgroundColor(getColor(R.color.paper));
            WindowCompat.getInsetsController(getWindow(), root).setAppearanceLightNavigationBars(true);
            displayScreen.run();
        };
        long remaining = splashUntil - SystemClock.uptimeMillis();
        if (remaining > 0) root.postDelayed(reveal, remaining);
        else reveal.run();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (page == null) return;
        state.putString("page", page.name());
        state.putString("returnPage", returnPage.name());
        state.putLong("record", recordId);
        state.putInt("homeScroll", page == Page.HOME ? scroll.getScrollY() : homeScroll);
        state.putParcelable("photo", selectedPhoto);
        state.putParcelable("reportPhoto", reportPhoto);
        reportScreens.save(state);
        claimScreens.save(state);
    }

    public FindItApplication services() { return services; }
    public User user() { return user; }
    public long userId() { return user.id(); }
    public PhotoStore photos() { return photos; }

    public void signedIn(User account) {
        user = account;
        go(Page.HOME);
    }

    public void updateUser(User account) { user = account; }

    public void signedOut() {
        user = null;
        homeScroll = 0;
        reportPhoto = null;
        reportScreens.resetSearch();
        claimScreens.reset();
        go(Page.LOGIN);
    }

    public void detail(long id) {
        returnPage = page;
        if (page == Page.HOME) homeScroll = scroll.getScrollY();
        show(Page.DETAIL, id);
    }

    public void claim(long id) {
        returnPage = page;
        show(Page.CLAIM, id);
    }

    public void reloadClaim() { show(Page.CLAIM, recordId); }

    public void go(Page next) {
        if (!saving && page != next) show(next, 0);
    }

    public void openReportLookup(boolean showFilter) {
        launchLookupWithFilter = showFilter;
        go(Page.REPORT_LOOKUP);
    }

    public void clearReportPhoto() { reportPhoto = null; }

    public void restoreHomeScroll() { scroll.post(() -> scroll.scrollTo(0, homeScroll)); }

    private void show(Page next, long id) {
        if (saving) return;
        if (page == Page.REPORT) reportScreens.leaveReport();
        revision++;
        page = next;
        recordId = id;
        selectedPhoto = null;
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(holder.getWindowToken(), 0);
        holder.removeAllViews();
        holder.requestFocus();
        scroll.scrollTo(0, 0);
        switch (next) {
            case LOGIN: authScreens.login(); break;
            case REGISTER: authScreens.register(); break;
            case HOME: reportScreens.home(); break;
            case REPORT:
                reportScreens.report();
                if (reportPhoto != null) markPhotoSelected();
                break;
            case REPORT_LOOKUP:
                reportScreens.lookup(launchLookupWithFilter);
                launchLookupWithFilter = null;
                break;
            case DETAIL: reportScreens.detail(id); break;
            case CLAIMS: claimScreens.claims(); break;
            case CLAIM: claimScreens.claimDetail(id); break;
            case HISTORY: claimScreens.history(); break;
            case ACCOUNT: accountScreens.account(); break;
            case PROFILE: accountScreens.editProfile(); break;
            case PASSWORD: accountScreens.changePassword(); break;
        }
    }

    private void back() {
        if (saving || page == null) return;
        switch (page) {
            case LOGIN:
            case HOME: finish(); break;
            case REGISTER: go(Page.LOGIN); break;
            case REPORT_LOOKUP: go(Page.REPORT); break;
            case PROFILE:
            case PASSWORD: go(Page.ACCOUNT); break;
            case DETAIL:
            case CLAIM: go(returnPage); break;
            default: go(Page.HOME);
        }
    }

    public View inflateScreen(int layout, String title, boolean showBack, boolean showNav, boolean art) {
        findViewById(R.id.header).setVisibility(title.isEmpty() ? View.GONE : View.VISIBLE);
        findViewById(R.id.back).setVisibility(showBack ? View.VISIBLE : View.GONE);
        findViewById(R.id.nav).setVisibility(showNav ? View.VISIBLE : View.GONE);
        boolean lightHeader = page == Page.REGISTER;
        View header = findViewById(R.id.header);
        header.setBackgroundResource(lightHeader ? android.R.color.white : R.drawable.art_header);
        ImageButton back = findViewById(R.id.back);
        back.setColorFilter(getColor(lightHeader ? R.color.teal : R.color.white));
        TextView heading = findViewById(R.id.title);
        heading.setTextColor(getColor(lightHeader ? R.color.teal : R.color.white));
        heading.setText(title);
        setStatusSurface(!lightHeader);
        showFooterArt(art, false);
        Page active = page == Page.CLAIM ? Page.CLAIMS : page;
        highlight(R.id.nav_home, active == Page.HOME);
        highlight(R.id.nav_report, active == Page.REPORT);
        highlight(R.id.nav_claims, active == Page.CLAIMS);
        highlight(R.id.nav_history, active == Page.HISTORY);
        highlight(R.id.nav_account, active == Page.ACCOUNT);
        View content = getLayoutInflater().inflate(layout, holder, false);
        int width = getResources().getDisplayMetrics().widthPixels;
        View base = findViewById(R.id.main);
        width -= base.getPaddingLeft() + base.getPaddingRight();
        boolean fullBleed = page == Page.LOGIN || page == Page.REGISTER;
        int capped = Math.min(Math.max(1, width - (fullBleed ? 0 : dp(36))), dp(620));
        int height = page == Page.REGISTER ? FrameLayout.LayoutParams.MATCH_PARENT
                : FrameLayout.LayoutParams.WRAP_CONTENT;
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(capped, height,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        params.topMargin = fullBleed ? 0 : dp(20);
        params.bottomMargin = fullBleed ? 0 : dp(18);
        holder.addView(content, params);
        return content;
    }

    private void setStatusSurface(boolean teal) {
        View root = findViewById(R.id.main);
        findViewById(R.id.status_scrim).setVisibility(teal ? View.VISIBLE : View.INVISIBLE);
        WindowCompat.getInsetsController(getWindow(), root).setAppearanceLightStatusBars(!teal);
    }

    private void highlight(int id, boolean active) {
        ImageButton tab = findViewById(id);
        tab.setSelected(active);
        tab.setColorFilter(getColor(active ? R.color.deep_teal : R.color.ink));
    }

    public int dp(float amount) {
        return Math.round(amount * getResources().getDisplayMetrics().density);
    }

    public void showFooterArt(boolean show, boolean aboveNavigation) {
        View artwork = findViewById(R.id.footer_art);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) artwork.getLayoutParams();
        params.bottomMargin = aboveNavigation ? Math.max(dp(62), findViewById(R.id.nav).getHeight()) : 0;
        artwork.setLayoutParams(params);
        artwork.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void choosePhoto() {
        picker.launch(new String[]{"image/*"});
    }

    private void photoSelected(Uri uri) {
        if (uri == null) return;
        if (page == null) {
            pendingPhoto = uri;
        } else if (page == Page.REPORT || page == Page.DETAIL) {
            setPhoto(uri);
        }
    }

    private void setPhoto(Uri uri) {
        if (page == Page.REPORT) reportPhoto = uri;
        else selectedPhoto = uri;
        markPhotoSelected();
    }

    private void markPhotoSelected() {
        Button button = holder.findViewById(R.id.photo);
        if (button != null) button.setText(R.string.photo_selected);
    }

    public <T> Callable<T> withPhoto(Function<String, T> action) {
        Uri uri = page == Page.REPORT ? reportPhoto : selectedPhoto;
        return () -> {
            String photo = photos.importPhoto(uri);
            try {
                return action.apply(photo);
            } catch (RuntimeException failure) {
                photos.discard(photo);
                throw failure;
            }
        };
    }

    public <T> void query(Callable<T> work, Consumer<T> result) {
        run(work, result, this::toast);
    }

    public <T> void submit(View button, Callable<T> work, Consumer<T> result) {
        if (saving) return;
        saving = true;
        button.setEnabled(false);
        run(work, value -> {
            saving = false;
            button.setEnabled(true);
            result.accept(value);
        }, message -> {
            saving = false;
            button.setEnabled(true);
            toast(message);
        });
    }

    private <T> void run(Callable<T> work, Consumer<T> result, Consumer<String> fail) {
        int expected = revision;
        services.worker.execute(() -> {
            try {
                T value = work.call();
                runOnUiThread(() -> {
                    if (!isDestroyed() && expected == revision) result.accept(value);
                });
            } catch (Exception failure) {
                String message = failure instanceof IllegalArgumentException ? failure.getMessage()
                        : "Terjadi kesalahan. Silakan coba lagi.";
                if (!(failure instanceof IllegalArgumentException)) Log.e("FindIt", "Operation failed", failure);
                runOnUiThread(() -> {
                    if (!isDestroyed() && expected == revision) fail.accept(message);
                });
            }
        });
    }

    public void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
