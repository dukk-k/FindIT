package com.example.myapplication.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import com.example.myapplication.MainActivity;
import com.example.myapplication.MainActivity.Page;
import com.example.myapplication.R;
import com.example.myapplication.model.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public final class ReportScreens extends Screen {
    private static final Item.Category[] CATEGORIES = Item.Category.values();
    private static final ReportStatus[] FILTERS = { ReportStatus.LOST, ReportStatus.FOUND, ReportStatus.PENDING,
            ReportStatus.PICKUP, ReportStatus.RETURNED };
    private static final int[] FORM_FIELDS = { R.id.name, R.id.color, R.id.location, R.id.description, R.id.brand, R.id.markers };
    private final DateTimeFormatter inputDate = DateTimeFormatter.ofPattern("dd/MM/yyyy", INDONESIAN);
    private BrowseState home = new BrowseState();
    private BrowseState lookup = new BrowseState();
    private Item.Type reportType = Item.Type.LOST;
    private Item.Category reportCategory;
    private LocalDate reportDate = LocalDate.now();
    private View reportRoot;
    private Bundle draftFields = new Bundle();
    private int request;

    public ReportScreens(MainActivity host) { super(host); }

    public void resetSearch() {
        home = new BrowseState();
        lookup = new BrowseState();
        clearDraft();
    }

    public void home() {
        bindBrowse(home, false, null);
    }

    public void lookup(Boolean filterOnEntry) {
        bindBrowse(lookup, true, filterOnEntry);
    }

    private void bindBrowse(BrowseState state, boolean fromReport, Boolean filterOnEntry) {
        String title = host.getString(fromReport ? R.string.lookup_title : R.string.browse_title);
        View root = host.inflateScreen(R.layout.screen_home, title, fromReport, !fromReport, false);
        EditText field = root.findViewById(R.id.search);
        field.setText(state.keyword);
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                state.keyword = s.toString();
                refresh(root, state, false);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        button(root, R.id.all, v -> selectType(root, state, null));
        button(root, R.id.lost, v -> selectType(root, state, Item.Type.LOST));
        button(root, R.id.found, v -> selectType(root, state, Item.Type.FOUND));
        button(root, R.id.filter, v -> filters(root, state));
        refresh(root, state, !fromReport);
        if (Boolean.TRUE.equals(filterOnEntry)) {
            filters(root, state);
        } else if (Boolean.FALSE.equals(filterOnEntry)) {
            field.requestFocus();
            field.setSelection(field.getText().length());
            field.post(() -> {
                if (field.isAttachedToWindow() && field.hasFocus()) {
                    WindowCompat.getInsetsController(host.getWindow(), field)
                            .show(WindowInsetsCompat.Type.ime());
                }
            });
        }
    }

    private void selectType(View root, BrowseState state, Item.Type type) {
        state.type = type;
        refresh(root, state, false);
    }

    private void refresh(View root, BrowseState state, boolean restoreScroll) {
        tabs(root, state.type == null ? R.id.all : state.type == Item.Type.LOST ? R.id.lost : R.id.found,
                R.id.all, R.id.lost, R.id.found);
        text(root, R.id.filter, host.getString(state.category == null && state.status == null
                ? R.string.browse_filter : R.string.browse_filter_active));
        int serial = ++request;
        String keyword = state.keyword;
        Item.Type type = state.type;
        Item.Category category = state.category;
        ReportStatus status = state.status;
        host.query(() -> host.services().items.browse(keyword, type, category, status), items -> {
            if (serial != request) return;
            LinearLayout list = root.findViewById(R.id.results);
            list.removeAllViews();
            if (items.isEmpty()) {
                boolean filtered = !keyword.isEmpty() || type != null || category != null || status != null;
                message(list, host.getString(filtered ? R.string.browse_no_matches : R.string.browse_empty));
                list.getChildAt(0).setBackgroundResource(R.drawable.bg_list_empty);
            }
            for (Item item : items) itemRow(list, item, () -> host.detail(item.id()));
            if (restoreScroll) host.restoreHomeScroll();
        });
    }

    private void filters(View root, BrowseState state) {
        String[] labels = {"Kategori · " + (state.category == null ? "Semua" : state.category.label),
                "Status · " + (state.status == null ? "Semua" : state.status.label)};
        new AlertDialog.Builder(host).setTitle("Filter barang").setItems(labels, (dialog, which) -> {
            if (which == 0) {
                String[] entries = new String[CATEGORIES.length + 1];
                entries[0] = "Semua kategori";
                for (int i = 0; i < CATEGORIES.length; i++) entries[i + 1] = CATEGORIES[i].label;
                choose("Kategori", entries, state.category == null ? 0 : state.category.ordinal() + 1, selected -> {
                    state.category = selected == 0 ? null : CATEGORIES[selected - 1];
                    refresh(root, state, false);
                });
            } else {
                String[] entries = new String[FILTERS.length + 1];
                entries[0] = "Semua status";
                for (int i = 0; i < FILTERS.length; i++) entries[i + 1] = FILTERS[i].label;
                choose("Status", entries, Arrays.asList(FILTERS).indexOf(state.status) + 1, selected -> {
                    state.status = selected == 0 ? null : FILTERS[selected - 1];
                    refresh(root, state, false);
                });
            }
        }).setNeutralButton("Reset", (dialog, which) -> {
            state.category = null;
            state.status = null;
            refresh(root, state, false);
        }).setNegativeButton("Batal", null).show();
    }

    public void report() {
        View root = host.inflateScreen(R.layout.screen_report, host.getString(R.string.report_title), true, true, false);
        reportRoot = root;
        for (int id : FORM_FIELDS) text(root, id, draftFields.getString("field" + id, ""));
        button(root, R.id.report_search, v -> host.openReportLookup(false));
        button(root, R.id.report_filter, v -> host.openReportLookup(true));
        button(root, R.id.lost, v -> setType(root, Item.Type.LOST));
        button(root, R.id.found, v -> setType(root, Item.Type.FOUND));
        setType(root, reportType);
        button(root, R.id.photo, v -> host.choosePhoto());
        button(root, R.id.category, v -> {
            String[] labels = Arrays.stream(CATEGORIES).map(category -> category.label).toArray(String[]::new);
            choose("Jenis barang", labels, reportCategory == null ? -1 : reportCategory.ordinal(), selected -> {
                reportCategory = CATEGORIES[selected];
                text(root, R.id.category, reportCategory.label);
            });
        });
        text(root, R.id.category, reportCategory == null ? "Pilih kategori" : reportCategory.label);
        text(root, R.id.date, inputDate.format(reportDate));
        button(root, R.id.date, v -> {
            DatePickerDialog calendar = new DatePickerDialog(host, (view, year, month, day) -> {
                reportDate = LocalDate.of(year, month + 1, day);
                text(root, R.id.date, inputDate.format(reportDate));
            }, reportDate.getYear(), reportDate.getMonthValue() - 1, reportDate.getDayOfMonth());
            calendar.getDatePicker().setMaxDate(System.currentTimeMillis());
            calendar.show();
        });
        button(root, R.id.submit, v -> submitReport(root, v));
    }

    private void setType(View root, Item.Type type) {
        reportType = type;
        tabs(root, type == Item.Type.LOST ? R.id.lost : R.id.found, R.id.lost, R.id.found);
        visible(root, R.id.secret_fields, type == Item.Type.FOUND);
        ((EditText) root.findViewById(R.id.description)).setHint(type == Item.Type.FOUND
                ? "Deskripsi publik tanpa ciri khusus (opsional)" : "Jelaskan barang yang hilang");
    }

    private void submitReport(View root, View submit) {
        long actor = host.userId();
        Item.Type type = reportType;
        Item.Category category = reportCategory;
        LocalDate date = reportDate;
        String name = value(root, R.id.name);
        String color = value(root, R.id.color);
        String location = value(root, R.id.location);
        String description = value(root, R.id.description);
        String brand = value(root, R.id.brand);
        String markers = value(root, R.id.markers);
        host.submit(submit, host.withPhoto(photo -> host.services().items.report(actor, type, name, category,
                color, location, date, description, photo, brand, markers)), id -> {
            clearDraft();
            host.clearReportPhoto();
            host.toast("Laporan berhasil dikirim.");
            host.go(Page.HOME);
        });
    }

    public void detail(long id) {
        View root = host.inflateScreen(R.layout.screen_detail, "Detail Barang", true, false, false);
        visible(root, R.id.claim_form, false);
        visible(root, R.id.detail_note, false);
        long actor = host.userId();
        host.query(() -> {
            Item item = host.services().items.detail(id);
            return new Detail(item, host.services().displayName(item.reporterId()),
                    host.services().items.secret(id, actor), host.services().claims.canSubmit(item, actor));
        }, detail -> bindDetail(root, detail, actor));
    }

    private void bindDetail(View root, Detail detail, long actor) {
        Item item = detail.item;
        text(root, R.id.item_name, item.name());
        TextView status = root.findViewById(R.id.item_status);
        badge(status, item.status().label, statusColor(item.status()), statusInk(item.status()));
        loadPhoto(root.findViewById(R.id.item_photo), item.photo(), 720);
        String description = item.description().isEmpty() ? "" : "\n\n" + item.description();
        text(root, R.id.item_meta, "Jenis: " + item.category().label + " · Warna: " + item.color()
                + "\nLokasi: " + item.location() + " · " + date(item.date())
                + "\nDilaporkan oleh: " + detail.reporter + description);
        visible(root, R.id.detail_note, item.supportsClaims());
        if (item.supportsClaims()) {
            text(root, R.id.detail_note, detail.secret == null ? host.getString(R.string.secret_note)
                    : "Informasi rahasia Anda\n" + (detail.secret.brand().isEmpty() ? "" : detail.secret.brand() + " · ")
                    + detail.secret.markers());
        }
        visible(root, R.id.claim_form, detail.canSubmit);
        if (detail.canSubmit) {
            button(root, R.id.photo, v -> host.choosePhoto());
            button(root, R.id.submit, v -> {
                String proof = value(root, R.id.proof);
                host.submit(v, host.withPhoto(photo -> host.services().claims.submit(actor, item.id(), proof, photo)),
                        claim -> {
                            host.toast("Klaim dikirim.");
                            host.go(Page.CLAIMS);
                        });
            });
        } else if (item.supportsClaims()) {
            LinearLayout actions = root.findViewById(R.id.detail_actions);
            message(actions, item.reporterId() == actor ? "Kelola klaim laporan Anda di tab Klaim."
                    : "Klaim baru tidak tersedia. Lihat tab Klaim untuk mengikuti klaim Anda.");
        }
    }

    private void clearDraft() {
        reportRoot = null;
        draftFields = new Bundle();
        reportType = Item.Type.LOST;
        reportCategory = null;
        reportDate = LocalDate.now();
    }

    private void captureDraft() {
        if (reportRoot == null) return;
        for (int id : FORM_FIELDS) draftFields.putString("field" + id, value(reportRoot, id));
    }

    public void leaveReport() {
        captureDraft();
        reportRoot = null;
    }

    public void save(Bundle bundle) {
        captureDraft();
        bundle.putBundle("homeBrowse", home.save());
        bundle.putBundle("lookupBrowse", lookup.save());
        bundle.putBundle("reportDraftFields", draftFields);
        bundle.putString("reportType", reportType.name());
        bundle.putString("reportDate", reportDate.toString());
        if (reportCategory != null) bundle.putString("reportCategory", reportCategory.name());
    }

    public void restore(Bundle bundle) {
        home = BrowseState.restore(bundle.getBundle("homeBrowse"));
        lookup = BrowseState.restore(bundle.getBundle("lookupBrowse"));
        Bundle fields = bundle.getBundle("reportDraftFields");
        draftFields = fields == null ? new Bundle() : new Bundle(fields);
        reportType = Item.Type.valueOf(bundle.getString("reportType", Item.Type.LOST.name()));
        reportCategory = restored(bundle, "reportCategory", Item.Category.class);
        reportDate = LocalDate.parse(bundle.getString("reportDate", LocalDate.now().toString()));
    }

    private static final class BrowseState {
        String keyword = "";
        Item.Type type;
        Item.Category category;
        ReportStatus status;

        Bundle save() {
            Bundle state = new Bundle();
            state.putString("keyword", keyword);
            if (type != null) state.putString("type", type.name());
            if (category != null) state.putString("category", category.name());
            if (status != null) state.putString("status", status.name());
            return state;
        }

        static BrowseState restore(Bundle saved) {
            BrowseState state = new BrowseState();
            if (saved != null) {
                state.keyword = saved.getString("keyword", "");
                state.type = restored(saved, "type", Item.Type.class);
                state.category = restored(saved, "category", Item.Category.class);
                state.status = restored(saved, "status", ReportStatus.class);
            }
            return state;
        }
    }

    private static <T extends Enum<T>> T restored(Bundle state, String key, Class<T> type) {
        String value = state.getString(key);
        return value == null ? null : Enum.valueOf(type, value);
    }

    private static final class Detail {
        final Item item;
        final String reporter;
        final FoundDetails secret;
        final boolean canSubmit;

        Detail(Item item, String reporter, FoundDetails secret, boolean canSubmit) {
            this.item = item;
            this.reporter = reporter;
            this.secret = secret;
            this.canSubmit = canSubmit;
        }
    }
}
