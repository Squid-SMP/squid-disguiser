package org.orsa.disguiser;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.brigadier.CommandDispatcher;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.event.player.PlayerLoadEvent;
import me.neznamy.tab.api.tablist.TabListFormatManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.orsa.disguiser.command.DisguiseCommand;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import org.orsa.disguiser.config.Config;
import org.orsa.disguiser.network.NetworkHandler;
import org.orsa.disguiser.util.SkinFetcher;

//import static org.samo_lego.fabrictailor.util.SkinFetcher.fetchSkinByUrl;
import static org.orsa.disguiser.config.Config.CONFIG;

public class Disguiser implements ModInitializer {
    public static final String MOD_ID = "disguiser";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final ExecutorService THREADPOOL = Executors.newCachedThreadPool();

    public static MinecraftServer SERVER;

    public static Map<UUID, GameProfile> defaultProfiles = new HashMap<>();

    @Override
    public void onInitialize() {
        RandomDisguiseSelector.initialize();

        AutoConfig.register(Config.class, GsonConfigSerializer::new);
        Config.save();

        CommandRegistrationCallback.EVENT.register((cd, ra, re) -> registerCommands(cd));
        ServerPlayConnectionEvents.INIT.register(NetworkHandler::onInit);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            SERVER = server;
            tryRegisterTabListener();
        });
    }

    private static void tryRegisterTabListener() {
        try {
            TabAPI.getInstance().getEventBus().register(PlayerLoadEvent.class, Disguiser::onTabRegister);
        } catch (Exception e) {
            LOGGER.debug("TAB API not available: {}", e.getMessage());
        }
    }

    private static void onTabRegister(PlayerLoadEvent event) {
        TabPlayer tabPlayer = event.getPlayer();
        UUID uuid = tabPlayer.getUniqueId();

        if (!CONFIG().disguises.containsKey(uuid.toString())) {
            return;
        }

        TabListFormatManager manager = TabAPI.getInstance().getTabListFormatManager();
        if (manager == null) return;

        manager.setName(tabPlayer, null);
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        DisguiseCommand.register(dispatcher);
    }

    public static void setRandomDisguise(UUID uuid) {
        var nickname = RandomDisguiseSelector.getRandomNickname();
        var skinProperty = RandomDisguiseSelector.getRandomSkin();

        Property prop = null;
        if (skinProperty.isPresent()) {
            prop = skinProperty.get();
        }

        storeDisguise(uuid, nickname, prop);
    }

    public static void setNameDisguise(UUID uuid, String name) {
        if (!Nicknamer.isNicknameValid(name)) {
            return;
        }

        var skinProperty = SkinFetcher.fetchSkinByName(name);

        Property prop = null;
        if (skinProperty.isPresent()) {
            prop = skinProperty.get();
        }

        storeDisguise(uuid, name, prop);
    }

    private static void storeDisguise(UUID uuid, String nickname, Property skinProperty) {
        Config.addDisguise(uuid, nickname, skinProperty);
    }

    public static void clearDisguise(UUID uuid) {
        Config.removeDisguise(uuid);
    }

}
