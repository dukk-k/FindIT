package com.example.myapplication.model;

public final class Claim {
    public enum Status {
        PENDING("Menunggu Klaim"), APPROVED("Klaim Disetujui"),
        REJECTED("Klaim Ditolak"), RETURNED("Sudah Dikembalikan");

        public final String label;

        Status(String label) {
            this.label = label;
        }
    }

    private final long id;
    private final long itemId;
    private final long claimantId;
    private final String proof;
    private final String photo;
    private final Status status;
    private final long submittedAt;

    public Claim(long id, long itemId, long claimantId, String proof, String photo,
                 Status status, long submittedAt) {
        this.id = id;
        this.itemId = itemId;
        this.claimantId = claimantId;
        this.proof = proof;
        this.photo = photo;
        this.status = status;
        this.submittedAt = submittedAt;
    }

    public long id() { return id; }
    public long itemId() { return itemId; }
    public long claimantId() { return claimantId; }
    public String proof() { return proof; }
    public String photo() { return photo; }
    public Status status() { return status; }
    public long submittedAt() { return submittedAt; }
}
