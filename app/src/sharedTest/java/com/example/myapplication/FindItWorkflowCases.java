package com.example.myapplication;

import android.content.Context;
import androidx.room.Room;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.myapplication.database.FindItDatabase;
import com.example.myapplication.database.RoomRepositories;
import com.example.myapplication.model.*;
import com.example.myapplication.repository.SessionStore;
import com.example.myapplication.service.*;
import com.example.myapplication.utils.PasswordHasher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import static org.junit.Assert.*;

public abstract class FindItWorkflowCases {
    private FindItDatabase database;
    private RoomRepositories storage;
    private ItemService items;
    private ClaimService claims;
    private HistoryService history;
    private HandoverService handover;
    private long finder;
    private long owner;
    private long other;

    @Before public void setup() {
        database = Room.inMemoryDatabaseBuilder(context(), FindItDatabase.class).allowMainThreadQueries().build();
        storage = new RoomRepositories(database);
        items = new ItemService(storage, storage);
        claims = new ClaimService(storage, storage, storage);
        history = new HistoryService(storage, storage);
        handover = new HandoverService(storage, storage, storage);
        finder = storage.register("Penemu Uji", "finder@kampus.test", "08123456780", "unused");
        owner = storage.register("Pemilik Uji", "owner@kampus.test", "08123456781", "unused");
        other = storage.register("Pengguna Uji", "other@kampus.test", "08123456782", "unused");
    }

    @After public void close() { database.close(); }

    @Test public void completeClaimRequiresProofReviewAndAOneTimeCode() {
        long report = found();
        expectValidation(() -> claims.submit(finder, report, "Laporan sendiri", ""));
        expectValidation(() -> claims.submit(owner, report, "", ""));
        long claim = claims.submit(owner, report, "Ada stiker di dalam", "");
        long competing = claims.submit(other, report, "Foto tanda pengenal", "");
        expectValidation(() -> claims.submit(owner, report, "Ulang", ""));
        assertEquals(ReportStatus.PENDING, items.detail(report).status());
        assertEquals(2, claims.incoming(finder).size());
        assertNull(claims.pickupCode(owner, claim));

        expectValidation(() -> claims.review(other, claim, true, true));
        expectValidation(() -> claims.review(finder, claim, true, false));
        assertNull(claims.pickupCode(owner, claim));
        claims.review(finder, claim, true, true);
        String code = claims.pickupCode(owner, claim).value();
        assertEquals(6, code.length());
        assertNull(claims.pickupCode(finder, claim));
        assertNull(claims.pickupCode(other, claim));
        assertEquals(Claim.Status.REJECTED, claims.view(other, competing).status());
        expectValidation(() -> claims.submit(other, report, "Coba lagi", ""));
        expectValidation(() -> handover.confirm(owner, claim, code));
        String wrong = (code.startsWith("A") ? "B" : "A") + code.substring(1);
        expectValidation(() -> handover.confirm(finder, claim, wrong));
        assertEquals(ReportStatus.PICKUP, items.detail(report).status());
        handover.confirm(finder, claim, code.toLowerCase(java.util.Locale.ROOT));
        assertEquals(ReportStatus.RETURNED, items.detail(report).status());
        assertEquals(Claim.Status.RETURNED, claims.view(owner, claim).status());
        assertNull(claims.pickupCode(owner, claim));
        expectValidation(() -> handover.confirm(finder, claim, code));

        List<ReportStatus> states = history.timeline(owner, report).stream()
                .map(StatusHistory::status).collect(Collectors.toList());
        assertEquals(Arrays.asList(ReportStatus.FOUND, ReportStatus.PENDING, ReportStatus.APPROVED,
                ReportStatus.PICKUP, ReportStatus.RETURNED), states);
    }

    @Test public void rejectLastClaimReopensReportAndLimitsPrivateAccess() {
        long report = found();
        assertEquals("Rahasia uji", items.secret(report, finder).markers());
        assertNull(items.secret(report, owner));
        assertNull(items.secret(report, other));
        assertTrue(items.browse("Rahasia uji", null, null, null).isEmpty());
        expectValidation(() -> history.timeline(other, report));
        long first = claims.submit(owner, report, "Bukti lama", "");
        expectValidation(() -> claims.view(other, first));
        claims.review(finder, first, false, false);
        assertEquals(Claim.Status.REJECTED, claims.view(owner, first).status());
        assertEquals(ReportStatus.FOUND, items.detail(report).status());
        assertNotEquals(first, claims.submit(owner, report, "Bukti tambahan", ""));
    }

