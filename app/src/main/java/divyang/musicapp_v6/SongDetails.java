package divyang.musicapp_v6;

import java.io.Serializable;

class SongDetails implements Serializable
{
    static int TARGET_IMAGE_THUMBNAIL_WIDTH = 150;
    static int TARGET_IMAGE_THUMBNAIL_HEIGHT = 150;
    static int TARGET_IMAGE_LOCKSCREEN_WIDTH = 200;
    static int TARGET_IMAGE_LOCKSCREEN_HEIGHT = 300;

    private long id;
    private String title;
    private String artist;
    private long duration;
    private String mediaArtPath;
    private int defaultResourceId;

    SongDetails(String title, String artist, long id, long duration,
                String mediaArtPath, int defaultResourceId)
    {
        this.title = title;
        this.artist = artist;
        this.id = id;
        this.duration = duration;
        this.mediaArtPath=mediaArtPath;
        this.defaultResourceId=defaultResourceId;
    }

    long getId()
    {
        return id;
    }

    String getTitle()
    {
        return title;
    }

    String getArtist()
    {
        return artist;
    }

    long getDuration()
    {
        return duration;
    }

    String getMediaArtPath()
    {
        return mediaArtPath;
    }

    int getDefaultResourceId()
    {
        return defaultResourceId;
    }
}
