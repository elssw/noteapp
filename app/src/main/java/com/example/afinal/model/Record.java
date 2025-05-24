package com.example.afinal.model;

public class Record {
    private final int iconResId;
    private final String price;
    private final String note;
    private final String time;

    public Record(int iconResId, String price, String note, String time) {
        this.iconResId = iconResId;
        this.price     = price;
        this.note      = note;
        this.time      = time;
    }

    public int getIconResId() { return iconResId; }
    public String getPrice()  { return price; }
    public String getNote()   { return note; }
    public String getTime()   { return time; }
}
