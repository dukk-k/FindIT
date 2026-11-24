package com.example.myapplication.database;

import com.example.myapplication.model.*;
import com.example.myapplication.repository.*;
import com.example.myapplication.database.FindItDatabase.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public final class RoomRepositories implements UserRepository, ItemRepository, ClaimRepository, TransactionRunner {
    private final FindItDatabase database;
    private final FindItDao dao;

    public RoomRepositories(FindItDatabase database) {
        this.database = database;
        this.dao = database.dao();
    }

    @Override public <T> T run(Callable<T> work) { return database.runInTransaction(work); }

    @Override public User profile(long id) {
        FindItDao.Profile row = dao.profile(id);
        return row == null ? null : new User(row.id, row.name, row.email, row.phone);
    }

    @Override public String displayName(long id) { return dao.displayName(id); }
    @Override public Credentials credentials(String email) { return dao.credentials(email); }

    @Override public long register(String name, String email, String phone, String passwordHash) {
        Account row = new Account();
        row.name = name;
        row.email = email;
        row.phone = phone;
        row.passwordHash = passwordHash;
        return dao.register(row);
    }

    @Override public void updateProfile(long id, String name, String phone) { dao.profile(id, name, phone); }
    @Override public void updatePassword(long id, String hash) { dao.password(id, hash); }

    @Override public long add(Item item) {
        Report row = new Report();
        row.reporterId = item.reporterId();
        row.name = item.name();
        row.type = item.type().name();
        row.category = item.category().name();
        row.color = item.color();
        row.location = item.location();
        row.date = item.date().toString();
        row.description = item.description();
        row.photo = item.photo();
        row.status = item.status().name();
        return dao.insert(row);
    }

    @Override public Item get(long id) { return toItem(dao.report(id)); }

    @Override public List<Item> browse(String keyword, Item.Type type, Item.Category category, ReportStatus status) {
        return dao.browse(keyword.toLowerCase(Locale.ROOT), key(type), key(category), key(status))
                .stream().map(RoomRepositories::toItem).collect(Collectors.toList());
    }

    @Override public List<Item> byReporter(long id) {
        return dao.reportsBy(id).stream().map(RoomRepositories::toItem).collect(Collectors.toList());
    }

    private static String key(Enum<?> value) { return value == null ? null : value.name(); }

    private static Item toItem(Report row) {
        if (row == null) return null;
        Item.Category category = Item.Category.valueOf(row.category);
        LocalDate date = LocalDate.parse(row.date);
        ReportStatus status = ReportStatus.valueOf(row.status);
        if (Item.Type.FOUND.name().equals(row.type)) {
            return new FoundItem(row.id, row.reporterId, row.name, category, row.color, row.location,
                    date, row.description, row.photo, status);
        }
        return new LostItem(row.id, row.reporterId, row.name, category, row.color, row.location,
                date, row.description, row.photo, status);
    }

    @Override public void storeDetails(long itemId, FoundDetails details) {
        Secret row = new Secret();
        row.itemId = itemId;
        row.brand = details.brand();
        row.markers = details.markers();
        dao.insert(row);
    }

    @Override public FoundDetails detailsForReporter(long itemId, long reporterId) {
        Secret row = dao.secret(itemId, reporterId);
        return row == null ? null : new FoundDetails(row.brand, row.markers);
    }

    @Override public void updateStatus(long id, ReportStatus status) { dao.reportStatus(id, status.name()); }

    @Override public void appendHistory(long id, ReportStatus status, long time) {
        Event row = new Event();
        row.itemId = id;
        row.status = status.name();
        row.time = time;
        dao.insert(row);
    }

    @Override public List<StatusHistory> history(long id) {
        return dao.history(id).stream().map(row -> new StatusHistory(ReportStatus.valueOf(row.status), row.time))
                .collect(Collectors.toList());
    }

    @Override public long add(long itemId, long claimantId, String proof, String photo, long time) {
        Request row = new Request();
        row.itemId = itemId;
        row.claimantId = claimantId;
        row.proof = proof;
        row.photo = photo;
        row.status = Claim.Status.PENDING.name();
        row.submittedAt = time;
        return dao.insert(row);
    }

    @Override public Claim getClaim(long id) { return toClaim(dao.claim(id)); }

    @Override public List<Claim> byClaimant(long id) {
        return dao.claimsBy(id).stream().map(RoomRepositories::toClaim).collect(Collectors.toList());
    }

    @Override public List<Claim> claimsForReporter(long id) {
        return dao.claimsFor(id).stream().map(RoomRepositories::toClaim).collect(Collectors.toList());
    }

    private static Claim toClaim(Request row) {
        return row == null ? null : new Claim(row.id, row.itemId, row.claimantId, row.proof, row.photo,
                Claim.Status.valueOf(row.status), row.submittedAt);
    }

    @Override public boolean hasActive(long itemId, long claimantId) { return dao.active(itemId, claimantId); }
    @Override public boolean hasPending(long itemId) { return dao.pending(itemId); }
    @Override public void updateStatus(long id, Claim.Status status) { dao.claimStatus(id, status.name()); }
    @Override public void rejectOtherPending(long itemId, long approvedId) { dao.rejectOthers(itemId, approvedId); }

    @Override public boolean reserveCode(long claimId, String value) {
        Code row = new Code();
        row.claimId = claimId;
        row.value = value;
        return dao.insert(row) != -1;
    }

    @Override public PickupCode codeForClaimant(long claimId, long claimantId) {
        String value = dao.codeFor(claimId, claimantId);
        return value == null ? null : new PickupCode(value);
    }

    @Override public boolean consumeCode(long claimId, String value) { return dao.consume(claimId, value) == 1; }
}
