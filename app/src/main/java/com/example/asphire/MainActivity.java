package com.example.asphire;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.PopupMenu;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PackageManagerCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.chibde.visualizer.BarVisualizer;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.firebase.auth.FirebaseAuth;
import com.jgabrielfreitas.core.BlurImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import de.hdodenhof.circleimageview.CircleImageView;


public class MainActivity extends AppCompatActivity {

    //members
    SwipeRefreshLayout swipeRefreshLayout;
    RecyclerView recyclerview;
    SongAdapter songAdapter;
    List<Song> allSongs = new ArrayList<>();
    private static final int STORAGE_PERMISSION_CODE = 1;
    ActivityResultLauncher<String> storagePermissionLauncher;
    final String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
    ExoPlayer player;
    ActivityResultLauncher<String> recordAudioPermissionLauncher; //To get access in the song adapter
    final String recordAudioPermission = Manifest.permission.RECORD_AUDIO;
    ConstraintLayout playerView;
    TextView playerCloseBtn;
    private MediaPlayer mediaPlayer;
    private float[] speedOptions = {1.0f, 1.5f, 2.0f, 0.5f};  // Array of speeds
    private int currentSpeedIndex = 0;  // Track the current speed
    //wrappers
    ConstraintLayout homeControlWrapper, headWrapper, artworkWrapper, seekbarWrapper, controlWrapper, audioVisualizerWrapper; //artwork
    CircleImageView artworkView;
    //seek bar
    SeekBar seekbar;
    TextView progressView, durationView;
    //audio visualizer
    BarVisualizer audioVisualizer;
    //blur inage view
    BlurImageView blurImageView;
    //status bar & navigation color;
    int defaultStatusColor;
    //repeat mode
    int repeatMode = 1; //repeat all 1, repeat one #2, shuffle all = 3
    SearchView searchView;
    //is the act. bound?
    boolean isBound = false;
    //controls
    TextView songNameView,skipPreviousBtn,playPauseBtn,skipNextBtn,repeatModeBtn,changePlaybackSpeed, playlistBtn;
    TextView homeSongNameView,homeSkipPreviousBtn,homePlayPauseBtn,homeSkipNextBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize permission launchers
        recordAudioPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted && player.isPlaying()) {
                        activateAudioVisualizer();
                    } else {
                        userResponsesOnRecordAudioPerm();
                    }
                }
        );

        storagePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        // Handle the permission granted case
                        new LoadSongsTask().execute();
                    } else {
                        // Handle the permission denied case
//                        Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Request the permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            } else {
                // Permission already granted, proceed with your logic
                startPlayerService();
            }
        } else {
            // For devices below Android 13, no need to request permission
            startPlayerService();
        }

        // Initialize the SwipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        recyclerview = findViewById(R.id.recyclerview);
        recyclerview.setLayoutManager(new LinearLayoutManager(this));

        // Set up the refresh listener
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Call the method to refresh the song list
            refreshSongList();
        });

        // Load songs initially
        loadSongsAfterRefresh();


        //save the status color
        defaultStatusColor = getWindow().getStatusBarColor();
        //set the navigation color
        getWindow().setNavigationBarColor(ColorUtils.setAlphaComponent(defaultStatusColor,199)); //0 & 255

        //set the tool bar, and app title
        Toolbar toolbar= findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.app_name));


        // Check and request storage permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11 and above
            if (!android.os.Environment.isExternalStorageManager()) {
                requestManageExternalStoragePermission();
            } else {
                new LoadSongsTask().execute();
            }
        } else {
            // For Android 10 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
            } else {
                new LoadSongsTask().execute();
            }
        }

        recordAudioPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted && player.isPlaying()) {
                activateAudioVisualizer();
            } else {
                userResponsesOnRecordAudioPerm();
            }
        });


        //views
        //player = new ExoPlayer.Builder(this).build();
        playerView = findViewById(R.id.playerView);
        playerCloseBtn = findViewById(R.id.playerCloseBtn);
        songNameView = findViewById(R.id.songNameView);
        skipPreviousBtn = findViewById(R.id.skipPreviousBtn);
        skipNextBtn = findViewById(R.id.skipNextBtn);
        playPauseBtn = findViewById(R.id.playPauseBtn);
        repeatModeBtn = findViewById(R.id.repeatModeBtn);
        changePlaybackSpeed=findViewById(R.id.speedControlBtn);
        playlistBtn = findViewById(R.id.playlistBtn);

