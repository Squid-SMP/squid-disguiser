package org.orsa.disguiser.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.orsa.disguiser.config.Config;
import org.orsa.disguiser.Nicknamer;
import org.orsa.disguiser.util.MojangApi;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static org.orsa.disguiser.Disguiser.*;

public class DisguiseCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var disguise = "disguise";
        var self = "self";
        var other = "other";
        var toggleVisibility = "toggleVisibility";
        var target = "target";
        var random = "random";
        var name = "name";
        var playerName = "playerName";
        var clear = "clear";

        var commandTree = literal(disguise)
                .then(literal(self)
                        .then(literal(random)
                                .executes(DisguiseCommand::disguiseSelfRandomCommand)
                        )
                        .then(literal(name)
                                .then(argument(playerName, StringArgumentType.word())
                                        .executes(DisguiseCommand::disguiseSelfNameCommand)
                                )
                        )
                        .then(literal(clear)
                                .executes(DisguiseCommand::disguiseSelfClearCommand)
                        )
                        .then(literal(toggleVisibility)
                                .executes(DisguiseCommand::disguiseToggleVisibility)
                        )
                )
                .then(literal(other)
                        .then(argument(target, StringArgumentType.word()).suggests(DisguiseCommand::suggestOnlinePlayers)
                                .then(literal(random)
                                        .executes(DisguiseCommand::disguiseOtherRandomCommand)
                                )
                                .then(literal(name)
                                        .then(argument(playerName, StringArgumentType.word())
                                                .executes(DisguiseCommand::disguiseOtherNameCommand)
                                        )
                                )
                                .then(literal(clear)
                                        .executes(DisguiseCommand::disguiseOtherClearCommand)
                                )
                        )
                );

        dispatcher.register(commandTree);
    }

    private static CompletableFuture<Suggestions> suggestOnlinePlayers(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        context.getSource().getServer().getPlayerList().getPlayers().forEach(p -> builder.suggest(p.getGameProfile().name()));
        return builder.buildFuture();
    }

    private static int disguiseToggleVisibility(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        var player = source.getPlayer();

        Config.toggleSelfVisibility(player);

        return 1;
    }

    private static int disguiseSelfRandomCommand(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        var player = source.getPlayer();

        source.sendSystemMessage(Component.literal("Applying disguise..."));

        if (player != null) {
            setRandomDisguise(player.getUUID());
        }

        return 1;
    }

    private static int disguiseSelfNameCommand(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        var player = source.getPlayer();
        var disguiseName = StringArgumentType.getString(context, "playerName");

        source.sendSystemMessage(Component.literal("Applying disguise..."));

        if (player != null) {
            setNameDisguise(player.getUUID(), disguiseName);
        }

        return 1;
    }

    private static int disguiseSelfClearCommand(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        var player = source.getPlayer();

        source.sendSystemMessage(Component.literal("Applying disguise..."));

        if (player != null) {
            clearDisguise(player.getUUID());
        }

        return 1;
    }

    private static int disguiseOtherRandomCommand(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        var playerName = StringArgumentType.getString(context, "target");

        ServerPlayer player = SERVER.getPlayerList().getPlayer(playerName);

        source.sendSystemMessage(Component.literal("Applying disguise..."));

        if (player != null) {
            setRandomDisguise(player.getUUID());
        }
        else {
            var uuid = MojangApi.getPlayerUUID(playerName);
            setRandomDisguise(uuid);
        }

        return 1;
    }

    private static int disguiseOtherNameCommand(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        var playerName = StringArgumentType.getString(context, "target");
        var disguiseName = StringArgumentType.getString(context, "playerName");

        ServerPlayer player = SERVER.getPlayerList().getPlayer(playerName);

        source.sendSystemMessage(Component.literal("Applying disguise..."));

        if (player != null) {
            setNameDisguise(player.getUUID(), disguiseName);
        }
        else {
            var uuid = MojangApi.getPlayerUUID(playerName);
            setNameDisguise(uuid, disguiseName);
        }

        return 1;
    }


    private static int disguiseOtherClearCommand(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        var playerName = StringArgumentType.getString(context, "target");

        ServerPlayer player = SERVER.getPlayerList().getPlayer(playerName);

        source.sendSystemMessage(Component.literal("Applying disguise..."));

        if (player != null) {
            clearDisguise(player.getUUID());
        }
        else {
            var uuid = MojangApi.getPlayerUUID(playerName);
            clearDisguise(uuid);
        }

        return 1;
    }
}
