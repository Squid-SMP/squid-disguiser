package org.orsa.disguiser.config;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.mojang.authlib.properties.Property;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

import static org.orsa.disguiser.Disguiser.SERVER;

@me.shedaniel.autoconfig.annotation.Config(name = "disguiser")
public class Config implements ConfigData {

    public Map<String,DisguiseData> disguises = new LinkedTreeMap<>();
    public List<UUID> selfVisibility = new ArrayList<>();

    public static Config CONFIG() {
        return AutoConfig.getConfigHolder(Config.class).getConfig();
    }

    public static void save() {
        AutoConfig.getConfigHolder(Config.class).save();
    }

    public static void addDisguise(UUID uuid, String nickname, Property skinProperty) {
        var skinValue = "";
        var skinSignature = "";

        if (skinProperty != null) {
            skinValue = skinProperty.value();
            skinSignature = skinProperty.signature();
        }

        var disguise = new DisguiseData();
        disguise.nickname = nickname;
        disguise.skinValue = skinValue;
        disguise.skinSignature = skinSignature;

        CONFIG().disguises.put(uuid.toString(), disguise);

        onDisguiseConfigUpdated(uuid, "Your disguise has been set to \"" + nickname + "\". Reconnect to see the changes.");
    }

    public static void removeDisguise(UUID uuid) {
        CONFIG().disguises.put(uuid.toString(), new DisguiseData());

        onDisguiseConfigUpdated(uuid, "Your disguise has been cleared. Reconnect to see the changes.");
    }

    private static void onDisguiseConfigUpdated(UUID uuid, String message) {
        var playerList = SERVER.getPlayerList();
        var player = playerList.getPlayer(uuid);

        if (player != null) {
            player.connection.disconnect(Component.literal(message));
        }

        save();
    }

    public static void toggleSelfVisibility(ServerPlayer player) {
        var uuid = player.getUUID();
        var message = "";

        if (CONFIG().selfVisibility.contains(uuid)) {
            CONFIG().selfVisibility.remove(uuid);
            message = "Your disguise is now visible for yourself. Reconnect to see the changes.";
        }
        else {
            CONFIG().selfVisibility.add(uuid);
            message = "Your disguise is now hidden for yourself. Reconnect to see the changes.";
        }

        player.connection.disconnect(Component.literal(message));
    }

    public static class DisguiseData {
        public String nickname = "";
        public String skinValue = "";
        public String skinSignature = "";

        public DisguiseData() {}
    }
}

