package com.project.guardianalertcapstone;

public class User {
    public String userId, name, email, phone;

    public User() {
        // Empty constructor required for Firebase
    }

    public User(String userId, String name, String email, String phone) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }
}
