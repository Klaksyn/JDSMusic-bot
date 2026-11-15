package com.syneation.manager;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// Track queue: starts the next track when one ends.
// Playback errors are logged: without this, a stream failure (e.g. YouTube) looks like "playing, but silent".
public class TrackScheduler extends AudioEventAdapter {
    private static final Logger logger = LoggerFactory.getLogger(TrackScheduler.class);

    private final AudioPlayer player;
    private final BlockingQueue<AudioTrack> queue = new LinkedBlockingQueue<>();

    public TrackScheduler(AudioPlayer player) {
        this.player = player;
    }

    public void queue(AudioTrack track) {
        // startTrack(.., true) does not interrupt a track that is already playing
        if (!player.startTrack(track, true)) {
            queue.offer(track);
        }
    }

    public void next() {
        player.startTrack(queue.poll(), false); // null stops the player when the queue is empty
    }

    public void clear() {
        queue.clear();
        player.stopTrack();
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        logger.info("Started playing: {}", track.getInfo().title);
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        logger.warn("Playback error for '{}'", track.getInfo().title, exception);
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        logger.warn("Track stuck for more than {} ms: {}", thresholdMs, track.getInfo().title);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason reason) {
        logger.info("Track ended: {} (reason: {})", track.getInfo().title, reason);
        if (reason.mayStartNext) {
            next();
        }
    }
}