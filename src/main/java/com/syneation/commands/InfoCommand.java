package com.syneation.commands;

import com.syneation.helper.Colors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;

public class InfoCommand implements Command {
    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String getDescription() {
        return "Displays information about the bot.";
    }

    @Override
    public void executeSlash(SlashCommandInteractionEvent event) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Bot Information");
        embedBuilder.setDescription("This is a simple Discord bot for listening music from SoundCloud");
        embedBuilder.setColor(Colors.PURPLE.getColor());
        embedBuilder.addField("Author", "Alexander Katin", true);
        embedBuilder.addField("Company", "Syneation", true);
        embedBuilder.addField("Language", "Java", true);
        embedBuilder.addField("Library", "JDA (Java Discord Api)", true);

        MessageEmbed embed = embedBuilder.build();
        event.replyEmbeds(embed).queue();
    }
}
