package com.example.asphire;

import static androidx.core.app.NotificationManagerCompat.IMPORTANCE_HIGH;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.MediaMetadata;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;

import java.util.Objects;

public class PlayerService extends Service {
    // Binder for clients to bind to the service
    private final IBinder serviceBinder = new ServiceBinder();

    // ExoPlayer instance and notification manager
     ExoPlayer player;
     PlayerNotificationManager notificationManager;

    public class ServiceBinder extends Binder {
        public PlayerService getPlayerService() {
            return PlayerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize ExoPlayer
        player = new ExoPlayer.Builder(getApplicationContext()).build();
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build();
        player.setAudioAttributes(audioAttributes, true);  // Handle audio focus

        // Initialize PlayerNotificationManager
        final String channelId = getResources().getString(R.string.app_name) + "Music Channel";
        final int notificationId = 1111111;

        notificationManager = new PlayerNotificationManager.Builder(this, notificationId, channelId)
                .setNotificationListener(notificationListener)
                .setMediaDescriptionAdapter(descriptionAdapter)
                .setChannelImportance(IMPORTANCE_HIGH)
                .setSmallIconResourceId(R.drawable.small_notification_icon)
                .setChannelDescriptionResourceId(R.string.app_name)
                .setNextActionIconResourceId(R.drawable.ic_skip_next)
                .setPreviousActionIconResourceId(R.drawable.ic_skip_previous)
                .setPauseActionIconResourceId(R.drawable.ic_pause)
                .setPlayActionIconResourceId(R.drawable.ic_play)
                .setChannelNameResourceId(R.string.app_name)
                .build();

        // Attach player to notification manager
        notificationManager.setPlayer(player);
        notificationManager.setPriority(NotificationCompat.PRIORITY_MAX);
        notificationManager.setUseRewindAction(false);
        notificationManager.setUseFastForwardAction(false);

        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "MUSIC_CHANNEL_ID",
                    "Music Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Channel for music playback notifications");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        if (player != null) {
            if (player.isPlaying()) player.stop();
            player.release();
            player = null;
        }
        notificationManager.setPlayer(null);
        stopForeground(true);
        stopSelf();
        super.onDestroy();
    }

    // Notification Listener to handle notification events
    private final PlayerNotificationManager.NotificationListener notificationListener =
            new PlayerNotificationManager.NotificationListener() {
                @Override
                public void onNotificationCancelled(int notificationId, boolean dismissedByUser) {
                    stopForeground(true);
                    if (player != null && player.isPlaying()) {
                        player.pause();
                    }
                }

                @Override
                public void onNotificationPosted(int notificationId, Notification notification, boolean ongoing) {
                    startForeground(notificationId, notification);
                }
            };

    // Media Description Adapter to display media info in the notification
    private final PlayerNotificationManager.MediaDescriptionAdapter descriptionAdapter =
            new PlayerNotificationManager.MediaDescriptionAdapter() {
                @Override
                public CharSequence getCurrentContentTitle(Player player) {
                    if (player == null || player.getCurrentMediaItem() == null ||
                            player.getCurrentMediaItem().mediaMetadata.title == null) {
                        return "Unknown Title";  // Fallback if no title is available
                    }
                    return player.getCurrentMediaItem().mediaMetadata.title;
                }

                @Nullable
                @Override
                public PendingIntent createCurrentContentIntent(Player player) {
                    Intent openAppIntent = new Intent(getApplicationContext(), MainActivity.class);
                    return PendingIntent.getActivity(
                            getApplicationContext(), 0, openAppIntent,
                            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                    );
                }

                @Nullable
                @Override
                public CharSequence getCurrentContentText(Player player) {
                    return "Enjoy your music";  // Optional additional text for the notification
                }

                @Nullable
                @Override
                public Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
                    ImageView view = new ImageView(getApplicationContext());

                    if (player != null && player.getCurrentMediaItem() != null &&
                            player.getCurrentMediaItem().mediaMetadata.artworkUri != null) {
                        view.setImageURI(player.getCurrentMediaItem().mediaMetadata.artworkUri);
                    } else {
                        view.setImageResource(R.drawable.asphire_logo);
                    }

                    BitmapDrawable bitmapDrawable = (BitmapDrawable) view.getDrawable();
                    if (bitmapDrawable == null) {
                        bitmapDrawable = (BitmapDrawable) ContextCompat.getDrawable(
                                getApplicationContext(), R.drawable.asphire_logo
                        );
                    }

                    assert bitmapDrawable != null;
                    return bitmapDrawable.getBitmap();
                }
            };
}
