package com.example.afinal.model;

public class Record {
    private final int iconResId;
    private final String price;
    private final String note;
    private final String date;
    private final String categoryName;
    private final String location;

    public Record(int iconResId, String price, String note, String date, String categoryName, String location) {
        this.iconResId = iconResId;
        this.price = price;
        this.note = note;
        this.date = date;
        this.categoryName = categoryName;
        this.location = location;
    }

    public int getIconResId()        { return iconResId; }
    public String getPrice()         { return price; }
    public String getNote()          { return note; }
    public String getDate()          { return date; }
    public String getCategoryName()  { return categoryName; }
    public String getLocation()      { return location; }
}
