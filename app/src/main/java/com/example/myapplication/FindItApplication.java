package com.example.myapplication;

import android.app.Application;
import androidx.room.Room;
import com.example.myapplication.database.FindItDatabase;
import com.example.myapplication.database.RoomRepositories;
import com.example.myapplication.repository.SessionStore;
import com.example.myapplication.service.*;
import com.example.myapplication.utils.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class FindItApplication extends Application {
    public final ExecutorService worker = Executors.newSingleThreadExecutor();
    public AuthService auth;
    public ItemService items;
    public ClaimService claims;
    public HandoverService handover;
    public HistoryService history;
    private RoomRepositories storage;

    @Override public void onCreate() {
        super.onCreate();
        FindItDatabase database = Room.databaseBuilder(this, FindItDatabase.class, "findit.db").build();
        connect(database, new KeystoreSession(this));
    }

    void connect(FindItDatabase database, SessionStore sessionStore) {
        storage = new RoomRepositories(database);
        auth = new AuthService(storage, sessionStore, new PasswordHasher());
        items = new ItemService(storage, storage);
        claims = new ClaimService(storage, storage, storage);
        handover = new HandoverService(storage, storage, storage);
        history = new HistoryService(storage, storage);
    }

    public String displayName(long id) { return storage.displayName(id); }
    public String finderContact(long claimant, long claimId) {
        if (claims.pickupCode(claimant, claimId) == null) return "";
        long finder = items.detail(claims.view(claimant, claimId).itemId()).reporterId();
        return storage.profile(finder).phone();
    }
}
