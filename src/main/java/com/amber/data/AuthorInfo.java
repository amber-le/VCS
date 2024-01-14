package com.amber.data;

public class AuthorInfo {
    String email;
    String name;

    public AuthorInfo(String email, String name) {
        this.email = email;
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "AuthorInfo{" +
                "email='" + email + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
