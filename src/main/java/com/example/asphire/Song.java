package com.example.asphire;

import android.net.Uri;

public class Song {
    //members
    String title;
    Uri uri;
    Uri artworkUri;
    int size;
    int duration;

    //constructor

    public Song(Uri artworkUri, String title, Uri uri, int size, int duration) {
        this.artworkUri = artworkUri;
        this.title = title;
        this.uri = uri;
        this.size = size;
        this.duration = duration;
    }

    //getters

    public Uri getArtworkUri() {
        return artworkUri;
    }

    public String getTitle() {
        return title;
    }

    public Uri getUri() {
        return uri;
    }

    public int getSize() {
        return size;
    }

    public int getDuration() {
        return duration;
    }
}
