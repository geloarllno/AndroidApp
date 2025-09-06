package com.project.guardianalertcapstone;

public class NotificationModel {
    private String docId;
    private String timestamp;
    private String imagePath;
    private String base64Image;
    private boolean isRead;

    // Full constructor (used in Firestore reading)
    public NotificationModel(String docId, String timestamp, String imagePath, String base64Image, boolean isRead) {
        this.docId = docId;
        this.timestamp = timestamp;
        this.imagePath = imagePath;
        this.base64Image = base64Image;
        this.isRead = isRead;
    }

    // Short constructor (for manual/in-app usage)
    public NotificationModel(String timestamp, String imagePath, String base64Image) {
        this("", timestamp, imagePath, base64Image, false);
    }

    public String getDocId() {
        return docId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getBase64Image() {
        return base64Image;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}