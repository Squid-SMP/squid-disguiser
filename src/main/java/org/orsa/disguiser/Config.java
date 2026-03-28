package org.orsa.disguiser;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.HashMap;
import java.util.UUID;

@me.shedaniel.autoconfig.annotation.Config(name = "disguiser")
public class Config implements ConfigData {

    public static Config CONFIG;

    HashMap<UUID,OfflinePlayerDisguise> offlinePlayerDisguises = new HashMap<>();

    static void refreshConfig() {
        CONFIG = AutoConfig.getConfigHolder(Config.class).getConfig();
    }

    static void addOfflinePlayerDisguise(UUID uuid, String skinType) {
        addOfflinePlayerDisguise(uuid, skinType, "");
    }

    static void addOfflinePlayerDisguise(UUID uuid, String skinType, String skinName) {
        var offlinePlayerDisguise = new OfflinePlayerDisguise();
        offlinePlayerDisguise.skinType = skinType;
        offlinePlayerDisguise.skinName = skinName;

        CONFIG.offlinePlayerDisguises.put(uuid, offlinePlayerDisguise);

        refreshConfig();
    }

    static void removeOfflinePlayerDisguise(UUID uuid) {
        CONFIG.offlinePlayerDisguises.remove(uuid);

        refreshConfig();
    }
}

class OfflinePlayerDisguise {
    String skinType;
    String skinName;
}