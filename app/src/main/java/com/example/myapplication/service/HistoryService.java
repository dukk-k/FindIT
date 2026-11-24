package com.example.myapplication.service;

import com.example.myapplication.model.*;
import com.example.myapplication.repository.*;
import java.util.List;
import static com.example.myapplication.service.Validation.require;

public final class HistoryService {
    private final ItemRepository items;
    private final ClaimRepository claims;

    public HistoryService(ItemRepository items, ClaimRepository claims) {
        this.items = items;
        this.claims = claims;
    }

    public List<StatusHistory> timeline(long user, long reportId) {
        Item item = items.get(reportId);
        require(item != null && (item.reporterId() == user || claims.byClaimant(user).stream()
                .anyMatch(claim -> claim.itemId() == reportId)), "Riwayat tidak tersedia untuk akun ini.");
        return items.history(reportId);
    }
}
