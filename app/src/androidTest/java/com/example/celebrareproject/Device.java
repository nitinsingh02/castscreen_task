package com.example.celebrareproject;

public class Device {
    public static final int TYPE_STANDARD = 0;
    public static final int TYPE_LARGE = 1;

    public int type;
    public String title;
    public String[] badges;
    public String description;
    public String imagePathOrDrawable;

    public Device(int type, String title, String[] badges, String description, String imagePathOrDrawable) {
        this.type = type;
        this.title = title;
        this.badges = badges;
        this.description = description;
        this.imagePathOrDrawable = imagePathOrDrawable;
    }
}
