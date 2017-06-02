package divyang.musicapp_v6;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

class BitmapLoaderTask extends AsyncTask<String,Integer,Bitmap>
{
    private WeakReference<ImageView> imageViewReference;
    private String concurencyPath;

    BitmapLoaderTask(ImageView imageView)
    {
        // Use a WeakReference to ensure the ImageView shouldn't be lost to garbage collector.
        imageViewReference = new WeakReference<>(imageView);
    }

    // Decode image in background.
    @Override
    protected Bitmap doInBackground(String... params)
    {
        concurencyPath = params[0];
        //Try at mediaMetaDataRetriever.
        MediaMetadataRetriever metaDataArt = new MediaMetadataRetriever();
        byte[] mediaArtbyteArray;
        try
        {
            metaDataArt.setDataSource(params[0]);
            mediaArtbyteArray = metaDataArt.getEmbeddedPicture();
            if( mediaArtbyteArray != null )
            {
                Bitmap musicArt;
                musicArt = MainActivity.decodeBitmapFrombyteArray(mediaArtbyteArray,50,50);
                Log.i("Check","Setting Cache with key "+params[1]);
                MainActivity.setBitmapToCache(params[1],musicArt);
                return musicArt;
            }
        }
        catch (/*IllegalArgumentException | */RuntimeException e) //metaDataArt.setDataSource(params[0]); was causing
        {                                                         //RuntimeException in MIUI.
            Log.i("Check","Exception Thrown");
        }
        finally
        {
            metaDataArt.release();
        }
        return null;
    }

    // Once complete, see if ImageView is still around and set bitmap.
    @Override
    protected void onPostExecute(Bitmap bm)
    {
        /*if (imageViewReference != null && bm != null)
        {
            ImageView imageView = imageViewReference.get();
            if (imageView != null)
            {
                imageView.setImageBitmap(bm);
            }
        }*/
        if(!isCancelled())
        {
            ImageView imageView = imageViewReference.get();
            if (imageViewReference != null && bm != null)
            {
                BitmapLoaderTask bitmapLoaderTask = SongAdapter.getBitmapLoaderTask(imageView);
                if (this == bitmapLoaderTask && imageView != null)
                {
                    imageView.setImageBitmap(bm);
                }
            }
            else if(imageViewReference != null)
                    imageView.setImageResource(R.drawable.default_cover_art);
        }
    }

    String getConcurencyPath()
    {
        return concurencyPath;
    }
}