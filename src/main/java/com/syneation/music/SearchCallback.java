package com.syneation.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.List;

public interface SearchCallback {
    /** A platform found nothing (or is disabled), moving on to the next one. */
    void onProgress(String text);

    /** Found. notes: lines about the platforms that were skipped before this one. */
    void onFound(SearchProvider provider, List<AudioTrack> tracks, List<String> notes);

    /** No platform found anything. notes: why each platform was skipped. */
    void onNotFound(List<String> notes);
}
