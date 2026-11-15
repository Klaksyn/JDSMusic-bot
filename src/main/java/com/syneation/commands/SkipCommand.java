package com.syneation.commands;

import com.syneation.service.MusicService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class SkipCommand implements Command{
    @Override
    public String getName() {
        return "skip";
    }

    @Override
    public String getDescription() {
        return "Skip track";
    }

    @Override
    public void executeSlash(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            event.reply("The command runs only on the server").setEphemeral(true).queue();
            return;
        }

        MusicService.get().manager(event.getGuild()).scheduler.next();
        event.reply("⏭ Skipped").queue();
    }
}
