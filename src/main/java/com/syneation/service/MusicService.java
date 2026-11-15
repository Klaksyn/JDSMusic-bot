package com.syneation.service;

import com.github.topi314.lavasrc.yandexmusic.YandexMusicSourceManager;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.syneation.manager.GuildMusicManager;
import com.syneation.music.LoadCallback;
import com.syneation.music.SearchCallback;
import com.syneation.music.SearchProvider;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.clients.skeleton.Client;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.lavalink.youtube.YoutubeSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** A single shared service for the entire bot: the Lavaplayer player and server-specific queues. */
public final class MusicService  {
    private static final Logger logger = LoggerFactory.getLogger(MusicService.class);
    private static final MusicService INSTANCE = new MusicService();

    private final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
    private final Map<Long, GuildMusicManager> managers = new ConcurrentHashMap<>();

    private final List<SearchProvider> providers = new ArrayList<>();

    private MusicService() {
        providers.add(registerYoutubeMusic());
        providers.add(registerBandCamp());
        providers.add(registerYandexMusic());
        providers.add(registerSoundCloud());
    }

    // ---- Platforms. Each one can be switched off with an environment variable (true/false) ----

    private SearchProvider registerYoutubeMusic() {
        if (!isEnabled("YOUTUBE_ENABLED", true)) {
            return SearchProvider.disabled(
                    "ytmsearch:",
                    "YouTube Music",
                    "disabled in settings (YOUTUBE_ENABLED=false)"
            );
        }

        try {
            // Получаем токены из переменных окружения
            String poToken = System.getenv("YOUTUBE_PO_TOKEN");
            String visitorData = System.getenv("YOUTUBE_VISITOR_DATA");

            // Применяем токены, если оба заданы
            if (poToken != null && !poToken.isBlank()
                    && visitorData != null && !visitorData.isBlank()) {

                YoutubeSource.setPoTokenAndVisitorData(
                        poToken,
                        visitorData
                );

                logger.info("YouTube PO Token configured");
            } else {
                logger.warn("YouTube PO Token is not configured");
            }

            YoutubeAudioSourceManager youtube =
                    new YoutubeAudioSourceManager();

            logger.info("YouTube source version: {}", YoutubeSource.VERSION);

            logger.info("YouTube clients: {}",
                    Arrays.stream(youtube.getClients()).map(Client::getIdentifier)
                            .toList());

            playerManager.registerSourceManager(youtube);

            logger.info("YouTube Music source registered successfully");

            return SearchProvider.enabled(
                    "ytmsearch:",
                    "YouTube Music"
            );

        } catch (Exception e) {
            logger.error("Failed to register YouTube", e);

            return SearchProvider.disabled(
                    "ytmsearch:",
                    "YouTube Music",
                    "failed to initialize"
            );
        }
    }

    private SearchProvider registerBandCamp() {
        if (!isEnabled("BANDCAMP_ENABLED", true)) {
            return SearchProvider.disabled(
                    "bcsearch:",
                    "Bandcamp",
                    "disabled in settings"
            );
        }

        try {
            BandcampAudioSourceManager bandcamp =
                    new BandcampAudioSourceManager(true);

            playerManager.registerSourceManager(bandcamp);

            logger.info("Bandcamp source registered successfully");

            return SearchProvider.enabled(
                    "bcsearch:",
                    "Bandcamp"
            );

        } catch (Exception e) {
            logger.error("Failed to register Bandcamp", e);

            return SearchProvider.disabled(
                    "bcsearch:",
                    "Bandcamp",
                    "failed to initialize"
            );
        }
    }

    private SearchProvider registerYandexMusic() {
        if (!isEnabled("YANDEX_MUSIC_ENABLED", true)) {
            return SearchProvider.disabled("ymsearch:", "Yandex Music", "disabled in settings (YANDEX_MUSIC_ENABLED=false)");
        }
        // A Yandex account token gives access to the account: keep it in an environment variable, not in code
        String token = System.getenv("YANDEX_MUSIC_TOKEN");
        if (token == null || token.isBlank()) {
            return SearchProvider.disabled("ymsearch:", "Yandex Music", "YANDEX_MUSIC_TOKEN is not set");
        }
        try {
            playerManager.registerSourceManager(new YandexMusicSourceManager(token));
            return SearchProvider.enabled("ymsearch:", "Yandex Music");
        } catch (Exception e) {
            logger.warn("Failed to enable Yandex Music", e);
            return SearchProvider.disabled("ymsearch:", "Yandex Music", "failed to start (see the log)");
        }
    }

