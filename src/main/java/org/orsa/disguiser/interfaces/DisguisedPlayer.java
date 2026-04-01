package org.orsa.disguiser.interfaces;

import com.mojang.authlib.properties.Property;

import java.util.Optional;

public interface DisguisedPlayer {
    String PROPERTY_TEXTURES = "textures";

    void disguiser_reloadDisguise();

    void disguiser_setDisguise(String nickname, Property skinData, boolean reload);

    void disguiser_setDisguise(String nickname, String value, String signature, boolean reload);
}
