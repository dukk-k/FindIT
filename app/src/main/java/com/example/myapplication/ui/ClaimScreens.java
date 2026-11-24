package com.example.myapplication.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import com.example.myapplication.MainActivity;
import com.example.myapplication.MainActivity.Page;
import com.example.myapplication.R;
import com.example.myapplication.model.*;
import java.util.ArrayList;
import java.util.List;

public final class ClaimScreens extends Screen {
    private boolean incoming;
    private boolean claimHistory;

    public ClaimScreens(MainActivity host) { super(host); }

    public void reset() {
        incoming = false;
        claimHistory = false;
    }

    public void save(Bundle state) {
        state.putBoolean("incoming", incoming);
        state.putBoolean("claimHistory", claimHistory);
    }

    public void restore(Bundle state) {
        incoming = state.getBoolean("incoming");
        claimHistory = state.getBoolean("claimHistory");
    }

    public void claims() {
        View root = host.inflateScreen(R.layout.screen_claims, "Klaim", false, true, false);
        LinearLayout list = root.findViewById(R.id.results);
        button(root, R.id.mine, v -> { incoming = false; loadClaims(root, list); });
        button(root, R.id.incoming, v -> { incoming = true; loadClaims(root, list); });
        loadClaims(root, list);
    }

    private void loadClaims(View root, LinearLayout list) {
        boolean manage = incoming;
        long actor = host.userId();
        tabs(root, manage ? R.id.incoming : R.id.mine, R.id.mine, R.id.incoming);
        host.query(() -> claimCards(actor, manage), cards -> {
            if (manage != incoming) return;
            list.removeAllViews();
            if (cards.isEmpty()) {
                message(list, manage ? "Belum ada klaim untuk barang temuan Anda."
                        : "Belum ada klaim. Buka detail barang temuan untuk mengajukan klaim.");
                return;
            }
            if (!manage) {
                for (ClaimCard card : cards) {
                    if (card.code == null) continue;
                    showClaimRow(list, card, false);
                    pickup(list, card);
                }
                if (cards.stream().anyMatch(card -> card.code != null)
                        && cards.stream().anyMatch(card -> card.code == null)) heading(list, "Klaim lainnya");
            }
            for (ClaimCard card : cards) {
                if (manage || card.code == null) showClaimRow(list, card, manage);
            }
        });
    }

    private List<ClaimCard> claimCards(long actor, boolean manage) {
        List<Claim> claims = manage ? host.services().claims.incoming(actor) : host.services().claims.mine(actor);
        List<ClaimCard> cards = new ArrayList<>();
        for (Claim claim : claims) cards.add(card(actor, claim, manage));
        return cards;
    }

    private ClaimCard card(long actor, Claim claim, boolean manage) {
        Item item = host.services().items.detail(claim.itemId());
        PickupCode code = manage ? null : host.services().claims.pickupCode(actor, claim.id());
        String person = host.services().displayName(manage ? claim.claimantId() : item.reporterId());
        String contact = code == null ? "" : host.services().finderContact(actor, claim.id());
        return new ClaimCard(claim, item, code, person, contact);
    }

    private void showClaimRow(LinearLayout list, ClaimCard card, boolean manage) {
        String subtitle = manage ? "Klaim dari " + card.person : "Diajukan " + date(card.claim.submittedAt());
        claimRow(list, card.item, card.claim, subtitle, () -> host.claim(card.claim.id()));
    }

    private void pickup(LinearLayout list, ClaimCard card) {
        View panel = host.getLayoutInflater().inflate(R.layout.panel_pickup, list, false);
        text(panel, R.id.pickup_code, card.code.value());
        text(panel, R.id.finder_contact, "Kontak penemu: " + card.person + " · " + card.contact);
        list.addView(panel);
    }

    public void claimDetail(long id) {
        View root = host.inflateScreen(R.layout.screen_claim_detail, "Klaim", true, true, false);
        visible(root, R.id.secret, false);
        visible(root, R.id.handover_form, false);
        long actor = host.userId();
        host.query(() -> {
            Claim claim = host.services().claims.view(actor, id);
            Item item = host.services().items.detail(claim.itemId());
            boolean manage = item.reporterId() == actor;
            return new ClaimDetail(card(actor, claim, manage), manage,
                    host.services().items.secret(item.id(), actor),
                    host.services().history.timeline(actor, item.id()));
        }, detail -> bindClaimDetail(root, detail, actor));
    }

