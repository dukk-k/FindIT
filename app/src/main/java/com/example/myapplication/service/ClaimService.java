package com.example.myapplication.service;

import com.example.myapplication.model.*;
import com.example.myapplication.repository.*;
import java.security.SecureRandom;
import java.util.List;
import static com.example.myapplication.service.Validation.*;

public final class ClaimService {
    private static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private final SecureRandom random = new SecureRandom();
    private final ItemRepository items;
    private final ClaimRepository claims;
    private final TransactionRunner transactions;

    public ClaimService(ItemRepository items, ClaimRepository claims, TransactionRunner transactions) {
        this.items = items;
        this.claims = claims;
        this.transactions = transactions;
    }

    public boolean canSubmit(Item item, long user) {
        return item.supportsClaims() && item.reporterId() != user
                && (item.status() == ReportStatus.FOUND || item.status() == ReportStatus.PENDING)
                && !claims.hasActive(item.id(), user);
    }

    public long submit(long user, long itemId, String proof, String photo) {
        String description = text(proof, 2000, "Bukti kepemilikan");
        require(!description.isEmpty() || !photo.isEmpty(), "Isi ciri barang atau lampirkan foto bukti.");
        return transactions.run(() -> {
            Item item = items.get(itemId);
            require(item != null && item.supportsClaims(), "Hanya barang temuan yang dapat diklaim.");
            require(item.reporterId() != user, "Anda tidak dapat mengklaim laporan sendiri.");
            require(!claims.hasActive(itemId, user), "Anda sudah memiliki klaim aktif untuk barang ini.");
            require(canSubmit(item, user), "Barang ini tidak tersedia untuk klaim baru.");
            long now = System.currentTimeMillis();
            long id = claims.add(itemId, user, description, photo, now);
            if (item.status() == ReportStatus.FOUND) changeReport(itemId, ReportStatus.PENDING, now);
            return id;
        });
    }

    public List<Claim> mine(long user) { return claims.byClaimant(user); }
    public List<Claim> incoming(long user) { return claims.claimsForReporter(user); }

    public Claim view(long user, long claimId) {
        Claim claim = claims.getClaim(claimId);
        require(claim != null, "Klaim tidak ditemukan.");
        require(claim.claimantId() == user || items.get(claim.itemId()).reporterId() == user,
                "Anda tidak memiliki akses ke klaim ini.");
        return claim;
    }

    public PickupCode pickupCode(long user, long claim) { return claims.codeForClaimant(claim, user); }

    public void review(long finder, long claimId, boolean approve, boolean proofVerified) {
        transactions.run(() -> {
            Claim claim = view(finder, claimId);
            Item item = items.get(claim.itemId());
            require(item.reporterId() == finder, "Hanya penemu yang dapat meninjau klaim.");
            require(claim.status() == Claim.Status.PENDING && item.status() == ReportStatus.PENDING,
                    "Klaim ini sudah diproses.");
            long now = System.currentTimeMillis();
            if (approve) {
                require(proofVerified, "Verifikasi bukti kepemilikan terlebih dahulu.");
                reserveCode(claimId);
                claims.updateStatus(claimId, Claim.Status.APPROVED);
                claims.rejectOtherPending(item.id(), claimId);
                items.appendHistory(item.id(), ReportStatus.APPROVED, now);
                changeReport(item.id(), ReportStatus.PICKUP, now);
            } else {
                claims.updateStatus(claimId, Claim.Status.REJECTED);
                if (!claims.hasPending(item.id())) changeReport(item.id(), ReportStatus.FOUND, now);
            }
            return null;
        });
    }

    private void reserveCode(long claim) {
        for (int attempt = 0; attempt < 16; attempt++) {
            StringBuilder code = new StringBuilder(6);
            for (int i = 0; i < 6; i++) code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            if (claims.reserveCode(claim, code.toString())) return;
        }
        throw new IllegalStateException("Kode belum dapat dibuat. Coba lagi.");
    }

    private void changeReport(long id, ReportStatus status, long now) {
        items.updateStatus(id, status);
        items.appendHistory(id, status, now);
    }
}
