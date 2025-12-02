package com.example.celebrareproject;

/**
 * Model for a supported device list item.
 * - title: device title
 * - chipLeft: text for chip1 (nullable)
 * - chipRight: text for chip2 (nullable)
 * - description: main text
 * - imageRes: drawable resource id (e.g. R.drawable.smart_tv). Use 0 for none.
 * - showButton: whether this item should show the action button (btnAction) in the layout
 */
public class Device {
    public final String title;
    public final String chipLeft;
    public final String chipRight;
    public final String description;
    public final int imageRes;
    public final boolean showButton;

    public Device(String title,
                  String chipLeft,
                  String chipRight,
                  String description,
                  int imageRes,
                  boolean showButton) {
        this.title = title;
        this.chipLeft = chipLeft;
        this.chipRight = chipRight;
        this.description = description;
        this.imageRes = imageRes;
        this.showButton = showButton;
    }
}