    private void bindClaimDetail(View root, ClaimDetail detail, long actor) {
        ClaimCard card = detail.card;
        LinearLayout summary = root.findViewById(R.id.summary);
        String subtitle = detail.manage ? "Klaim dari " + card.person : "Diajukan " + date(card.claim.submittedAt());
        claimRow(summary, card.item, card.claim, subtitle, null);
        LinearLayout content = root.findViewById(R.id.claim_content);
        if (card.code != null) {
            pickup(content, card);
            host.showFooterArt(true, true);
        }
        if (!card.claim.proof().isEmpty() || !card.claim.photo().isEmpty()) {
            heading(content, detail.manage ? "Jawaban pengklaim" : "Bukti yang Anda kirim");
            if (!card.claim.proof().isEmpty()) message(content, card.claim.proof());
            if (!card.claim.photo().isEmpty()) {
                View picture = host.getLayoutInflater().inflate(R.layout.row_proof, content, false);
                content.addView(picture);
                loadPhoto(picture.findViewById(R.id.proof_image), card.claim.photo(), 720);
            }
        }
        if (detail.secret != null) {
            visible(root, R.id.secret, true);
            text(root, R.id.secret, "Informasi rahasia Anda\n"
                    + (detail.secret.brand().isEmpty() ? "" : detail.secret.brand() + " · ") + detail.secret.markers());
        }
        if (detail.manage && card.claim.status() == Claim.Status.PENDING) {
            View actions = host.getLayoutInflater().inflate(R.layout.row_review, content, false);
            content.addView(actions);
            CheckBox verification = actions.findViewById(R.id.verify_proof);
            button(actions, R.id.reject, v -> review(v, actor, card.claim.id(), false, false));
            button(actions, R.id.approve, v -> review(v, actor, card.claim.id(), true, verification.isChecked()));
        }
        timeline(content, detail.events);
        if (detail.manage && card.claim.status() == Claim.Status.APPROVED) {
            visible(root, R.id.handover_form, true);
            button(root, R.id.handover, v -> {
                String entered = value(root, R.id.pickup_entry);
                host.submit(v, () -> {
                    host.services().handover.confirm(actor, card.claim.id(), entered);
                    return null;
                }, ignored -> {
                    host.toast("Barang sudah dikembalikan.");
                    host.reloadClaim();
                });
            });
        }
    }

    private void review(View button, long actor, long id, boolean approve, boolean verified) {
        if (approve && !verified) {
            host.toast("Centang verifikasi setelah memeriksa bukti pengklaim.");
            return;
        }
        new AlertDialog.Builder(host).setTitle(approve ? "Setujui klaim ini?" : "Tolak klaim ini?")
                .setMessage(approve ? "Sistem akan membuat kode pengambilan untuk pemilik."
                        : "Klaim akan ditandai ditolak.")
                .setNegativeButton("Batal", null).setPositiveButton(approve ? "Setujui" : "Tolak",
                        (dialog, which) -> host.submit(button, () -> {
                            host.services().claims.review(actor, id, approve, verified);
                            return null;
                        }, ignored -> host.reloadClaim())).show();
    }

    public void history() {
        View root = host.inflateScreen(R.layout.screen_history, "Riwayat", false, true, false);
        LinearLayout list = root.findViewById(R.id.results);
        button(root, R.id.reports, v -> { claimHistory = false; loadHistory(root, list); });
        button(root, R.id.mine, v -> { claimHistory = true; loadHistory(root, list); });
        loadHistory(root, list);
    }

    private void loadHistory(View root, LinearLayout list) {
        long actor = host.userId();
        tabs(root, claimHistory ? R.id.mine : R.id.reports, R.id.reports, R.id.mine);
        if (claimHistory) {
            host.query(() -> claimCards(actor, false), cards -> {
                if (!claimHistory) return;
                list.removeAllViews();
                if (cards.isEmpty()) message(list, "Riwayat klaim akan muncul di sini.");
                for (ClaimCard card : cards) showClaimRow(list, card, false);
            });
        } else {
            host.query(() -> host.services().items.ownReports(actor), items -> {
                if (claimHistory) return;
                list.removeAllViews();
                if (items.isEmpty()) message(list, "Laporan yang Anda kirim akan muncul di sini.");
                for (int index = 0; index < items.size(); index++) {
                    Item item = items.get(index);
                    itemRow(list, item, () -> reportHistory(item, actor));
                    if (index == 0) {
                        LinearLayout latest = new LinearLayout(host);
                        latest.setOrientation(LinearLayout.VERTICAL);
                        list.addView(latest);
                        host.query(() -> host.services().history.timeline(actor, item.id()), events -> {
                            if (latest.isAttachedToWindow()) timeline(latest, events);
                        });
                    }
                }
            });
        }
    }

    private void reportHistory(Item item, long actor) {
        host.query(() -> host.services().history.timeline(actor, item.id()), events -> {
            LinearLayout body = (LinearLayout) host.getLayoutInflater().inflate(R.layout.panel_timeline, null, false);
            fillTimeline(body, events);
            android.widget.ScrollView container = new android.widget.ScrollView(host);
            container.addView(body);
            new AlertDialog.Builder(host).setTitle(item.name()).setView(container)
                    .setNeutralButton("Detail barang", (dialog, which) -> host.detail(item.id()))
                    .setPositiveButton("Tutup", null).show();
        });
    }

    private void timeline(LinearLayout list, List<StatusHistory> events) {
        heading(list, "Perkembangan barang");
        LinearLayout panel = (LinearLayout) host.getLayoutInflater().inflate(R.layout.panel_timeline, list, false);
        fillTimeline(panel, events);
        list.addView(panel);
    }

    private void fillTimeline(LinearLayout panel, List<StatusHistory> events) {
        for (StatusHistory event : events) {
            View row = host.getLayoutInflater().inflate(R.layout.row_event, panel, false);
            text(row, R.id.event_title, event.status().label);
            text(row, R.id.event_date, date(event.time()));
            panel.addView(row);
        }
    }

    private static final class ClaimCard {
        final Claim claim;
        final Item item;
        final PickupCode code;
        final String person;
        final String contact;

        ClaimCard(Claim claim, Item item, PickupCode code, String person, String contact) {
            this.claim = claim;
            this.item = item;
            this.code = code;
            this.person = person;
            this.contact = contact;
        }
    }

    private static final class ClaimDetail {
        final ClaimCard card;
        final boolean manage;
        final FoundDetails secret;
        final List<StatusHistory> events;

        ClaimDetail(ClaimCard card, boolean manage, FoundDetails secret, List<StatusHistory> events) {
            this.card = card;
            this.manage = manage;
            this.secret = secret;
            this.events = events;
        }
    }
}
