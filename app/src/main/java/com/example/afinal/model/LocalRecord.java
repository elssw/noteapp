package com.example.afinal.model;

import java.util.List;

public class LocalRecord {
    public String id;
    public String amount;
    public String note;
    public String categoryName;
    public String location;
    public String date;
    public int iconRes;
    public List<String> imageUris;

    public LocalRecord(String id, int iconRes, String amount, String note, String date, String categoryName, String location, List<String> imageUris) {
        this.id = id;
        this.iconRes = iconRes;
        this.amount = amount;
        this.note = note;
        this.date = date;
        this.categoryName = categoryName;
        this.location = location;
        this.imageUris = imageUris;
    }

    public LocalRecord() {}
}
