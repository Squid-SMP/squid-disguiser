package org.orsa.disguiser;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.orsa.disguiser.command.DisguiseCommand;
//import org.samo_lego.fabrictailor.command.SkinCommand;
//import org.samo_lego.fabrictailor.util.SkinFetcher;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

//import static org.samo_lego.fabrictailor.util.SkinFetcher.fetchSkinByUrl;
import static org.orsa.disguiser.Config.CONFIG;

public class Disguiser implements ModInitializer {
    public static final String MOD_ID = "disguiser";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final ExecutorService THREADPOOL = Executors.newCachedThreadPool();

    public static MinecraftServer SERVER;

    public static DisguiseCommand disguiseCommand;

    @Override
    public void onInitialize() {
        RandomDisguiseSelector.initialize();

        AutoConfig.register(Config.class, GsonConfigSerializer::new);
        Config.refreshConfig();

        CommandRegistrationCallback.EVENT.register((cd, ra, re) -> registerCommands(cd));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> SERVER = server);
        ServerPlayConnectionEvents.JOIN.register(Disguiser::onPlayerJoin);
    }

    private static void onPlayerJoin(ServerGamePacketListenerImpl listener, PacketSender sender, MinecraftServer server) {
        var player = listener.getPlayer();
        var uuid = player.getUUID();

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

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        DisguiseCommand.register(dispatcher);
    }

    public static void randomDisguise(ServerPlayer player, boolean setNickname) {
        boolean result;

        try {
            result = THREADPOOL.submit(() -> randomSkin(player)).get();
        } catch (Exception e) {
            result = false;
        }

        if (result && setNickname) {
            randomName(player.getUUID());
        }
    }

    private static boolean randomSkin(ServerPlayer player) {
        var skin = RandomDisguiseSelector.getRandomSkin();

        var skinUrl = skin[1];
        var skinUsesSlim = skin[0].equals("slim");

        boolean skinChangeResult = true;
//        try {
//            skinChangeResult = THREADPOOL.submit(() -> SkinCommand.setSkin(player, () -> fetchSkinByUrl(skinUrl, skinUsesSlim))).get();
//        }
//        catch (Exception e) {
//            skinChangeResult = false;
//        }

        return skinChangeResult;
    }

    public static void randomName(UUID uuid) {
        var name = RandomDisguiseSelector.getRandomName();
        Nicknamer.trySetPlayerNickname(uuid, name);
    }

    public static void nameDisguise(ServerPlayer player, String name, boolean setNickname) {
        boolean skinChangeResult = true;
//        try {
//            skinChangeResult = THREADPOOL.submit(() -> SkinCommand.setSkin(player, () -> SkinFetcher.fetchSkinByName(name))).get();
//        }
//        catch (Exception e) {
//            skinChangeResult = false;
//        }

        if (!skinChangeResult) {
            return;
        }

        if (setNickname) {
            Nicknamer.trySetPlayerNickname(player.getUUID(), name);
        }
    }

    public static void clearDisguise(ServerPlayer player) {
        clearDisguise(player, true);
    }

    public static void clearDisguise(ServerPlayer player, boolean setNickname) {
        boolean skinChangeResult = true;
//        try {
//            skinChangeResult = THREADPOOL.submit(() -> SkinCommand.setSkin(player, () -> SkinFetcher.fetchSkinByUUID(player.getUUID()))).get();
//        }
//        catch (Exception e) {
//            skinChangeResult = false;
//        }

        if (!skinChangeResult) {
            return;
        }

        if (setNickname) {
            Nicknamer.clearPlayerNickname(player.getUUID());
        }
    }
}
