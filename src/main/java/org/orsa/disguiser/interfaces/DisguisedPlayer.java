package org.orsa.disguiser.interfaces;

import com.mojang.authlib.properties.Property;

import java.util.Optional;

public interface DisguisedPlayer {
    String PROPERTY_TEXTURES = "textures";

    void disguiser_setDisguise(String nickname, Property skinData);

    void disguiser_setDisguise(String nickname, String value, String signature);
}
