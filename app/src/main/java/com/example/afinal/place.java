package com.example.afinal;

public class place {
    private String name;
    private String id;
    private String uri;
    private int num;

    public place() {} // Firestore 需要無參構造器

    public place(String name, String id, String email, int num) {
        this.name = name;
        this.id = id;
        this.uri = email;
        this.num = num;
    }

}
