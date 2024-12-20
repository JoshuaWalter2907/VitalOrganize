package com.springboot.vitalorganize.dto;

public class ProfileData {

    private final String name;
    private final String email;
    private final String photoUrl;

    public ProfileData(String name, String email, String photoUrl) {
        this.name = name;
        this.email = email;
        this.photoUrl = photoUrl;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }
}
