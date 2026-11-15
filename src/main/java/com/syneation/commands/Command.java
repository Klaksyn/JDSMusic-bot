package com.syneation.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

public interface Command {

    String getName();

    String getDescription();

    default List<OptionData> getOptions() {
        return List.of();
    }

    void executeSlash(SlashCommandInteractionEvent event);

}
