package com.example.myapplication.service;

import com.example.myapplication.model.*;
import com.example.myapplication.repository.ItemRepository;
import com.example.myapplication.repository.TransactionRunner;
import java.time.LocalDate;
import java.util.List;
import static com.example.myapplication.service.Validation.*;

public final class ItemService {
    private final ItemRepository items;
    private final TransactionRunner transactions;

    public ItemService(ItemRepository items, TransactionRunner transactions) {
        this.items = items;
        this.transactions = transactions;
    }

    public long report(long reporter, Item.Type type, String name, Item.Category category, String color,
                       String location, LocalDate date, String description, String photo, String brand, String markers) {
        String title = required(name, 100, "Nama barang");
        String tone = required(color, 60, "Warna");
        String place = required(location, 120, "Lokasi");
        require(date != null && !date.isAfter(LocalDate.now()), "Tanggal tidak boleh di masa depan.");
        require(category != null, "Pilih kategori barang.");
        boolean found = type == Item.Type.FOUND;
        String publicText = found ? text(description, 1000, "Deskripsi umum")
                : required(description, 1000, "Deskripsi");
        FoundDetails secret = found ? new FoundDetails(text(brand, 100, "Merek"),
                required(markers, 1000, "Ciri khusus")) : null;
        ReportStatus status = found ? ReportStatus.FOUND : ReportStatus.LOST;
        Item item = found ? new FoundItem(0, reporter, title, category, tone, place, date, publicText, photo, status)
                : new LostItem(0, reporter, title, category, tone, place, date, publicText, photo, status);

        return transactions.run(() -> {
            long id = items.add(item);
            if (secret != null) items.storeDetails(id, secret);
            items.appendHistory(id, item.status(), System.currentTimeMillis());
            return id;
        });
    }

    public Item detail(long id) {
        Item item = items.get(id);
        require(item != null, "Laporan tidak ditemukan.");
        return item;
    }

    public List<Item> browse(String search, Item.Type type, Item.Category category, ReportStatus status) {
        return items.browse(text(search, 120, "Pencarian"), type, category, status);
    }

    public List<Item> ownReports(long user) { return items.byReporter(user); }
    public FoundDetails secret(long item, long user) { return items.detailsForReporter(item, user); }
}