    @Test public void searchAndFiltersCombineAndLostItemsCannotBeClaimed() {
        long found = found();
        long lost = items.report(owner, Item.Type.LOST, "Dompet kulit", Item.Category.ACCESSORIES,
                "Cokelat", "Kantin", LocalDate.now(), "Berisi kartu", "", "", "");
        assertEquals(found, items.browse("EARBUDS", Item.Type.FOUND, Item.Category.ELECTRONICS,
                ReportStatus.FOUND).get(0).id());
        assertEquals(found, items.browse("perpustakaan", null, null, null).get(0).id());
        assertTrue(items.browse("earbuds", Item.Type.LOST, null, null).isEmpty());
        assertTrue(items.browse("earbuds", null, Item.Category.ACCESSORIES, null).isEmpty());
        assertTrue(items.browse("earbuds", null, null, ReportStatus.RETURNED).isEmpty());
        assertEquals(lost, items.ownReports(owner).get(0).id());
        expectValidation(() -> claims.submit(finder, lost, "Bukti", ""));
    }

    @Test public void authenticationValidatesAndRestoresSession() {
        SessionStore session = new SessionStore() {
            private long current;
            @Override public long read() { return current; }
            @Override public void save(long userId) { current = userId; }
            @Override public void clear() { current = 0; }
        };
        AuthService auth = new AuthService(storage, session, new PasswordHasher());
        expectValidation(() -> auth.register("Nama", "email-salah", "08123456780", "sandi-aman", "sandi-aman"));
        User account = auth.register("Nama", "TEST@kampus.test", "08123456780", "sandi-aman", "sandi-aman");
        assertEquals("test@kampus.test", account.email());
        assertEquals(account.id(), auth.current().id());
        auth.logout();
        assertNull(auth.current());
        expectValidation(() -> auth.login("test@kampus.test", "sandi-salah"));
        assertEquals(account.id(), auth.login("test@kampus.test", "sandi-aman").id());
        auth.changePassword(account, "sandi-aman", "sandi-baru", "sandi-baru");
        expectValidation(() -> auth.login("test@kampus.test", "sandi-aman"));
        assertEquals(account.id(), auth.login("test@kampus.test", "sandi-baru").id());
    }

    @Test public void reportsPersistAfterReopeningDiskDatabase() {
        String name = "findit-test-" + java.util.UUID.randomUUID() + ".db";
        try {
            long id;
            FindItDatabase initial = Room.databaseBuilder(context(), FindItDatabase.class, name).allowMainThreadQueries().build();
            try {
                RoomRepositories rows = new RoomRepositories(initial);
                long user = rows.register("Penemu", "persist@kampus.test", "08123456780", "unused");
                id = new ItemService(rows, rows).report(user, Item.Type.LOST, "Buku", Item.Category.SUPPLIES,
                        "Biru", "Kantin", LocalDate.now(), "Buku PBO", "", "", "");
            } finally {
                initial.close();
            }

            FindItDatabase reopened = Room.databaseBuilder(context(), FindItDatabase.class, name).allowMainThreadQueries().build();
            try {
                RoomRepositories rows = new RoomRepositories(reopened);
                assertEquals("Buku", rows.get(id).name());
                assertEquals(ReportStatus.LOST, rows.history(id).get(0).status());
            } finally {
                reopened.close();
            }
        } finally {
            context().deleteDatabase(name);
        }
    }

    private long found() {
        return items.report(finder, Item.Type.FOUND, "Earbuds nirkabel", Item.Category.ELECTRONICS,
                "Hitam", "Perpustakaan", LocalDate.now(), "", "", "Merek uji", "Rahasia uji");
    }

    private static Context context() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    private static void expectValidation(Runnable action) {
        assertThrows(IllegalArgumentException.class, action::run);
    }
}
