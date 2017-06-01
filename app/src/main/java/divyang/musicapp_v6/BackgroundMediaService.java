package divyang.musicapp_v6;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.service.media.MediaBrowserService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

import java.util.List;

public class BackgroundMediaService extends MediaBrowserService implements
        AudioManager.OnAudioFocusChangeListener
{
    /*
    Functions Call Tree -->
        onCreate()
        onStartCommand
        onGetRoot
    */

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid,
                                 @Nullable Bundle rootHints)
    {
        //onGetRoot() controls access to the service.
        if(TextUtils.equals(clientPackageName, getPackageName()))
            return  new BrowserRoot(getString(R.string.app_name),null);
        return null;
    }

    @Override
    public void onLoadChildren(@NonNull String parentId,
                               @NonNull Result<List<MediaBrowser.MediaItem>> result)
    {
        // onLoadChildren() provides the ability for a client to build and
        // display a menu of the MediaBrowserService's content hierarchy.
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if( intent != null && intent.getExtras() != null )
        {
            Log.i("Check","Checking data from delete intent.");
            Bundle intentData = intent.getExtras();
            Log.i("Check",String.valueOf(intentData.getInt("deleteKey@991")));
            if( intentData.getInt("deleteKey@991") == 991 )
                stopSelf();
        }
        handleIntent(mediaSession,intent);
        return super.onStartCommand(intent, flags, startId);
    }

    //Declaration Of Variables.
    int songPos;
    final int PAUSE_NOTIFICATION = 12;
    final int PLAY_NOTIFICATION = 12;
    final int PLAY_STATE = 1;
    final int PAUSE_STATE = 0;
    public static MediaPlayer mediaPlayer;
    private MediaSession mediaSession;
    private PlaybackState.Builder playbackstateBuilder;

    //Noisy Receiver.
    private BroadcastReceiver headPhoneReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if( mediaPlayer != null && mediaPlayer.isPlaying() )
            {
                mediaPlayer.pause();
                showPausedNotification();
            }
        }
    };

    @Override
    public void onLowMemory()
    {
        stopForeground(true);
        stopSelf();
        super.onLowMemory();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent)
    {
        Log.i("Check","App removed from memory");
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        initMediaSession();
        initMediaPlayer();
        initNoisyReceiver();
    }

    private void initMediaPlayer()
    {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setVolume(1.0f, 1.0f);
    }

    private void initMediaSession()
    {
        // Create a MediaSession.
        mediaSession = new MediaSession(getApplicationContext(), "Tag");

        // MySessionCallback() has methods that handle callbacks from a media controller
        mediaSession.setCallback(mediaSessionCallback);

        // Enable callbacks from MediaButtons and TransportControls
        mediaSession.setFlags(
                MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS );

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        playbackstateBuilder = new PlaybackState.Builder();
        playbackstateBuilder.setActions( PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PLAY_PAUSE |
                PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_SKIP_TO_NEXT |
                PlaybackState.ACTION_SKIP_TO_PREVIOUS );
        setMediaPlaybackState(PlaybackState.STATE_PLAYING);

        // Set the session's token so that client activities can communicate with it.
        setSessionToken(mediaSession.getSessionToken());

        // Set's the MediaSession to Active.
        mediaSession.setActive(true);
    }

    private void initNoisyReceiver()
    {
        //Handles headphones coming unplugged. cannot be done through a manifest receiver
        IntentFilter filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(headPhoneReceiver, filter);
    }

    private MediaSession.Callback mediaSessionCallback = new MediaSession.Callback()
    {
        @Override
        public boolean onMediaButtonEvent(@NonNull Intent mediaButtonIntent)
        {
            return super.onMediaButtonEvent(mediaButtonIntent);
        }

        @Override
        public void onPlay()
        {
            super.onPlay();
            if( !successfullyRetrievedAudioFocus() )
                return;

            mediaPlayer.start();
            setMediaPlaybackState(PlaybackState.STATE_PLAYING);
            showPlayingNotification();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
            {
                @Override
                public void onCompletion(MediaPlayer mp)
                {
                    Log.i("Check","onCompletion");
                    mediaPlayer.stop();
                    setMediaPlaybackState(PlaybackState.STATE_STOPPED);
                    stopForeground(false);
                    showPausedNotification();
                }
            });
        }

        @Override
        public void onPause()
        {
            mediaPlayer.pause();
            setMediaPlaybackState(PlaybackState.STATE_PAUSED);
            stopForeground(false);
            showPausedNotification();
            super.onPause();
        }

        @Override
        public void onSkipToNext()
        {
            super.onSkipToNext();

            if(++songPos >= MainActivity.songDetailArrayList.size())
                songPos = 0;
            String mediaId = String.valueOf(songPos);
            initMediaSessionMetadata(mediaId);
            mediaPlayer.reset();
            SongDetails playSong = MainActivity.songDetailArrayList.get(songPos);
            long playSongId = playSong.getId();
            Uri playSongUri = ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, playSongId);
            mediaPlayer = MediaPlayer.create( getApplicationContext(), playSongUri);
            onPlay();
        }

        @Override
        public void onSkipToPrevious()
        {
            if(--songPos < 0)
                songPos = (MainActivity.songDetailArrayList.size() - 1);
            String mediaId = String.valueOf(songPos);
            initMediaSessionMetadata(mediaId);
            mediaPlayer.reset();
            SongDetails playSong = MainActivity.songDetailArrayList.get(songPos);
            long playSongId = playSong.getId();
            Uri playSongUri = ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, playSongId);
            mediaPlayer = MediaPlayer.create( getApplicationContext(), playSongUri);
            onPlay();
            super.onSkipToPrevious();
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras)
        {
            super.onPlayFromMediaId(mediaId, extras);

            initMediaSessionMetadata(mediaId);

            songPos = Integer.parseInt(mediaId);
            mediaPlayer.reset();
            SongDetails playSong = MainActivity.songDetailArrayList.get(songPos);
            long playSongId = playSong.getId();
            Uri playSongUri = ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, playSongId);
            mediaPlayer = MediaPlayer.create( getApplicationContext(), playSongUri);
            onPlay();
        }

        @Override
        public void onSeekTo(long pos)
        {
            super.onSeekTo(pos);
        }
    };

    private void showPlayingNotification()
    {
        Notification.Builder builder = MediaStyleHelper.from(this, mediaSession, MainActivity.class, PLAY_STATE);
        if( builder == null )
            return;

        /*Notification notify = builder.build();
        notify.priority = Notification.PRIORITY_MAX;
        notify.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        NotificationManager notifyManage = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notifyManage.notify(PLAY_NOTIFICATION,notify);*/

        //It before hand start the notification with Ongoing as true; Also it does not
        // refreshes the notification when paused.
        startForeground(PLAY_NOTIFICATION, builder.build());
    }

    private void showPausedNotification()
    {
        Notification.Builder builder = MediaStyleHelper.from(this, mediaSession, MainActivity.class, PAUSE_STATE);
        if( builder == null )
            return;

        Notification notify = builder.build();
        notify.priority = Notification.PRIORITY_HIGH;
        NotificationManager notifyManage = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //Updates the notification without flashing it & keeping it removable.
        //stopForeground(false);
        notifyManage.notify(PAUSE_NOTIFICATION,notify);
    }

    private void setMediaPlaybackState(int state)
    {
        playbackstateBuilder.setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1);
        mediaSession.setPlaybackState(playbackstateBuilder.build());
    }

    private void initMediaSessionMetadata(String mediaId)
    {
        MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();
        byte[] mediabyteArray = getMediaArtByteArray(MainActivity.songDetailArrayList.
                get(Integer.parseInt(mediaId)).getMediaArtPath());
        if(mediabyteArray != null)
        {
            //Notification icon in card.
            metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, MainActivity.
                    decodeBitmapFrombyteArray(mediabyteArray ,SongDetails.TARGET_IMAGE_THUMBNAIL_WIDTH,
                            SongDetails.TARGET_IMAGE_THUMBNAIL_HEIGHT));
            metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, MainActivity.
                    decodeBitmapFrombyteArray(mediabyteArray,SongDetails.TARGET_IMAGE_THUMBNAIL_WIDTH,
                            SongDetails.TARGET_IMAGE_THUMBNAIL_HEIGHT));

            //lock screen icon for pre lollipop
            metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, MainActivity.
                    decodeBitmapFrombyteArray(mediabyteArray,SongDetails.TARGET_IMAGE_LOCKSCREEN_WIDTH,
                            SongDetails.TARGET_IMAGE_LOCKSCREEN_HEIGHT));
        }
        else
        {
            //Notification icon in card.
            metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, BitmapFactory.decodeResource
                    (getResources(),MainActivity.songDetailArrayList.get(Integer.parseInt(mediaId)).
                            getDefaultResourceId()));
            metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, BitmapFactory.decodeResource
                    (getResources(),MainActivity.songDetailArrayList.get(Integer.parseInt(mediaId)).
                            getDefaultResourceId()));

            //lock screen icon for pre lollipop
            metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, BitmapFactory.decodeResource
                    (getResources(),MainActivity.songDetailArrayList.get(Integer.parseInt(mediaId)).
                            getDefaultResourceId()));
        }
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE,
                MainActivity.songDetailArrayList.get(Integer.parseInt(mediaId)).getTitle());
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE,
                MainActivity.songDetailArrayList.get(Integer.parseInt(mediaId)).getArtist());

        mediaSession.setMetadata(metadataBuilder.build());
    }

    private boolean successfullyRetrievedAudioFocus()
    {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        int result = audioManager.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        return result == AudioManager.AUDIOFOCUS_GAIN;
    }

    @Override
    public void onAudioFocusChange(int focusChange)
    {
        switch( focusChange )
        {
            case AudioManager.AUDIOFOCUS_LOSS:
                if( mediaPlayer.isPlaying() )
                    mediaPlayer.stop();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                mediaPlayer.pause();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if( mediaPlayer != null )
                    mediaPlayer.setVolume(0.3f, 0.3f);
                break;

            case AudioManager.AUDIOFOCUS_GAIN:
                if( mediaPlayer != null )
                {
                    if( !mediaPlayer.isPlaying() )
                        mediaPlayer.start();
                    mediaPlayer.setVolume(1.0f, 1.0f);
                }
                break;
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        stopForeground(true);
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.abandonAudioFocus(this);
        unregisterReceiver(headPhoneReceiver);
        mediaSession.release();
        mediaPlayer.release();
        NotificationManagerCompat.from(this).cancel(PLAY_NOTIFICATION);
        NotificationManagerCompat.from(this).cancel(PAUSE_NOTIFICATION);
    }

    private void handleIntent(MediaSession mediaSession, Intent intent)
    {
        if(mediaSession != null && intent != null &&
                "android.intent.action.MEDIA_BUTTON".equals(intent.getAction()) &&
                intent.hasExtra("android.intent.extra.KEY_EVENT"))
        {
            KeyEvent ke = intent.getParcelableExtra("android.intent.extra.KEY_EVENT");
            MediaController mediaController = mediaSession.getController();
            mediaController.dispatchMediaButtonEvent(ke);
        }
    }

    private byte[] getMediaArtByteArray(String path)
    {
        MediaMetadataRetriever metaDataArt = new MediaMetadataRetriever();
        try
        {
            metaDataArt.setDataSource(path);
        }
        catch (IllegalArgumentException e)
        {
            Log.i("Check","Exception Thrown");
        }
        return metaDataArt.getEmbeddedPicture();
    }
}
