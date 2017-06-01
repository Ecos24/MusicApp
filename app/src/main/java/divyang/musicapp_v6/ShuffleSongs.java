package divyang.musicapp_v6;

import java.util.ArrayList;
import java.util.Collections;

public class ShuffleSongs
{
    private ArrayList<SongDetails> shuffledSongList;

    public ShuffleSongs(ArrayList<SongDetails> songList)
    {
        this.shuffledSongList = songList;
        Collections.shuffle(this.shuffledSongList);
    }

    public ArrayList<SongDetails> getShuffledSongList()
    {
        return this.shuffledSongList;
    }
}
