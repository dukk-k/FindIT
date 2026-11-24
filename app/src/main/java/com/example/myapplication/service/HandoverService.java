package com.example.myapplication.service;

import com.example.myapplication.model.Claim;
import com.example.myapplication.model.Item;
import com.example.myapplication.model.ReportStatus;
import com.example.myapplication.repository.*;
import java.util.Locale;
import static com.example.myapplication.service.Validation.require;

public final class HandoverService {
    private final ItemRepository items;
    private final ClaimRepository claims;
    private final TransactionRunner transactions;

    public HandoverService(ItemRepository items, ClaimRepository claims, TransactionRunner transactions) {
        this.items = items;
        this.claims = claims;
        this.transactions = transactions;
    }

    public void confirm(long finder, long claimId, String value) {
        String code = value.trim().toUpperCase(Locale.ROOT);
        require(code.matches("[2-9A-HJ-NP-Z]{6}"), "Masukkan kode pengambilan yang valid.");
        transactions.run(() -> {
            Claim claim = claims.getClaim(claimId);
            require(claim != null, "Klaim tidak ditemukan.");
            Item item = items.get(claim.itemId());
            require(item.reporterId() == finder, "Hanya penemu yang dapat mengonfirmasi pengembalian.");
            require(claim.status() == Claim.Status.APPROVED && item.status() == ReportStatus.PICKUP,
                    "Klaim ini tidak menunggu pengambilan.");
            require(claims.consumeCode(claimId, code), "Kode salah atau sudah digunakan.");
            claims.updateStatus(claimId, Claim.Status.RETURNED);
            items.updateStatus(item.id(), ReportStatus.RETURNED);
            items.appendHistory(item.id(), ReportStatus.RETURNED, System.currentTimeMillis());
            return null;
        });
    }
}
