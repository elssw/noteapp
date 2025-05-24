package com.example.afinal.model;

public class Category {
    private final int iconResId;
    private final String name;

    public Category(int iconResId, String name) {
        this.iconResId = iconResId;
        this.name = name;
    }
    public int getIconResId() {
        return iconResId;
    }
    public String getName()    {
        return name;
    }
}