// Wrappers
        homeSongNameView = findViewById(R.id.homeSongNameView);
        homeSkipPreviousBtn = findViewById(R.id.homeSkipPreviousBtn);
        homeSkipNextBtn = findViewById(R.id.homeSkipNextBtn);
        homePlayPauseBtn = findViewById(R.id.homePlayPauseBtn);

        homeControlWrapper = findViewById(R.id.homeControlWrapper);
        headWrapper = findViewById(R.id.headWrapper);
        artworkWrapper = findViewById(R.id.artworkWrapper);
        controlWrapper = findViewById(R.id.controlWrapper);
        audioVisualizerWrapper = findViewById(R.id.audioVisualizerWrapper);

// Artwork
        artworkView = findViewById(R.id.artworkView);
        seekbar = findViewById(R.id.seekbar);
        progressView = findViewById(R.id.progressView);
        durationView = findViewById(R.id.durationView);

// Audio Visualizer
        audioVisualizer = findViewById(R.id.visualizer);
// Blue image view
        blurImageView = findViewById(R.id.blurImageView);

        // Add playback speed control button
        TextView speedControlBtn = findViewById(R.id.speedControlBtn); // Ensure this ID is in your layout

        // Set up the listener for the home play/pause button
        homePlayPauseBtn.setOnClickListener(view -> {
            Log.d("HomeControl", "Home play/pause button pressed");

            if (player.isPlaying()) {
                player.pause();
                homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play, 0, 0, 0);
                playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_circle_outline, 0, 0, 0);
            } else {
                player.play();
                homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause, 0, 0, 0);
                playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_circle_outline, 0, 0, 0);
            }
        });
        // Add listener for the speed control button
        speedControlBtn.setOnClickListener(v -> {
            // Cycle to the next speed option
            currentSpeedIndex = (currentSpeedIndex + 1) % speedOptions.length;
            float speed = speedOptions[currentSpeedIndex];

            // Set playback speed on the player
            player.setPlaybackParameters(new PlaybackParameters(speed));

        });
        //bind to the player service, and do everything after the binding
        doBindService();

        Button optionsButton = findViewById(R.id.optionsButton);
        optionsButton.setOnClickListener(v -> showOptionsMenu(v));

        player = new ExoPlayer.Builder(this).build();

    }

    private void startPlayerService() {
        // Start your PlayerService or any other logic
        Intent serviceIntent = new Intent(this, PlayerService.class);
        startService(serviceIntent);
    }

    private void loadSongsAfterRefresh() {
        // Load songs from storage (implement this method)
        new LoadSongsTask().execute();
    }

    private void refreshSongList() {
        // Store the current playback position
        int currentPosition = (int) player.getCurrentPosition();
        MediaItem currentMediaItem = player.getCurrentMediaItem();

        // Call the LoadSongsTask with parameters
        new LoadSongsTask(currentPosition, currentMediaItem).execute();
    }


    private void doBindService() {
        Intent playerServiceIntent = new Intent(this, PlayerService.class);
        bindService(playerServiceIntent, playerServiceConnection, Context.BIND_AUTO_CREATE);
        isBound= true;
    }
    ServiceConnection playerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            //get the service instance
            PlayerService.ServiceBinder  binder = (PlayerService.ServiceBinder)  iBinder;
            player = binder.getPlayerService().player;
            isBound=true;
            //ready to show songs
            storagePermissionLauncher.launch(permission);
            //call player control method
            playerControls();

            // Only launch storage permission if the launcher is initialized
            if (recordAudioPermissionLauncher != null) {
                storagePermissionLauncher.launch(permission);
            } else {
                Log.e("MainActivity", "recordAudioPermissionLauncher is not initialized.");
            }

            if (storagePermissionLauncher != null) {
                storagePermissionLauncher.launch(permission);
            } else {
                Log.e("MainActivity", "storagePermissionLauncher is not initialized.");
            }

            storagePermissionLauncher.launch(permission);
            new LoadSongsTask().execute(); // This will now use the default constructor
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    private void showOptionsMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.options_menu, popupMenu.getMenu());

        // Get the user's email from the intent
        String userEmail = getIntent().getStringExtra("USER_EMAIL");
        if (userEmail != null && !userEmail.isEmpty()) {
            popupMenu.getMenu().findItem(R.id.user_email).setTitle(userEmail);
        } else {
            popupMenu.getMenu().findItem(R.id.user_email).setTitle("Guest");
        }

        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.logout) {
                logoutUser ();
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

    private void logoutUser () {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.removeExtra("USER_EMAIL"); // Clear the email extra
        startActivity(intent);
        finish();
    }



    @Override
    public void onBackPressed() {
        if (playerView.getVisibility() == View.VISIBLE) {
            Log.d("MainActivity", "Exiting Player View");
            exitPlayerView();
        } else {
            Log.d("MainActivity", "Calling super onBackPressed");
            super.onBackPressed();
        }
    }

    private void playerControls() {
        //song name marquee
        songNameView.setSelected(true);
        homeSongNameView.setSelected(true);

        //exit the player view
        playerCloseBtn.setOnClickListener(view -> exitPlayerView());
        playlistBtn.setOnClickListener(view -> exitPlayerView());
        //open player view on home control wrapper click
        homeControlWrapper.setOnClickListener(view -> showPlayerView());

        //player listener
        player.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                Player.Listener.super.onMediaItemTransition(mediaItem, reason);
                //show the song name
                songNameView.setText(mediaItem.mediaMetadata.title);
                homeSongNameView.setText(mediaItem.mediaMetadata.title);

                progressView.setText(getReadableTime((int) player.getCurrentPosition()));
                seekbar.setProgress((int) player.getCurrentPosition());
                seekbar.setMax((int) player.getDuration());

                durationView.setText(getReadableTime((int) player.getDuration()));
                playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_circle_outline, 0, 0, 0);
                homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause, 0, 0, 0);

                //show the current artwork
                showCurrentArtwork();

                //seekbar controller
                seekBarController();

                //update the progress position of a current playing song
                updatePlayerPositionProgress();
                //load the artwork animation
                artworkView.setAnimation(loadRotation());

                //show the audio visualizer
                activateAudioVisualizer();
                //update player view colors
                updatePlayerColors();

