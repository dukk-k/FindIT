package com.example.myapplication.repository;

import com.example.myapplication.model.FoundDetails;
import com.example.myapplication.model.Item;
import com.example.myapplication.model.ReportStatus;
import com.example.myapplication.model.StatusHistory;
import java.util.List;

public interface ItemRepository {
    long add(Item item);
    Item get(long id);
    List<Item> browse(String keyword, Item.Type type, Item.Category category, ReportStatus status);
    List<Item> byReporter(long reporterId);
    void storeDetails(long itemId, FoundDetails details);
    FoundDetails detailsForReporter(long itemId, long reporterId);
    void updateStatus(long id, ReportStatus status);
    void appendHistory(long id, ReportStatus status, long time);
    List<StatusHistory> history(long id);
}
