package org.orsa.disguiser;

import net.minecraft.network.chat.Component;

import java.util.*;

import static org.orsa.disguiser.Config.CONFIG;
import static org.orsa.disguiser.Config.refreshConfig;
import static org.orsa.disguiser.Disguiser.SERVER;

public class Nicknamer {
    // A regex to validate usernames
    private static final String USERNAME_REGEX = "^[0-9a-zA-Z_]{1,16}$";

    public static void clearPlayerNickname(UUID uuid) {
        CONFIG.nicknames.put(uuid, "");

        refreshConfig();

        var player = SERVER.getPlayerList().getPlayer(uuid);

        if (player == null) {
            return;
        }

        player.connection.disconnect(Component.literal("Your nickname has been cleared. Reconnect to see the changes."));

    }

    public static boolean trySetPlayerNickname(UUID uuid, String nick) {
        if (!nick.matches(USERNAME_REGEX)) {
            return false;
        }

        if (CONFIG.nicknames.containsValue(nick) && !nick.equals(CONFIG.nicknames.get(uuid))) {
            return false;
        }

        CONFIG.nicknames.put(uuid, nick);

        refreshConfig();

        var player = SERVER.getPlayerList().getPlayer(uuid);

        if (player == null) {
            return true;
        }

        player.connection.disconnect(Component.literal("Your nickname has been set to \"" + nick + "\". Reconnect to see the changes."));

        return true;
    }
}