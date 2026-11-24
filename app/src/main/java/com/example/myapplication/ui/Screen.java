package com.example.myapplication.ui;

import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.model.Claim;
import com.example.myapplication.model.Item;
import com.example.myapplication.model.ReportStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.IntConsumer;

abstract class Screen {
    static final Locale INDONESIAN = Locale.forLanguageTag("id-ID");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", INDONESIAN);
    final MainActivity host;

    Screen(MainActivity host) { this.host = host; }

    static String value(View root, int id) {
        return ((EditText) root.findViewById(id)).getText().toString();
    }

    static void text(View root, int id, String value) {
        ((TextView) root.findViewById(id)).setText(value);
    }

    static void visible(View root, int id, boolean visible) {
        root.findViewById(id).setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    Button button(View root, int id, View.OnClickListener click) {
        Button button = root.findViewById(id);
        button.setOnClickListener(click);
        return button;
    }

    void tabs(View root, int selected, int... ids) {
        for (int id : ids) {
            Button button = root.findViewById(id);
            button.setSelected(id == selected);
            button.setBackgroundResource(id == selected ? R.drawable.bg_tab_on : R.drawable.bg_tab_off);
            button.setTextColor(host.getColor(id == selected ? R.color.deep_teal : R.color.white));
        }
    }

    void choose(String title, String[] entries, int selected, IntConsumer selection) {
        new AlertDialog.Builder(host).setTitle(title)
                .setSingleChoiceItems(entries, selected, (dialog, which) -> {
                    selection.accept(which);
                    dialog.dismiss();
                }).setNegativeButton("Batal", null).show();
    }

    void message(LinearLayout list, String message) {
        TextView label = new TextView(host);
        label.setText(message);
        label.setTextSize(14);
        label.setTextColor(host.getColor(R.color.muted));
        label.setBackgroundResource(R.drawable.bg_card);
        int pad = host.dp(16);
        label.setPadding(pad, pad, pad, pad);
        list.addView(label);
    }

    View row(LinearLayout parent, Item item, String subtitle, String status, int color, int ink, Runnable click) {
        View row = host.getLayoutInflater().inflate(R.layout.row_item, parent, false);
        text(row, R.id.item_name, item.name());
        text(row, R.id.item_meta, subtitle);
        badge(row.findViewById(R.id.item_status), status, color, ink);
        ImageView thumbnail = row.findViewById(R.id.thumbnail);
        if (item.category() == Item.Category.ACCESSORIES || item.category() == Item.Category.SUPPLIES) {
            thumbnail.setImageResource(R.drawable.ic_bag);
        }
        loadPhoto(thumbnail, item.photo(), 144);
        if (click != null) {
            row.setOnClickListener(v -> click.run());
            row.setFocusable(true);
        }
        parent.addView(row);
        return row;
    }

    void itemRow(LinearLayout parent, Item item, Runnable click) {
        row(parent, item, item.location() + " · " + date(item.date()), item.status().label,
                statusColor(item.status()), statusInk(item.status()), click);
    }

    void claimRow(LinearLayout parent, Item item, Claim claim, String subtitle, Runnable click) {
        int fill;
        int ink;
        switch (claim.status()) {
            case PENDING: fill = R.color.yellow; ink = R.color.amber; break;
            case APPROVED: fill = R.color.mint; ink = R.color.green; break;
            case REJECTED: fill = R.color.rose; ink = R.color.red; break;
            default: fill = R.color.tile; ink = R.color.deep_teal;
        }
        row(parent, item, subtitle, claim.status().label, fill, ink, click);
    }

    void loadPhoto(ImageView target, String name, int pixels) {
        if (name.isEmpty()) return;
        host.query(() -> host.photos().load(name, pixels), (Bitmap bitmap) -> {
            if (bitmap != null && target.isAttachedToWindow()) {
                target.setScaleType(ImageView.ScaleType.CENTER_CROP);
                target.setImageBitmap(bitmap);
            }
        });
    }

    void badge(TextView target, String label, int fill, int ink) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(host.getColor(fill));
        shape.setCornerRadius(host.dp(20));
        target.setBackground(shape);
        target.setText(label);
        target.setTextColor(host.getColor(ink));
    }

    static int statusColor(ReportStatus status) {
        switch (status) {
            case PENDING: return R.color.yellow;
            case PICKUP: return R.color.peach;
            case APPROVED: case RETURNED: return R.color.mint;
            default: return R.color.aqua;
        }
    }

    static int statusInk(ReportStatus status) {
        switch (status) {
            case PENDING: return R.color.amber;
            case PICKUP: return R.color.orange;
            case APPROVED: case RETURNED: return R.color.green;
            default: return R.color.deep_teal;
        }
    }

    static String date(LocalDate value) { return DATE.format(value); }

    static String date(long milliseconds) {
        return date(Instant.ofEpochMilli(milliseconds).atZone(ZoneId.systemDefault()).toLocalDate());
    }

    TextView heading(LinearLayout parent, String label) {
        TextView view = new TextView(host);
        view.setText(label);
        view.setTextSize(17);
        view.setTypeface(null, android.graphics.Typeface.BOLD);
        view.setTextColor(host.getColor(R.color.ink));
        view.setPadding(0, host.dp(10), 0, host.dp(7));
        parent.addView(view);
        return view;
    }
}
