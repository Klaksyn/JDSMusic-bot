package com.syneation.helper.music;


import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.syneation.music.SearchProvider;
import com.syneation.service.MusicService;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;

import java.util.EnumSet;
import java.util.List;

public class Helper {

    private static final String[] NUMBER_EMOJIS = {
            "1\uFE0F\u20E3", "2\uFE0F\u20E3", "3\uFE0F\u20E3", "4\uFE0F\u20E3", "5\uFE0F\u20E3"
    };

    public static final int MAX_CHOICES = NUMBER_EMOJIS.length;

    public static final int TIMEOUT_SECONDS = 60;
    public static final int MAX_TITLE_LENGTH = 80;
    public static final String ID_PREFIX = "pick";

    public static AudioTrack trackAt(List<AudioTrack> tracks, String index) {
        try {
            int i = Integer.parseInt(index);

            return i >= 0 && i < tracks.size() ? tracks.get(i) : null;
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    public static String buttonId(String token, String value) {
        return ID_PREFIX  + ":" + token + ":" + value;
    }

    public static String buildText(List<AudioTrack> tracks) {
        StringBuilder text = new StringBuilder("**Choice track** (click emoji)")
                .append(TIMEOUT_SECONDS).append(" s):\n");
        for (int i = 0; i < tracks.size(); i++) {
            var info = tracks.get(i).getInfo();
            text.append(NUMBER_EMOJIS[i]).append(" ")
                    .append(MarkdownSanitizer.escape(shorten(info.title)))
                    .append(" -- ")
                    .append(MarkdownSanitizer.escape(shorten(info.author)))
                    .append(" (").append(formatDuration(info.length, info.isStream)).append(")\n");
        }

        return text.toString();
    }

    public static String shorten(String value) {
        if (value == null) return "?";

        return value.length() > MAX_TITLE_LENGTH ? value.substring(0, MAX_TITLE_LENGTH) : value;
    }

    public static String formatDuration(long millis, boolean stream) {
        if (stream) return "LIVE";

        long seconds = millis / 1000;

        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    public static String foundHeader(SearchProvider provider, List<String> failures) {
        StringBuilder header = new StringBuilder();
        failures.forEach(failure -> header.append("❌ ").append(failure).append('\n'));
        return header.append("🔎 Found: **").append(provider.name()).append("**").toString();
    }

    public static void edit(InteractionHook hook, String text) {
        hook.editOriginal(text)
                .setAllowedMentions(EnumSet.noneOf(Message.MentionType.class)) // track titles must not ping anyone
                .queue();
    }

}
