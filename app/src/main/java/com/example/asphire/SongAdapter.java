package com.example.asphire;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.MediaMetadata;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    //memebers
    Context context;
    List<Song> songs;
    ExoPlayer player;
    ConstraintLayout playerView;

    //constructor


    public SongAdapter(Context context, List<Song> songs, ExoPlayer player, ConstraintLayout playerView) {
        this.context = context;
        this.songs = songs;
        this.player = player;
        this.playerView = playerView;
        prepareMediaItems();  // Prepare the media items once

    }

    private void prepareMediaItems() {
        List<MediaItem> mediaItems = new ArrayList<>();
        for (Song song : songs) {
            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(song.getUri())
                    .setMediaMetadata(getMetadata(song))
                    .build();
            mediaItems.add(mediaItem);
        }
        player.setMediaItems(mediaItems);  // Set all media items at once
        player.prepare();  // Prepare the player
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate song row item layout
        View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.song_row_item,parent,false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        // Get the current song
        Song song = songs.get(position);
        SongViewHolder viewHolder = (SongViewHolder) holder;

        // Set values to views
        viewHolder.titleHolder.setText(song.getTitle());
        viewHolder.durationHolder.setText(getDuration(song.getDuration()));
        viewHolder.sizeHolder.setText(getSize(song.getSize()));

        // Handle artwork
        Uri artworkUri = song.getArtworkUri();
        if (artworkUri != null) {
            viewHolder.artworkHolder.setImageURI(artworkUri);
        }

        // Play song on item click
        viewHolder.itemView.setOnClickListener(view -> {
            // Stop the current playback if needed
            if (player.isPlaying()) {
                player.stop();
            }

            // Seek to the selected song's position
            player.seekToDefaultPosition(position);
            player.play();
            playerView.setVisibility(View.VISIBLE);
            Toast.makeText(context, song.getTitle(), Toast.LENGTH_SHORT).show();
        });
    }

    private List<MediaItem> getMediaItems() {
        List<MediaItem> mediaItems = new ArrayList<>();
        for (Song song : songs){
            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(song.getUri())
                    .setMediaMetadata(getMetadata(song))
                    .build();

            //add the media
            mediaItems.add(mediaItem);
        }
        return mediaItems;
    }

    private MediaMetadata getMetadata(Song song) {
        return new MediaMetadata.Builder()
                .setTitle(song.getTitle())
                .setArtworkUri(song.getArtworkUri())
                .build();
    }


    //View Holder
    public static class SongViewHolder extends RecyclerView.ViewHolder{

        //Members
        ImageView artworkHolder;
        TextView titleHolder, durationHolder, sizeHolder;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);

            artworkHolder = itemView.findViewById(R.id.artworkView);
            titleHolder= itemView.findViewById(R.id.titleView);
            durationHolder=itemView.findViewById(R.id.durationView);
            sizeHolder=itemView.findViewById(R.id.sizeView);

        }
    }
    @Override
    public int getItemCount() {
        return songs.size();
    }

    //filter songs/ search results
    public void filterSongs(List<Song> filteredList) {
        this.songs.clear();
        this.songs.addAll(filteredList);  // 'songs' is the list in your adapter
        notifyDataSetChanged();  // Notifies the adapter to refresh the view with the new filtered data
    }

    private String getDuration(int totalDuration) {
        String totalDurationText;

        int hrs = totalDuration / (1000 * 60 * 60);
        int min = (totalDuration % (1000 * 60 * 60)) / (1000 * 60);
        int secs = (totalDuration % (1000 * 60)) / 1000;

        if (hrs < 1) {
            totalDurationText = String.format("%02d:%02d", min, secs);
        } else {
            totalDurationText = String.format("%d:%02d:%02d", hrs, min, secs);
        }
        return totalDurationText;
    }


    //size
    private String getSize(long bytes){
        String hrSize;

        double k= bytes/1024.0;
        double m= ((bytes/1024.0)/1024.0);
        double g= (((bytes/1024.0)/1024.0)/1024.0);
        double t= ((((bytes/1024.0)/1024.0)/1024.0)/1024.0);

        //the format
        DecimalFormat dec= new DecimalFormat("0.00");
        if (t > 1) {
            hrSize = dec.format(t).concat(" TB");
        } else if (g > 1) {
            hrSize = dec.format(g).concat(" GB");
        } else if (m > 1) {
            hrSize = dec.format(m).concat(" MB");
        } else if (k > 1) {
            hrSize = dec.format(k).concat(" KB");
        } else {
            hrSize = dec.format(bytes).concat(" Bytes");
        }

        return hrSize;
    }
}
