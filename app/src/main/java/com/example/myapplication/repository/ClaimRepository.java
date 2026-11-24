package com.example.myapplication.repository;

import com.example.myapplication.model.Claim;
import com.example.myapplication.model.PickupCode;
import java.util.List;

public interface ClaimRepository {
    long add(long itemId, long claimantId, String proof, String photo, long time);
    Claim getClaim(long id);
    List<Claim> byClaimant(long claimantId);
    List<Claim> claimsForReporter(long reporterId);
    boolean hasActive(long itemId, long claimantId);
    boolean hasPending(long itemId);
    void updateStatus(long id, Claim.Status status);
    void rejectOtherPending(long itemId, long approvedId);
    boolean reserveCode(long claimId, String value);
    PickupCode codeForClaimant(long claimId, long claimantId);
    boolean consumeCode(long claimId, String value);
}
