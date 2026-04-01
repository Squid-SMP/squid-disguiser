package org.orsa.disguiser.config;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.mojang.authlib.properties.Property;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import net.minecraft.network.chat.Component;

import java.util.*;

import static org.orsa.disguiser.Disguiser.SERVER;

@me.shedaniel.autoconfig.annotation.Config(name = "disguiser")
public class Config implements ConfigData {

    public Map<String,DisguiseData> disguises = new LinkedTreeMap<>();
    public String lalala = "";

    // Helper to get the live config
    public static Config CONFIG() {
        return AutoConfig.getConfigHolder(Config.class).getConfig();
    }

    // Helper to save
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

    public static class DisguiseData {
        public String nickname = "";
        public String skinValue = "";
        public String skinSignature = "";

        public DisguiseData() {}
    }
}

