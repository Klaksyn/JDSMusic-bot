package com.syneation.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.syneation.music.LoadCallback;
import com.syneation.music.SearchCallback;
import com.syneation.music.SearchProvider;
import com.syneation.service.MusicService;
import com.syneation.music.TrackSelection;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.EnumSet;
import java.util.List;

import static com.syneation.helper.music.Helper.edit;
import static com.syneation.helper.music.Helper.foundHeader;

public class PlayCommand implements Command {
    private static final String OPTION_QUERY = "query";
    private static final int MAX_LINK_TRACKS = 50; // guard against huge playlists

    private final MusicService music = MusicService.get();
    private final TrackSelection selection;

    public PlayCommand(TrackSelection selection) {
        this.selection = selection;
    }

    @Override
    public String getName() {
        return "play";
    }

    @Override
    public String getDescription() {
        return "Search for a track by name and pick one with a reaction, or play from a link";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(new OptionData(OptionType.STRING, OPTION_QUERY, "Name or link", true));
    }

    @Override
    public void executeSlash(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null || event.getMember() == null) {
            event.reply("This command only works on a server").setEphemeral(true).queue();
            return;
        }

        GuildVoiceState voiceState = event.getMember().getVoiceState();
        if (voiceState == null || !voiceState.inAudioChannel()) {
            event.reply("Join a voice channel first").setEphemeral(true).queue();
            return;
        }

        String query = event.getOption(OPTION_QUERY).getAsString().trim();
        Guild guild = event.getGuild();
        AudioChannel channel = voiceState.getChannel();
        long userId = event.getUser().getIdLong();
        InteractionHook hook = event.getHook();

        event.deferReply().queue(); // the search can take longer than 3 seconds

        if (query.startsWith("http://") || query.startsWith("https://")) {
            playLink(guild, channel, query, hook);
        } else {
            searchByText(guild, channel, userId, query, hook);
        }
    }

    /** Link to a track, album or file: play it right away. */
    private void playLink(Guild guild, AudioChannel channel, String link, InteractionHook hook) {
        music.load(guild, link, new LoadCallback() {
            @Override
            public void onTracks(List<AudioTrack> tracks, boolean searchResult) {
                List<AudioTrack> toPlay = tracks.stream().limit(MAX_LINK_TRACKS).toList();
                toPlay.forEach(track -> music.play(guild, channel, track));
                edit(hook, toPlay.size() == 1
                        ? "🎶 Queued: " + toPlay.get(0).getInfo().title
                        : "🎶 Tracks added: " + toPlay.size());
            }

            @Override
            public void onEmpty() {
                edit(hook, "Nothing was found at this link");
            }

            @Override
            public void onError(String message) {
                edit(hook, message);
            }
        });
    }

    /** Text: search the platforms one by one until one finds something, then let the user pick with a reaction. */
    private void searchByText(Guild guild, AudioChannel channel, long userId, String query, InteractionHook hook) {
        music.search(guild, query, new SearchCallback() {
            @Override
            public void onProgress(String text) {
                edit(hook, text);
            }

            @Override
            public void onFound(SearchProvider provider, List<AudioTrack> tracks, List<String> failures) {
                String header = foundHeader(provider, failures);
                if (tracks.size() == 1) {
                    music.play(guild, channel, tracks.get(0));
                    edit(hook, header + "\n🎶 Queued: " + tracks.get(0).getInfo().title);
                    return;
                }
                selection.offer(hook, userId, guild, channel, tracks, header);
            }

            @Override
            public void onNotFound(List<String> failures) {
                StringBuilder text = new StringBuilder("😕 Nothing was found on any platform:");
                failures.forEach(failure -> text.append("\n❌ ").append(failure));
                edit(hook, text.toString());
            }
        });
    }


}