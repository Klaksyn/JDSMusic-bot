package com.syneation;

import club.minnced.discord.jdave.interop.JDaveSessionFactory;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.syneation.config.BotConfig;
import com.syneation.listeners.CommandListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.audio.AudioModuleConfig;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.EnumSet;


public class Bot extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(Bot.class);
    private static JDA jda;

    private final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();

    public Bot() {
        playerManager.registerSourceManager(
                SoundCloudAudioSourceManager.createDefault()
        );
    }
    static void main() throws InterruptedException {
        String botToken = BotConfig.getBotToken();

        if (botToken == null || botToken.isEmpty()) {
            logger.error("Bot token not found in config.properties.");
            return;
        }

        try {
            AudioModuleConfig audio = new AudioModuleConfig()
                    .withDaveSessionFactory(new JDaveSessionFactory());

            jda = JDABuilder.createLight(
                            botToken,
                            EnumSet.of(
                                    GatewayIntent.GUILD_VOICE_STATES,
                                    GatewayIntent.GUILD_MESSAGE_REACTIONS
                            )
                    )
                    .setAudioModuleConfig(audio)
                    .enableCache(CacheFlag.VOICE_STATE)
                    .setMemberCachePolicy(MemberCachePolicy.VOICE)
                    .addEventListeners(new CommandListener())
                    .build();

            jda.awaitReady();
            logger.info("Bot is online and ready");

            registerSlashCommands();
        } catch (Exception e) {
            logger.error("Error starting the bot: ", e);
        }
    }

    public static void registerSlashCommands() {
        if (jda == null) {
            logger.error("JDA instance is not initialized. Cannot register slash commands.");
            return;
        }

        logger.info("Register Slash commands...");
        jda.updateCommands()
                .addCommands(
                        // Main Commands
                        Commands.slash("help", "Displays all commands."),
                        Commands.slash("info", "Displays information about the bot."),
                        Commands.slash("ping", "Check the bot's latency to Discord's gateway."),
                        Commands.slash("echo", "Responds back with your message")
                                .addOption(OptionType.STRING, "text", "The text to echo", true),
                        // Music commands
                        Commands.slash("play", "Find the track on SoundCloud and listen")
                                .addOption(OptionType.STRING, "query", "Name or link SoundCloud", true),
                        Commands.slash("skip", "Skip track"),
                        Commands.slash("stop", "Stop music and leave the channel channel")
                ).queue(success -> logger.info("Slash commands registered successfully!"),
                        failure -> logger.error("Failed to register slash commands: ", failure));
    }
}
