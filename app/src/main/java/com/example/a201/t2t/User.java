package com.example.a201.t2t;

import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;

/**
 * A class represent user registered to system
 */
public class User {
    private String name;
    private String phone;
    private Uri imageUrl;
    private Uri thumbUrl;
    private Uri oldThumbUrl;
    private Bitmap image;
    private boolean isOnline;
    private boolean isGroup = false;

    /**
     * A constructor
     * @param name - user name
     * @param phone - user phone
     * @param thumbUrl - user thumbnail url
     */
    User(@NonNull String name, @NonNull String phone, String thumbUrl) {
        this.name = name;
        this.phone = phone;
        if(thumbUrl != null && !thumbUrl.isEmpty())
        {
            this.thumbUrl = Uri.parse(thumbUrl);
        }
    }

    /* --- Getters and Setters for all different private fields --- */
    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public Uri getThumbUrl() {
        return this.thumbUrl;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setThumbUrl(String thumbUrl) {
        this.thumbUrl = Uri.parse(thumbUrl);
    }

    public Uri getOldThumbUrl() {
        return oldThumbUrl;
    }

    public void setOldThumbUrl(String oldThumbUrl) {
        this.oldThumbUrl = Uri.parse(oldThumbUrl);
    }

    public Bitmap getImage() {
        return image;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    public Uri getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = Uri.parse(imageUrl);
    }

    public boolean isGroup() {
        return isGroup;
    }

    public void setGroup(boolean group) {
        isGroup = group;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public boolean isOnline() {
        return isOnline;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && ((User)obj).getPhone().equals(this.phone);
    }

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                ", imageUrl=" + imageUrl +
                ", thumbUrl=" + thumbUrl +
                ", oldThumbUrl=" + oldThumbUrl +
                ", image=" + image +
                ", isOnline=" + isOnline +
                ", isGroup=" + isGroup +
                '}';
    }
}
