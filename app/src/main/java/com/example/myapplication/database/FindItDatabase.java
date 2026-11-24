package com.example.myapplication.database;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.RoomDatabase;

@Database(entities = {FindItDatabase.Account.class, FindItDatabase.Report.class,
        FindItDatabase.Secret.class, FindItDatabase.Request.class, FindItDatabase.Code.class,
        FindItDatabase.Event.class}, version = 1, exportSchema = false)
public abstract class FindItDatabase extends RoomDatabase {
    public abstract FindItDao dao();

    @Entity(tableName = "accounts", indices = @Index(value = "email", unique = true))
    public static class Account {
        @PrimaryKey(autoGenerate = true) public long id;
        @NonNull public String name = "";
        @NonNull public String email = "";
        @NonNull public String phone = "";
        @NonNull public String passwordHash = "";
    }

    @Entity(tableName = "reports", indices = @Index("reporterId"),
            foreignKeys = @ForeignKey(entity = Account.class, parentColumns = "id", childColumns = "reporterId"))
    public static class Report {
        @PrimaryKey(autoGenerate = true) public long id;
        public long reporterId;
        @NonNull public String name = "";
        @NonNull public String type = "";
        @NonNull public String category = "";
        @NonNull public String color = "";
        @NonNull public String location = "";
        @NonNull public String date = "";
        @NonNull public String description = "";
        @NonNull public String photo = "";
        @NonNull public String status = "";
    }

    @Entity(tableName = "found_details",
            foreignKeys = @ForeignKey(entity = Report.class, parentColumns = "id", childColumns = "itemId"))
    public static class Secret {
        @PrimaryKey public long itemId;
        @NonNull public String brand = "";
        @NonNull public String markers = "";
    }

    @Entity(tableName = "claims", indices = {@Index(value = {"itemId", "claimantId"}), @Index("claimantId")},
            foreignKeys = {
                    @ForeignKey(entity = Report.class, parentColumns = "id", childColumns = "itemId"),
                    @ForeignKey(entity = Account.class, parentColumns = "id", childColumns = "claimantId")})
    public static class Request {
        @PrimaryKey(autoGenerate = true) public long id;
        public long itemId;
        public long claimantId;
        @NonNull public String proof = "";
        @NonNull public String photo = "";
        @NonNull public String status = "";
        public long submittedAt;
    }

    @Entity(tableName = "pickup_codes", indices = @Index(value = "value", unique = true),
            foreignKeys = @ForeignKey(entity = Request.class, parentColumns = "id", childColumns = "claimId"))
    public static class Code {
        @PrimaryKey public long claimId;
        @NonNull public String value = "";
        public boolean used;
    }

    @Entity(tableName = "status_history", indices = @Index("itemId"),
            foreignKeys = @ForeignKey(entity = Report.class, parentColumns = "id", childColumns = "itemId"))
    public static class Event {
        @PrimaryKey(autoGenerate = true) public long id;
        public long itemId;
        @NonNull public String status = "";
        public long time;
    }
}
