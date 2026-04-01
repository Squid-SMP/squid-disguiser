package org.orsa.disguiser;

import net.minecraft.network.chat.Component;

import java.util.*;

import static org.orsa.disguiser.config.Config.CONFIG;
import static org.orsa.disguiser.config.Config.refreshConfig;
import static org.orsa.disguiser.Disguiser.SERVER;

public class Nicknamer {
    // A regex to validate usernames
    private static final String USERNAME_REGEX = "^[0-9a-zA-Z_]{1,16}$";

    public static boolean isNicknameValid(String nickname) {
        if (!nickname.matches(USERNAME_REGEX)) {
            return false;
        }

//        if (CONFIG.nicknames.containsValue(nickname) && !nickname.equals(CONFIG.nicknames.get(nickname))) {
//            return false;
//        }

        return true;
    }
}