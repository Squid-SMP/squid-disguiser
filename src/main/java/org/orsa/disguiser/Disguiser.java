package org.orsa.disguiser;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.orsa.disguiser.util.MojangApi;
import org.samo_lego.fabrictailor.command.SkinCommand;
import org.samo_lego.fabrictailor.util.SkinFetcher;
import org.orsa.nativeSquidnames.NativeSquidnames;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

import static net.minecraft.server.command.CommandManager.*;
import static org.samo_lego.fabrictailor.util.SkinFetcher.fetchSkinByUrl;
import static org.orsa.disguiser.Config.refreshConfig;
import static org.orsa.disguiser.Config.CONFIG;

public class Disguiser implements ModInitializer {
    public static final String MOD_ID = "disguiser";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final ExecutorService THREADPOOL = Executors.newCachedThreadPool();

    public static MinecraftServer SERVER;

    @Override
    public void onInitialize() {
        RandomDisguiseSelector.initialize();

        AutoConfig.register(Config.class, GsonConfigSerializer::new);
        Config.refreshConfig();

        CommandRegistrationCallback.EVENT.register((cd, ra, re) -> registerCommands(cd));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> SERVER = server);
        ServerPlayConnectionEvents.JOIN.register(Disguiser::onPlayerJoin);
    }

    private static void onPlayerJoin(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        var player = handler.player;
        var uuid = player.getUuid();

        if (!CONFIG.offlinePlayerDisguises.containsKey(uuid)) {
            return;
        }

        var offlinePlayerDisguise = CONFIG.offlinePlayerDisguises.get(uuid);

        switch (offlinePlayerDisguise.skinType) {
            case "random" -> randomDisguise(player, false);
            case "clear" -> clearDisguise(player, false);
            case "name" -> nameDisguise(player, offlinePlayerDisguise.skinName, false);
        }

        Config.removeOfflinePlayerDisguise(uuid);
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("disguise")
            .then(literal("self")
                .then(literal("random")
                    .executes(Disguiser::disguiseSelfRandomCommand)
                )
                .then(literal("name")
                    .then(argument("playerName", StringArgumentType.word())
                        .executes(Disguiser::disguiseSelfNameCommand)
                    )
                )
                .then(literal("clear")
                    .executes(Disguiser::disguiseSelfClearCommand)
                )
            )
            .then(literal("other")
                .then(argument("target", StringArgumentType.word()).suggests(Disguiser::suggestOnlinePlayers)
                    .then(literal("random")
                        .executes(Disguiser::disguiseOtherRandomCommand)
                    )
                    .then(literal("name")
                        .then(argument("playerName", StringArgumentType.word())
                            .executes(Disguiser::disguiseOtherNameCommand)
                        )
                    )
                    .then(literal("clear")
                        .executes(Disguiser::disguiseOtherClearCommand)
                    )
                )
            )
        );
    }

    private static int disguiseSelfRandomCommand(CommandContext<ServerCommandSource> context) {
        var source = context.getSource();
        var player = source.getPlayer();

        randomDisguise(player);

        source.sendMessage(Text.literal("Applying disguise..."));

        return 1;
    }

    private static int disguiseSelfNameCommand(CommandContext<ServerCommandSource> context) {
        var source = context.getSource();
        var player = source.getPlayer();
        var disguiseName = StringArgumentType.getString(context, "playerName");

        nameDisguise(player, disguiseName);

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
            randomDisguise(player);
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
            nameDisguise(player, disguiseName);
        }
        else {
            var uuid = MojangApi.getPlayerUUID(playerName);
            Config.addOfflinePlayerDisguise(uuid, "name", disguiseName);
            NativeSquidnames.trySetPlayerNickname(uuid, disguiseName);
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
            NativeSquidnames.tryClearPlayerNickname(uuid);
        }

        source.sendMessage(Text.literal("Applying disguise..."));

        return 1;
    }

    private static void randomDisguise(ServerPlayerEntity player) {
        randomDisguise(player, true);
    }

    private static void randomDisguise(ServerPlayerEntity player, boolean setNickname) {
        boolean result;

        try {
            result = THREADPOOL.submit(() -> randomSkin(player)).get();
        } catch (Exception e) {
            result = false;
        }

        if (result && setNickname) {
            randomName(player.getUuid());
        }
    }

    private static boolean randomSkin(ServerPlayerEntity player) {
        var skin = RandomDisguiseSelector.getRandomSkin();

        var skinUrl = skin[1];
        var skinUsesSlim = skin[0].equals("slim");

        boolean skinChangeResult;
        try {
            skinChangeResult = THREADPOOL.submit(() -> SkinCommand.setSkin(player, () -> fetchSkinByUrl(skinUrl, skinUsesSlim))).get();
        }
        catch (Exception e) {
            skinChangeResult = false;
        }

        return skinChangeResult;
    }

    private static void randomName(UUID uuid) {
        var name = RandomDisguiseSelector.getRandomName();
        NativeSquidnames.trySetPlayerNickname(uuid, name);
    }

    private static void nameDisguise(ServerPlayerEntity player, String name) {
        nameDisguise(player, name, true);
    }

    private static void nameDisguise(ServerPlayerEntity player, String name, boolean setNickname) {
        boolean skinChangeResult;
        try {
            skinChangeResult = THREADPOOL.submit(() -> SkinCommand.setSkin(player, () -> SkinFetcher.fetchSkinByName(name))).get();
        }
        catch (Exception e) {
            skinChangeResult = false;
        }

        if (!skinChangeResult) {
            return;
        }

        if (setNickname) {
            NativeSquidnames.trySetPlayerNickname(player.getUuid(), name);
        }
    }

    private static void clearDisguise(ServerPlayerEntity player) {
        clearDisguise(player, true);
    }

    private static void clearDisguise(ServerPlayerEntity player, boolean setNickname) {
        boolean skinChangeResult;
        try {
            skinChangeResult = THREADPOOL.submit(() -> SkinCommand.setSkin(player, () -> SkinFetcher.fetchSkinByUUID(player.getUuid()))).get();
        }
        catch (Exception e) {
            skinChangeResult = false;
        }

        if (!skinChangeResult) {
            return;
        }

        if (setNickname) {
            NativeSquidnames.tryClearPlayerNickname(player.getUuid());
        }
    }

    private static CompletableFuture<Suggestions> suggestOnlinePlayers(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        context.getSource().getServer().getPlayerManager().getPlayerList().forEach(p -> builder.suggest(p.getGameProfile().name()));
        return builder.buildFuture();
    }
}
