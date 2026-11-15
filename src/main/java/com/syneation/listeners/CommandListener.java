package com.syneation.listeners;

import com.syneation.commands.*;
import com.syneation.service.MusicService;
import com.syneation.music.TrackSelection;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class CommandListener extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(CommandListener.class);
    private final Map<String, Command> commands = new HashMap<>();
    private final TrackSelection selection = new TrackSelection(MusicService.get());
    private final String BORDER = "----------------------------------------------------------------";

    public CommandListener() {
        commands.put("help", new HelpCommand());
        commands.put("info", new InfoCommand());
        commands.put("ping", new PingCommand());
        commands.put("echo", new EchoCommand());
        commands.put("play", new PlayCommand(selection));
        commands.put("skip", new SkipCommand());
        commands.put("stop", new StopCommand());
        logger.info("Registered {} commands.", commands.size());
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String commandName = event.getName();
        Command command = commands.get(commandName);

        if (command != null) {
            logger.debug(BORDER);
            logger.debug("Executing slash commands: {} from user: {}", commandName, event.getUser().getName());
            logger.debug(BORDER);
            command.executeSlash(event);
        } else {
            logger.debug(BORDER);
            logger.warn("Unknown slash command: {} from user: {}", commandName, event.getUser().getName());
            logger.debug(BORDER);
            event.reply("Unknown command!").setEphemeral(true).queue();
        }
    }


    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        selection.handleReaction(event);
    }
}