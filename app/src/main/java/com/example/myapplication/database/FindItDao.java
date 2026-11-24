package com.example.myapplication.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.myapplication.database.FindItDatabase.*;
import com.example.myapplication.repository.UserRepository.Credentials;
import java.util.List;

@Dao
public interface FindItDao {
    final class Profile {
        public long id;
        public String name, email, phone;
    }

    @Query("SELECT id, name, email, phone FROM accounts WHERE id = :id")
    Profile profile(long id);

    @Query("SELECT name FROM accounts WHERE id = :id")
    String displayName(long id);

    @Query("SELECT id AS userId, passwordHash AS hash FROM accounts WHERE email = :email")
    Credentials credentials(String email);

    @Insert(onConflict = OnConflictStrategy.IGNORE) long register(Account account);

    @Query("UPDATE accounts SET name = :name, phone = :phone WHERE id = :id")
    void profile(long id, String name, String phone);

    @Query("UPDATE accounts SET passwordHash = :hash WHERE id = :id")
    void password(long id, String hash);

    @Insert long insert(Report report);

    @Query("SELECT * FROM reports WHERE id = :id")
    Report report(long id);

    @Query("SELECT * FROM reports WHERE (:keyword = '' OR instr(lower(name), :keyword) > 0 "
            + "OR instr(lower(color), :keyword) > 0 OR instr(lower(location), :keyword) > 0) "
            + "AND (:type IS NULL OR type = :type) AND (:category IS NULL OR category = :category) "
            + "AND (:status IS NULL OR status = :status) ORDER BY id DESC")
    List<Report> browse(String keyword, String type, String category, String status);

    @Query("SELECT * FROM reports WHERE reporterId = :id ORDER BY id DESC")
    List<Report> reportsBy(long id);

    @Insert void insert(Secret secret);

    @Query("SELECT s.* FROM found_details s JOIN reports r ON s.itemId = r.id "
            + "WHERE s.itemId = :itemId AND r.reporterId = :reporterId")
    Secret secret(long itemId, long reporterId);

    @Query("UPDATE reports SET status = :status WHERE id = :id")
    void reportStatus(long id, String status);

    @Insert void insert(Event event);

    @Query("SELECT * FROM status_history WHERE itemId = :id ORDER BY id")
    List<Event> history(long id);

    @Insert long insert(Request request);

    @Query("SELECT * FROM claims WHERE id = :id")
    Request claim(long id);

    @Query("SELECT * FROM claims WHERE claimantId = :id ORDER BY id DESC")
    List<Request> claimsBy(long id);

    @Query("SELECT c.* FROM claims c JOIN reports r ON c.itemId = r.id "
            + "WHERE r.reporterId = :id ORDER BY c.id DESC")
    List<Request> claimsFor(long id);

    @Query("SELECT EXISTS(SELECT 1 FROM claims WHERE itemId = :itemId AND claimantId = :claimantId "
            + "AND status IN ('PENDING', 'APPROVED'))")
    boolean active(long itemId, long claimantId);

    @Query("SELECT EXISTS(SELECT 1 FROM claims WHERE itemId = :itemId AND status = 'PENDING')")
    boolean pending(long itemId);

    @Query("UPDATE claims SET status = :status WHERE id = :id")
    void claimStatus(long id, String status);

    @Query("UPDATE claims SET status = 'REJECTED' WHERE itemId = :itemId "
            + "AND id != :approvedId AND status = 'PENDING'")
    void rejectOthers(long itemId, long approvedId);

    @Insert(onConflict = OnConflictStrategy.IGNORE) long insert(Code code);

    @Query("SELECT p.value FROM pickup_codes p JOIN claims c ON p.claimId = c.id "
            + "WHERE c.id = :claimId AND c.claimantId = :claimantId AND c.status = 'APPROVED' AND p.used = 0")
    String codeFor(long claimId, long claimantId);

    @Query("UPDATE pickup_codes SET used = 1 WHERE claimId = :claimId AND value = :value AND used = 0")
    int consume(long claimId, String value);
}