//                if(!player.isPlaying()){
//                    player.play();
//                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                Player.Listener.super.onPlaybackStateChanged(playbackState);
                if(playbackState == ExoPlayer.STATE_READY){
                    songNameView.setText(Objects.requireNonNull(player.getCurrentMediaItem()).mediaMetadata.title);
                    homeSongNameView.setText(player.getCurrentMediaItem().mediaMetadata.title);
                    progressView.setText(getReadableTime((int) player.getCurrentPosition()));
                    durationView.setText(getReadableTime((int) player.getDuration()));
                    seekbar.setMax((int) player.getDuration());
                    seekbar.setProgress((int) player.getCurrentPosition());
                    playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_circle_outline, 0, 0, 0);
                    homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause, 0, 0, 0);

                    //show the current artwork
                    showCurrentArtwork();

                    //update the progress position of a current playing song
                    updatePlayerPositionProgress();
                    //load the artwork animation
                    artworkView.setAnimation(loadRotation());

                    //show the audio visualizer
                    activateAudioVisualizer();
                    //update player view colors
                    updatePlayerColors();
                }
                else{
                    playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_circle_outline, 0, 0, 0);
                    homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play, 0, 0, 0);
                }
            }
        });

        //skip to next track
        skipNextBtn.setOnClickListener(view -> skipToNextSong());
        homeSkipNextBtn.setOnClickListener(view -> skipToNextSong());

        //skip to previous track
        skipPreviousBtn.setOnClickListener(view -> skipToPreviousSong());
        homeSkipPreviousBtn.setOnClickListener(view -> skipToPreviousSong());

        //play and pause the player
        playPauseBtn.setOnClickListener(view -> playOrPausePlayer());

    }

    private void playOrPausePlayer() {
        Log.d("MainActivity", "Play/Pause Button Pressed");

        if (player.isPlaying()) {
            player.pause();
            playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_circle_outline, 0, 0, 0);
            homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play, 0, 0, 0);
            artworkView.clearAnimation();
        } else {
            player.play();
            playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_circle_outline, 0, 0, 0);
            homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause, 0, 0, 0);
            artworkView.startAnimation(loadRotation());
        }
        // Set up the listener for the home play/pause button
        homePlayPauseBtn.setOnClickListener(view -> {
            Log.d("HomeControl", "Home play/pause button pressed");

            if (player.isPlaying()) {
                player.pause();
                homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play, 0, 0, 0);
                playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_circle_outline, 0, 0, 0);
            } else {
                player.play();
                homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause, 0, 0, 0);
                playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_circle_outline, 0, 0, 0);
            }
        });

        //update player colors
        updatePlayerColors();
    }

    private void skipToNextSong() {
        Log.d("MainActivity", "Skip Next Button Clicked");
        if (player.hasNextMediaItem()) {
            player.seekToNext();  // This should work if the media items are correctly set
            player.play();  // Ensure the player starts playing after skipping
            updateUIAfterSkip();  // Update UI after skipping
        } else {
            Log.d("MainActivity", "No next media item available.");
        }
    }

    private void updateUIAfterSkip() {
        // Update song name, progress, artwork, etc.
        songNameView.setText(Objects.requireNonNull(player.getCurrentMediaItem()).mediaMetadata.title);
        progressView.setText(getReadableTime((int) player.getCurrentPosition()));
        durationView.setText(getReadableTime((int) player.getDuration()));
        seekbar.setProgress((int) player.getCurrentPosition());
        seekbar.setMax((int) player.getDuration());
        showCurrentArtwork();
    }

    private void skipToPreviousSong() {
        Log.d("MainActivity", "Skip Previous Button Clicked");
        if (player.hasPreviousMediaItem()) {
            player.seekToPrevious();  // This should work if the media items are correctly set
            player.play();  // Ensure the player starts playing after skipping
            updateUIAfterSkip();  // Update UI after skipping
        } else {
            Log.d("MainActivity", "No previous media item available.");
        }
    }

    private void seekBarController() {
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Only seek the player when the change is initiated by the user
                if (fromUser) {
                    progressView.setText(getReadableTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Optional: You can pause the player when the user starts dragging
                player.pause();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Seek to the position the user dragged to when they stop moving the SeekBar
                player.seekTo(seekBar.getProgress());
                // Optional: Resume the player after seeking
                if (!player.isPlaying()) {
                    player.play();
                }
            }
        });
        //repeat mode
        repeatModeBtn.setOnClickListener(view -> {
            if (repeatMode ==1 ){
                //repat one
                player.setRepeatMode(ExoPlayer.REPEAT_MODE_ONE);
                repeatMode=2;
                repeatModeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_repeat_one,0,0,0);
            }
            else if(repeatMode == 2) {
                //shuffle all
                player.setShuffleModeEnabled(true);
                player.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL);
                repeatMode=3;
                repeatModeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_shuffle,0,0,0);
            }
            else if (repeatMode ==3){
                //repeat all
                player.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL);
                player.setShuffleModeEnabled(false);
                repeatMode=1 ;
                repeatModeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_repeat_all, 0,0,0);
            }
            //update colors
            updatePlayerColors();
            });
    }

    private void updatePlayerPositionProgress() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (player.isPlaying()) {
                    progressView.setText(getReadableTime((int) player.getCurrentPosition()));
                    seekbar.setProgress((int) player.getCurrentPosition());
                }
                // repeat calling method
                updatePlayerPositionProgress();
            }
        }, 1000); // Adjust delay as needed, 1000 means every 1 second
    }


