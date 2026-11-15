package com.syneation.commands;

import com.syneation.service.MusicService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class StopCommand implements Command{
    @Override
    public String getName() {
        return "stop";
    }

    @Override
    public String getDescription() {
        return "Stop music and leave the channel channel";
    }

    @Override
    public void executeSlash(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            event.reply("The command runs only on the server").setEphemeral(true).queue();
            return;
        }

        MusicService.get().manager(event.getGuild()).scheduler.clear();
        event.getGuild().getAudioManager().closeAudioConnection();

        event.reply("⏹ Stopped").queue();
    }
}
