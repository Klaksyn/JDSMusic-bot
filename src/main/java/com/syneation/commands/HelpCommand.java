package com.syneation.commands;

import com.syneation.helper.Colors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class HelpCommand implements Command{
    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Displays all commands.";
    }

    @Override
    public void executeSlash(SlashCommandInteractionEvent event) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Bot Help");
        embedBuilder.setDescription("All commands");
        embedBuilder.setColor(Colors.RED.getColor());
        embedBuilder.addField("help", "Display all commands", false);
        embedBuilder.addField("info", "Displays information about the bot.", false);
        embedBuilder.addField("ping", "Checks the bot's latency to Discord's gateway.", false);
        embedBuilder.addField("echo {text}", "Echoes back the text yoy provide", false);
        embedBuilder.addField("play {name music or name music and author}", "Find music from SoundCloud and play", false);

        MessageEmbed embed = embedBuilder.build();
        event.replyEmbeds(embed).queue();
    }
}
