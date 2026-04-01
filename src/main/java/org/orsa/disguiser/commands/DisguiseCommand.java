package org.orsa.disguiser.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.orsa.disguiser.Config;
import org.orsa.disguiser.Nicknamer;
import org.orsa.disguiser.util.MojangApi;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static org.orsa.disguiser.Disguiser.*;

public class DisguiseCommand extends Command {

    public DisguiseCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        super(dispatcher);
    }

    @Override
    protected void init() {
        var base = literal("disguise");
        var self = literal("self");
        var other = literal("other");
        var target = argument("target", StringArgumentType.word()).suggests(DisguiseCommand::suggestOnlinePlayers);
        var random = literal("random");
        var name = literal("name");
        var playerName = argument("playerName", StringArgumentType.word());
        var clear = literal("clear");

        var commandTree = base
                .then(self
                        .then(random
                                .executes(DisguiseCommand::disguiseSelfRandomCommand)
                        )
                        .then(name
                                .then(playerName
                                        .executes(DisguiseCommand::disguiseSelfNameCommand)
                                )
                        )
                        .then(clear
                                .executes(DisguiseCommand::disguiseSelfClearCommand)
                        )
                )
                .then(other
                        .then(target
                                .then(random
                                        .executes(DisguiseCommand::disguiseOtherRandomCommand)
                                )
                                .then(name
                                        .then(playerName
                                                .executes(DisguiseCommand::disguiseOtherNameCommand)
                                        )
                                )
                                .then(clear
                                        .executes(DisguiseCommand::disguiseOtherClearCommand)
                                )
                        )
                );

        dispatcher.register(commandTree);
    }

    private static CompletableFuture<Suggestions> suggestOnlinePlayers(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        context.getSource().getServer().getPlayerManager().getPlayerList().forEach(p -> builder.suggest(p.getGameProfile().name()));
        return builder.buildFuture();
    }

    private static int disguiseSelfRandomCommand(CommandContext<ServerCommandSource> context) {
        var source = context.getSource();
        var player = source.getPlayer();

        randomDisguise(player, true);

        source.sendMessage(Text.literal("Applying disguise..."));

        return 1;
    }

    private static int disguiseSelfNameCommand(CommandContext<ServerCommandSource> context) {
        var source = context.getSource();
        var player = source.getPlayer();
        var disguiseName = StringArgumentType.getString(context, "playerName");

        nameDisguise(player, disguiseName, true);

        source.sendMessage(Text.literal("Applying disguise..."));

        return 1;
    }

    private static int disguiseSelfClearCommand(CommandContext<ServerCommandSource> context) {
        var source = context.getSource();
        var player = source.getPlayer();

        clearDisguise(player);

        source.sendMessage(Text.literal("Applying disguise..."));

        return 1;
    }

    private static int disguiseOtherRandomCommand(CommandContext<ServerCommandSource> context) {
        var source = context.getSource();
        var playerName = StringArgumentType.getString(context, "target");

        ServerPlayerEntity player = SERVER.getPlayerManager().getPlayer(playerName);

        if (player != null) {
            randomDisguise(player, true);
        }
        else {
            var uuid = MojangApi.getPlayerUUID(playerName);
            Config.addOfflinePlayerDisguise(uuid, "random");
            randomName(uuid);
        }

        source.sendMessage(Text.literal("Applying disguise..."));

        return 1;
    }

    private static int disguiseOtherNameCommand(CommandContext<ServerCommandSource> context) {
        var source = context.getSource();
        var playerName = StringArgumentType.getString(context, "target");
        var disguiseName = StringArgumentType.getString(context, "playerName");

        ServerPlayerEntity player = SERVER.getPlayerManager().getPlayer(playerName);

        if (player != null) {
            nameDisguise(player, disguiseName, true);
        }
        else {
            var uuid = MojangApi.getPlayerUUID(playerName);
            Config.addOfflinePlayerDisguise(uuid, "name", disguiseName);
            Nicknamer.trySetPlayerNickname(uuid, disguiseName);
        }

        source.sendMessage(Text.literal("Applying disguise..."));

        return 1;
    }


    private static int disguiseOtherClearCommand(CommandContext<ServerCommandSource> context) {
        var source = context.getSource();
        var playerName = StringArgumentType.getString(context, "target");

        ServerPlayerEntity player = SERVER.getPlayerManager().getPlayer(playerName);

        if (player != null) {
            clearDisguise(player);
        }
        else {
            var uuid = MojangApi.getPlayerUUID(playerName);
            Config.addOfflinePlayerDisguise(uuid, "clear");
            Nicknamer.clearPlayerNickname(uuid);
        }

        source.sendMessage(Text.literal("Applying disguise..."));

        return 1;
    }
}
