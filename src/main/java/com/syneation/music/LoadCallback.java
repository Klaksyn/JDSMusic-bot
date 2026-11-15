package com.syneation.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.List;

public interface LoadCallback {
    void onTracks(List<AudioTrack> tracks, boolean searchResult);

    void onEmpty();

    void onError(String message);
}