private Animation loadRotation() {
    RotateAnimation rotateAnimation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
    rotateAnimation.setInterpolator(new LinearInterpolator());
    rotateAnimation.setRepeatCount(Animation.INFINITE);
    rotateAnimation.setDuration(10000);
    return rotateAnimation;
}

private void showCurrentArtwork() {
        artworkView.setImageURI(player.getCurrentMediaItem().mediaMetadata.artworkUri);

        if(artworkView.getDrawable() == null){
            artworkView.setImageResource(R.drawable.asphire_logo);
        }
    }

    String getReadableTime(int duration) {
        String time;
        int hrs = duration / (1000 * 60 * 60);
        int min = (duration % (1000 * 60 * 60)) / (1000 * 60);
        int secs = ((duration % (1000 * 60 * 60)) % (1000 * 60)) / 1000;

        if (hrs < 1) {
            time = min + ":" + secs;
        } else {
            time = hrs + ":" + min + ":" + secs;
        }
        return time;
    }


    private void showPlayerView() {
        playerView.setVisibility(View.VISIBLE);
        updatePlayerColors();
    }

    private void updatePlayerColors() {

        //only player view is visible
        if (playerView.getVisibility() == View.GONE)
            return;

        BitmapDrawable bitmapDrawable=(BitmapDrawable) artworkView.getDrawable();
        if (bitmapDrawable == null){
            bitmapDrawable= (BitmapDrawable) ContextCompat.getDrawable(this, R.drawable.default_artwork_background);
        }
        assert bitmapDrawable!=null;
        Bitmap bmp = bitmapDrawable.getBitmap();
        //set bitmap to blur imageview
        blurImageView.setImageBitmap(bmp);
        blurImageView.setBlur(4);

        //player control colors
        Palette.from(bmp).generate(palette -> {
            if (palette !=null){
                Palette.Swatch swatch= palette.getDarkVibrantSwatch();
                if (swatch==null){
                    swatch=palette.getMutedSwatch();
                    if (swatch == null){
                        swatch = palette.getDominantSwatch();
                    }
                }
                //extract text colors
                assert swatch!=null;
                int titleTextColor = swatch.getTitleTextColor();
                int bodyTextColor = swatch.getBodyTextColor();
                int rgbColor = swatch.getRgb();

                //set colors to player views
                //status & navigation bar colors
                getWindow().setStatusBarColor(rgbColor);
                getWindow().setNavigationBarColor(rgbColor);

                //more view colors
                songNameView.setTextColor(titleTextColor);
                playerCloseBtn.getCompoundDrawables()[0].setTint(titleTextColor);
                progressView.setTextColor(bodyTextColor);
                durationView.setTextColor(bodyTextColor);

                changePlaybackSpeed.getCompoundDrawables()[0].setTint(bodyTextColor);
                repeatModeBtn.getCompoundDrawables()[0].setTint(bodyTextColor);
                skipPreviousBtn.getCompoundDrawables()[0].setTint(bodyTextColor);
                skipNextBtn.getCompoundDrawables()[0].setTint(bodyTextColor);
                playPauseBtn.getCompoundDrawables()[0].setTint(titleTextColor);
                playlistBtn.getCompoundDrawables()[0].setTint(bodyTextColor);

            }
        });
    }

    private void exitPlayerView() {
        playerView.setVisibility(View.GONE);
        getWindow().setStatusBarColor(defaultStatusColor);
        getWindow().setNavigationBarColor(ColorUtils.setAlphaComponent(defaultStatusColor,199));
    }

    private void userResponsesOnRecordAudioPerm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(recordAudioPermission)) {
                // Show an educational UI explaining why we need this permission
                // Use alert dialog
                new AlertDialog.Builder(this)
                        .setTitle("Requesting to show Audio Visualizer")
                        .setMessage("Allow this app to display audio visualizer when music is playing")
                        .setPositiveButton("Allow", (dialogInterface, i) -> {
                            // Request the permission
                            recordAudioPermissionLauncher.launch(recordAudioPermission);
                        })
                        .setNegativeButton("No", (dialogInterface, i) -> {
                            Toast.makeText(getApplicationContext(), "You denied to show the audio visualizer", Toast.LENGTH_SHORT).show();
                            dialogInterface.dismiss();
                        })
                        .show();
            }
        }
        else{
            Toast.makeText(getApplicationContext(), "You denied to show the audio visualizer", Toast.LENGTH_SHORT).show();
        }
    }


    //audio visualizer
    private void activateAudioVisualizer() {
        //check if we have record audio permission to show an audio visualizer
        if (ContextCompat.checkSelfPermission(this, recordAudioPermission)!= PackageManager.PERMISSION_GRANTED){
            return;
        }
        //set color to  audio visualizer
        audioVisualizer.setColor(ContextCompat.getColor(this,R.color.secondary_color));
        //set number of visualizer btn 10 & 256
        audioVisualizer.setDensity(10);
        //set the audio session id from the player
        audioVisualizer.setPlayer(player.getAudioSessionId());
    }

    protected void onDestroy(){
        super.onDestroy();
        //release the player
//        if(player.isPlaying()){
//            player.stop();
//        }
//        player.release();
        doUnbindService();
    }

    private void doUnbindService() {
        if (isBound){
            unbindService(playerServiceConnection);
            isBound=false;
        }
    }


    // Request permission result callback
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, load songs
                new LoadSongsTask().execute();
            } else {
                // Permission denied
//                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class LoadSongsTask extends AsyncTask<Void, Void, List<Song>> {
        private final int currentPosition;
        private final MediaItem currentMediaItem;

        // Default constructor
        LoadSongsTask() {
            this.currentPosition = 0; // Default value
            this.currentMediaItem = null; // Default value
        }

        LoadSongsTask(int currentPosition, MediaItem currentMediaItem) {
            this.currentPosition = currentPosition;
            this.currentMediaItem = currentMediaItem;
        }

        @Override
        protected List<Song> doInBackground(Void... voids) {
            return loadSongs();
        }

        @Override
        protected void onPostExecute(List<Song> songs) {
            if (songs != null) {
                allSongs.clear();
                allSongs.addAll(songs);
                songAdapter = new SongAdapter(MainActivity.this, songs, player, playerView);
                recyclerview.setAdapter(songAdapter);

                // Restore the playback position if the same song is still in the list
                if (currentMediaItem != null) {
                    int index = findSongIndex(currentMediaItem);
                    if (index != -1) {
                        player.seekTo(index, currentPosition);
                    }
                }
            } else {
                Toast.makeText(MainActivity.this, "Failed to load songs", Toast.LENGTH_SHORT).show();
            }
            swipeRefreshLayout.setRefreshing(false); // Stop the refreshing animation
        }

        private int findSongIndex(MediaItem mediaItem) {
            // Logic to find the index of the current song in the new song list
            for (int i = 0; i < allSongs.size(); i++) {
                if (allSongs.get(i).getUri().equals(mediaItem.mediaId)) {
                    return i;
                }
            }
            return -1; // Return -1 if the song is not found
        }
    }


    // Method to fetch songs from external storage
    private List<Song> loadSongs() {
        List<Song> songList = new ArrayList<>();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA, // File path
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.ALBUM_ID
        };

        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                int duration = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                long size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE));
                Uri artworkUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"),
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)));

                Song song = new Song(artworkUri, title, Uri.parse(path), (int) size, duration);
                songList.add(song);
            }
            cursor.close();
        }

        return songList;

    }

    private void showSongs(List<Song> songs) {
        if (songs.size()==0){
            Toast.makeText(this, "No songs ", Toast.LENGTH_SHORT).show();
            return;

        }
        //save songs
        allSongs.clear();
        allSongs.addAll(songs);

        //update the tool bar title
        String title = getResources().getString(R.string.app_name)+  " - " + songs.size();
        Objects.requireNonNull(getSupportActionBar()).setTitle(title);

        //layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerview.setLayoutManager(layoutManager);

        //songs adapter
        songAdapter = new SongAdapter(this, songs, player, playerView);
        //set the adapter to recyclerview
        recyclerview.setAdapter(songAdapter);


        }
    //setting the menu/search btn

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_btn,menu);

        //search btn item
        MenuItem menuItem= menu.findItem(R.id.searchBtn);
        searchView= (SearchView) menuItem.getActionView();

        //search song method
        SearchSong(searchView);
        return super.onCreateOptionsMenu(menu);
    }

    private void SearchSong(SearchView searchView) {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;  // We handle real-time filtering with text changes, so this can return false
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterSongs(newText.toLowerCase());  // Pass the query to filterSongs()
                return true;
            }
        });
    }


    private void filterSongs(String query) {
        // Ensure songs are loaded before filtering
        if (songAdapter == null || allSongs.isEmpty()) {
            Log.d("Filter", "Song adapter is not initialized or no songs loaded");
            return; // Exit if adapter isn't ready
        }

        List<Song> filteredList = new ArrayList<>();

        for (Song song : allSongs) {
            if (song.getTitle().toLowerCase().contains(query)) {
                filteredList.add(song);
            }
        }

        // Update the adapter with the filtered songs
        songAdapter.filterSongs(filteredList);
    }

    // Method to request Manage External Storage Permission for Android 11 and above
    private void requestManageExternalStoragePermission() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Manage External Storage Permission");
        builder.setMessage("This app needs permission to manage external storage in order to display songs.");
        builder.setPositiveButton("Grant", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }


}