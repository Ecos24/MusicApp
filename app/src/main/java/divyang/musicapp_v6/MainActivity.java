//https://stuff.mit.edu/afs/sipb/project/android/docs/training/displaying-bitmaps/process-bitmap.html

package divyang.musicapp_v6;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.LruCache;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity
{
    ///////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////PERMISSION FUNCTIONS/////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////
    private final int REQUEST_PERMISSION_READ_EXT_STORAGE = 1;
    private boolean Permission_Granted = false;

    private void showPhoneStatePermission(final String permission)
    {
        final String permissionDetails = "Dumbo! if you won't allow App to access Storage then How are you planning to listen to your Stored Musics. AssHole";
        int permissionCheck = ContextCompat.checkSelfPermission(this, permission);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED)
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,permission))
            {
                showExplanation("Permission Needed", permissionDetails,
                        permission, REQUEST_PERMISSION_READ_EXT_STORAGE);
            }
            else
            {
                requestPermission(permission, REQUEST_PERMISSION_READ_EXT_STORAGE);
            }
        }
        else
        {
            Permission_Granted = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        switch(requestCode)
        {
            case REQUEST_PERMISSION_READ_EXT_STORAGE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    Toast.makeText(MainActivity.this, "Permission Granted!", Toast.LENGTH_SHORT).show();
                    Permission_Granted = true;
                }
                else
                {
                    Toast.makeText(MainActivity.this, "Permission is Mandatory!", Toast.LENGTH_SHORT).show();
                    Permission_Granted = false;
                    //Send's User to App Settings page to grant Permissions.
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivityForResult(intent, REQUEST_PERMISSION_READ_EXT_STORAGE);
                }
        }
    }

    private void showExplanation(String title,String message,final String permission,final int permissionRequestCode)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        requestPermission(permission, permissionRequestCode);
                    }
                });
        builder.create().show();
    }

    private void requestPermission(String permissionName, int permissionRequestCode)
    {
        ActivityCompat.requestPermissions(this, new String[]{permissionName}, permissionRequestCode);
    }

    
    ///////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////PERMISSION FUNCTIONS END///////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    //Activity Flag
    static boolean activity = false;

    //Cache for Music Art Bitmap.
    private static LruCache<String,Bitmap> artCache;

    //Shared SongArrayList.
    public static ArrayList<SongDetails> songDetailArrayList;
    protected ListView songlistview;

    public static MediaBrowser mediaBrowser;

    private static final int STATE_PAUSED = 0;
    private static final int STATE_PLAYING = 1;
    private int currentState;
    private Intent serviceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            showPhoneStatePermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        else
            Permission_Granted = true;

        setContentView(R.layout.activity_main);

        // Code to change NavigationBar Colour.
        Window win = this.getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        win.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        win.setStatusBarColor(ContextCompat.getColor(this,R.color.colorPrimary));

        if (Permission_Granted)
        {
            activity = true;
            Log.i("Check", "Activity Started --> "+activity);

            //ListView & SongList Initialisation.
            songlistview = (ListView) findViewById(R.id.song_listView);
            songDetailArrayList = new ArrayList<>();

            //Fetch music from device
            getSongList();

            //Populating the ListView with the Song ArrayList.
            SongAdapter songAdt = new SongAdapter(songDetailArrayList ,this);
            songlistview.setAdapter(songAdt);
            
            TextView sizeSongListView = (TextView) findViewById(R.id.size_song_listView);
            sizeSongListView.setText(songDetailArrayList.size()+" Total Songs");


            // Create MediaBrowserService.
            mediaBrowser = new MediaBrowser(this, new ComponentName(this,
                    BackgroundMediaService.class),connectionCallback, null);
            mediaBrowser.connect();

            //Starting the Background Service.
            serviceIntent = new Intent(this, BackgroundMediaService.class);
            startService(serviceIntent);


            //Song ListView Click Listener.
            songlistview.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                {
                    String mediaId = Integer.valueOf(position).toString();
                    getMediaController().getTransportControls().playFromMediaId(mediaId, null);
                }
            });

            //Cache Initialisation.
            final int maxMemorySize = (int) Runtime.getRuntime().maxMemory() / 1024;
            final int cacheSize = maxMemorySize/10;
            Log.d("Cache Info","Creating Cache with Size "+cacheSize);
            artCache = new LruCache<String, Bitmap>(cacheSize)
            {
                @Override
                protected int sizeOf(String key, Bitmap value)
                {
                    return value.getByteCount()/1024;
                }
            };
        }
    }

    @Override
    protected void onDestroy()
    {
        activity = false;
        Log.i("Check", "Activity Stopped --> "+activity);

        if( mediaBrowser!=null )
        {
            mediaBrowser.disconnect();
        }
        if( BackgroundMediaService.mediaPlayer != null )
        {
            if( !BackgroundMediaService.mediaPlayer.isPlaying() )
                stopService(serviceIntent);
        }
        super.onDestroy();
    }

    private MediaBrowser.ConnectionCallback connectionCallback =
            new MediaBrowser.ConnectionCallback()
            {
                @Override
                public void onConnected()
                {
                    // Create a MediaController with the token for the MediaSession.
                    MediaController mediaController = new MediaController
                            (MainActivity.this, mediaBrowser.getSessionToken());
                    // Save the controller.
                    setMediaController(mediaController);

                    // Register a Callback to stay in sync
                    mediaController.registerCallback(mediaControllerCallback);
                }
            };

    private MediaController.Callback mediaControllerCallback = new MediaController.Callback()
    {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackState state)
        {
            super.onPlaybackStateChanged(state);

            switch( state.getState() )
            {
                case PlaybackState.STATE_BUFFERING:
                    break;

                case PlaybackState.STATE_CONNECTING:
                    break;

                case PlaybackState.STATE_ERROR:
                    break;

                case PlaybackState.STATE_FAST_FORWARDING:
                    break;

                case PlaybackState.STATE_NONE:
                    break;

                case PlaybackState.STATE_REWINDING:
                    break;

                case PlaybackState.STATE_SKIPPING_TO_NEXT:
                    break;

                case PlaybackState.STATE_SKIPPING_TO_PREVIOUS:
                    break;

                case PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM:
                    break;

                case PlaybackState.STATE_STOPPED:
                    break;

                case PlaybackState.STATE_PLAYING:
                    currentState = STATE_PLAYING;
                    break;

                case PlaybackState.STATE_PAUSED:
                    currentState = STATE_PAUSED;
                    break;
            }
        }
    };

    /////////////////////////////Function To Get SongList From Storage//////////////////////////////
    public void getSongList()
    {
        //Meta data for Songs.
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        ContentResolver resolver = getContentResolver();
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        //String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cursor = resolver.query(musicUri, null, selection, null, null);

        //Declarations to be used.
        int titleCol;
        int idColumn;
        int artistColumn;
        int durationColumn;
        long id;
        String title;
        String artist;
        long duration;
        int defaultResourceId;
        SongDetails song;

        //Usage for Bitmap.
        String path;

        if (cursor != null && cursor.moveToFirst())
        {
            try
            {
                do
                {
                    //metaData.setDataSource(this,);
                    //get Columns
                    titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                    idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
                    artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                    durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);

                    //Get values corresponding to above columnIndex's.
                    id = cursor.getLong(idColumn);
                    title = cursor.getString(titleCol);
                    artist = cursor.getString(artistColumn);
                    duration = cursor.getLong(durationColumn);
                    defaultResourceId = R.drawable.default_cover_art;

                    //Try at mediaMetaDataRetriever.
                    path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));

                    //Formatting Details to Fit View Page Correctly.
                    StringBuilder titleBuild = new StringBuilder();
                    titleBuild.append(title);
                    if(titleBuild.length() > 21)
                    {
                        titleBuild.setLength(21);
                        title = titleBuild.toString()+"...";
                    }
                    else
                        title = titleBuild.toString();
                    StringBuilder artistBuild = new StringBuilder();
                    artistBuild.append(artist);
                    if(artistBuild.length() > 20)
                    {
                        artistBuild.setLength(20);
                        artist = artistBuild.toString()+"...";
                    }
                    else
                        artist = artistBuild.toString();

                    //Pass these value to SongDetails Class to initialise it.
                    song = new SongDetails(title, artist, id, duration, path, defaultResourceId);
                    //Add to the list
                    songDetailArrayList.add(song);
                } while (cursor.moveToNext());
            }
            finally {
                cursor.close();
            }
        }
        //Sort's the songs list alphabetically according to their title.
        Collections.sort(songDetailArrayList, new Comparator<SongDetails>()
        {
            public int compare(SongDetails a, SongDetails b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////Functions For Decoding byte[]///////////////////////////////
    public static Bitmap decodeBitmapFrombyteArray(byte[] ArtbyteArray, int reqWidth, int reqHeight)
    {
        if(ArtbyteArray != null)
        {
            // First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            options.inSampleSize = 2;
            BitmapFactory.decodeByteArray(ArtbyteArray, 0, ArtbyteArray.length, options);

            // Calculate inSampleSize
            options.inSampleSize = calculateInSize(options, reqWidth, reqHeight);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeByteArray(ArtbyteArray, 0, ArtbyteArray.length, options);
        }
        else
            return null;

    }

    public static int calculateInSize(BitmapFactory.Options options, int reqWidth, int reqHeight)
    {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        return inSampleSize;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////Functions For Cache/////////////////////////////////////////
    public static Bitmap getBitmapFromCache(String key)
    {
        return artCache.get(key);
    }

    public static void setBitmapToCache(String key, Bitmap bitmap)
    {
        if( key != null && bitmap != null)
        {
            artCache.put(key,bitmap);
        }
        else
            Log.d("Cache Error","Key/Bitmap Null");
        /*if(getBitmapFromCache(key)!= null)
        {
            Log.i("Check","Setting Cache for key "+key+" in Main Activity");
            artCache.put(key,bitmap);
        }*/
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
}