    private SearchProvider registerSoundCloud() {
        // Off by default: playback is broken by lavaplayer#204. Set SOUNDCLOUD_ENABLED=true once it is fixed.
        if (!isEnabled("SOUNDCLOUD_ENABLED", false)) {
            return SearchProvider.disabled("scsearch:", "SoundCloud", "disabled (known issue: lavaplayer#204)");
        }
        playerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        return SearchProvider.enabled("scsearch:", "SoundCloud");
    }

    private static boolean isEnabled(String variable, boolean defaultValue) {
        String value = System.getenv(variable);
        return value == null || value.isBlank() ? defaultValue : Boolean.parseBoolean(value.trim());
    }

    public static MusicService get() {
        return INSTANCE;
    }

    public GuildMusicManager manager(Guild guild) {
        return managers.computeIfAbsent(guild.getIdLong(), id -> new GuildMusicManager(playerManager));
    }

    // ---- Search with automatic switching between platforms ----

    /** Searches by text: platforms one by one until one finds something. Plays nothing. */
    public void search(Guild guild, String query, SearchCallback callback) {
        searchFrom(0, guild, query, new ArrayList<>(), callback);
    }

    private void searchFrom(int index, Guild guild, String query, List<String> notes, SearchCallback callback) {
        if (index >= providers.size()) {
            callback.onNotFound(notes);
            return;
        }

        SearchProvider provider = providers.get(index);
        if (!provider.enabled()) {
            skipTo(index, guild, query, notes, callback, "⛔ " + provider.name() + ": " + provider.disabledReason());
            return;
        }

        load(guild, provider.prefix() + query, new LoadCallback() {
            @Override
            public void onTracks(List<AudioTrack> tracks, boolean searchResult) {
                callback.onFound(provider, tracks, notes);
            }

            @Override
            public void onEmpty() {
                skipTo(index, guild, query, notes, callback, "❌ " + provider.name() + ": nothing found");
            }

            @Override
            public void onError(String message) {
                skipTo(index, guild, query, notes, callback, "❌ " + provider.name() + ": search error");
            }
        });
    }

    private void skipTo(int index, Guild guild, String query, List<String> notes, SearchCallback callback, String note) {
        notes.add(note);
        String next = nextEnabledName(index);
        if (next != null) {
            callback.onProgress(String.join("\n", notes) + "\n🔄 Trying " + next + "…");
        }
        searchFrom(index + 1, guild, query, notes, callback);
    }

    private String nextEnabledName(int index) {
        for (int i = index + 1; i < providers.size(); i++) {
            if (providers.get(i).enabled()) {
                return providers.get(i).name();
            }
        }
        return null;
    }

    // ---- Loading and playback ----

    /** It loads a link or an identifier, but doesn't trigger anything and doesn't join the channel. */
    public void load(Guild guild, String identifier, LoadCallback callback) {
        playerManager.loadItemOrdered(manager(guild), identifier, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                callback.onTracks(List.of(track), false);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (playlist.getTracks().isEmpty()) {
                    callback.onEmpty();
                    return;
                }
                callback.onTracks(playlist.getTracks(), playlist.isSearchResult());
            }

            @Override
            public void noMatches() {
                callback.onEmpty();
            }

            @Override
            public void loadFailed(FriendlyException e) {
                logger.warn("Failed to load {}", identifier, e);
                String reason = e.getMessage() == null ? "" : e.getMessage();
                callback.onError(reason.contains("Unknown file format")
                        ? "This link is not supported."
                        : "Failed to load: " + reason);
            }
        });
    }

    /** It joins the channel (only now, once the track is confirmed to exist) and adds the track to the queue. */
    public void play(Guild guild, AudioChannel channel, AudioTrack track) {
        GuildMusicManager gm = manager(guild);
        guild.getAudioManager().setSendingHandler(gm.sendHandler);
        guild.getAudioManager().openAudioConnection(channel);
        gm.scheduler.queue(track);
    }
}