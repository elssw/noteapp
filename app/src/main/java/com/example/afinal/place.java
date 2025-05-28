package com.example.afinal;

import java.util.List;

public class place {
    private String name;
    private String id;
    List<String> uri;
    private int num;

    public place() {} // Firestore 需要無參構造器

    public place(String name, String id, List<String> uri, int num) {
        this.name = name;
        this.id = id;
        this.uri = uri;
        this.num = num;
    }

}
