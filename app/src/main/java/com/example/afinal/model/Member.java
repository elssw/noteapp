package com.example.afinal.model;

public class Member {
    private String email;
    private String nickname;

    public Member(String email, String nickname) {
        this.email = email;
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public String getNickname() {
        return nickname;
    }

    @Override
    public String toString() {
        return nickname; // 用於 UI 顯示
    }
}
