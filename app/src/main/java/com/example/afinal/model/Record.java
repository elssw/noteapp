package com.example.afinal.model;

public class Record {
    private String docId;
    private final int iconResId;
    private final String price;
    private final String note;
    private final String date;
    private final String categoryName;
    private final String location;

    public Record(String docId, int iconResId, String price, String note, String date, String categoryName, String location) {
        this.docId = docId;
        this.iconResId = iconResId;
        this.price = price;
        this.note = note;
        this.date = date;
        this.categoryName = categoryName;
        this.location = location;
    }

    public String getDocId(){
        return docId;
    }
    public int getIconResId()        { return iconResId; }
    public String getPrice()         { return price; }
    public String getNote()          { return note; }
    public String getDate()          { return date; }
    public String getCategoryName()  { return categoryName; }
    public String getLocation()      { return location; }
}
