package com.syneation.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.syneation.service.MusicService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Shows the list of found tracks and adds reactions 1️⃣…5️⃣ under it.
 * Clicking a reaction plays the matching track.
 * Requires the GUILD_MESSAGE_REACTIONS intent and the bot permissions Add Reactions + Read Message History.
 */
public class TrackSelection {
    private static final Logger logger = LoggerFactory.getLogger(TrackSelection.class);

    // Emojis 1️⃣ … 5️⃣ (digit + U+FE0F + U+20E3). As many emojis, as many choices.
    private static final String[] NUMBER_EMOJIS = {
            "1\uFE0F\u20E3", "2\uFE0F\u20E3", "3\uFE0F\u20E3", "4\uFE0F\u20E3", "5\uFE0F\u20E3"
    };
    private static final String CANCEL_EMOJI = "\u274C"; // ❌
    public static final int MAX_CHOICES = NUMBER_EMOJIS.length;

    private static final int TIMEOUT_SECONDS = 60;
    private static final int MAX_TITLE_LENGTH = 80;

    private record Pending(long userId, Guild guild, AudioChannel channel, List<AudioTrack> tracks,
                           InteractionHook hook, Message message) {}

    private final MusicService music;
    private final Map<Long, Pending> pending = new ConcurrentHashMap<>(); // key: id of the message with the list
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "track-selection-timeout");
        thread.setDaemon(true);
        return thread;
    });

    public TrackSelection(MusicService music) {
        this.music = music;
    }

    /** Replaces "bot is thinking" with the track list and adds reactions. The user who ran /play makes the choice. */
    public void offer(InteractionHook hook, long userId, Guild guild, AudioChannel channel,
                      List<AudioTrack> tracks, String header) {
        List<AudioTrack> choices = tracks.stream().limit(MAX_CHOICES).toList();

        hook.editOriginal(buildText(header, choices))
                .setAllowedMentions(EnumSet.noneOf(Message.MentionType.class)) // track titles must not ping anyone
                .queue(message -> {
                    pending.put(message.getIdLong(), new Pending(userId, guild, channel, choices, hook, message));
                    addReactions(message, hook, choices.size());
                    scheduleTimeout(message.getIdLong());
                });
    }

    /** Called from CommandListener for every new reaction. */
    public void handleReaction(MessageReactionAddEvent event) {
        if (event.getUserIdLong() == event.getJDA().getSelfUser().getIdLong()) {
            return;
        }

        Pending selection = pending.get(event.getMessageIdLong());
        if (selection == null || event.getUserIdLong() != selection.userId()) {
            return;
        }

        String reactedEmoji = normalize(event.getEmoji().getName());

        // 1. Проверяем нажатие на крестик (отмена)
        if (normalize(CANCEL_EMOJI).equals(reactedEmoji)) {
            if (pending.remove(event.getMessageIdLong(), selection)) {
                selection.hook().editOriginal("❌ Selection cancelled.").queue();
                clearReactions(selection.message());
            }
            return;
        }

        // 2. Проверяем нажатие на цифру трека
        int index = indexOf(reactedEmoji);
        if (index < 0 || index >= selection.tracks().size()) {
            return;
        }

        if (!pending.remove(event.getMessageIdLong(), selection)) {
            return;
        }

        AudioTrack track = selection.tracks().get(index);
        music.play(selection.guild(), selection.channel(), track);
        selection.hook().editOriginal("🎶 Queued: " + shorten(track.getInfo().title))
                .setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
                .queue();
        clearReactions(selection.message());
    }
    private void addReactions(Message message, InteractionHook hook, int count) {
        RestAction<Void> chain = message.addReaction(Emoji.fromUnicode(NUMBER_EMOJIS[0]));
        for (int i = 1; i < count; i++) {
            String emoji = NUMBER_EMOJIS[i];
            chain = chain.flatMap(done -> message.addReaction(Emoji.fromUnicode(emoji)));
        }
        // Добавляем крестик в самый конец
        chain = chain.flatMap(done -> message.addReaction(Emoji.fromUnicode(CANCEL_EMOJI)));

        chain.queue(null, error -> {
            logger.warn("Failed to add reactions", error);
            pending.remove(message.getIdLong());
            hook.editOriginal("I can't add reactions. Give the bot the **Add Reactions** and **Read Message History** permissions.")
                    .queue(null, ignored -> { });
        });
    }

    private void scheduleTimeout(long messageId) {
        timer.schedule(() -> {
            Pending expired = pending.remove(messageId);
            if (expired != null) { // if nobody picked
                expired.hook().editOriginal("⌛ Selection timed out, run /play again").queue(null, ignored -> { });
                clearReactions(expired.message());
            }
        }, TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private static void clearReactions(Message message) {
        // Needs the Manage Messages permission; without it the reactions simply stay
        message.clearReactions().queue(null, ignored -> { });
    }

    private static int indexOf(String emojiName) {
        String normalized = normalize(emojiName);
        for (int i = 0; i < NUMBER_EMOJIS.length; i++) {
            if (normalize(NUMBER_EMOJIS[i]).equals(normalized)) {
                return i;
            }
        }
        return -1;
    }

    /** Discord may send the emoji with or without U+FE0F. */
    private static String normalize(String value) {
        return value.replace("\uFE0F", "");
    }

    private static String buildText(String header, List<AudioTrack> tracks) {
        StringBuilder text = new StringBuilder(header)
                .append("\n\nReact with a number to play a track (")
                .append(TIMEOUT_SECONDS).append(" s):\n");
        for (int i = 0; i < tracks.size(); i++) {
            var info = tracks.get(i).getInfo();
            text.append(NUMBER_EMOJIS[i]).append(" ")
                    .append(MarkdownSanitizer.escape(shorten(info.title)))
                    .append(" — ").append(MarkdownSanitizer.escape(shorten(info.author)))
                    .append(" (").append(formatDuration(info.length, info.isStream)).append(")\n");
        }
        return text.toString();
    }

    private static String shorten(String value) {
        if (value == null) {
            return "?";
        }
        return value.length() > MAX_TITLE_LENGTH ? value.substring(0, MAX_TITLE_LENGTH) + "…" : value;
    }

    private static String formatDuration(long millis, boolean stream) {
        if (stream) {
            return "LIVE";
        }
        long seconds = millis / 1000;
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }
}