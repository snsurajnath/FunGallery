package com.example.fungallery;

import android.net.Uri;

public class Image {
    Uri imageUri;

    public Image(Uri imageUri) {
        this.imageUri = imageUri;
    }

    public Uri getImageUri() {
        return imageUri;
    }

    public void setImageUri(Uri imageUri) {
        this.imageUri = imageUri;
    }
}
