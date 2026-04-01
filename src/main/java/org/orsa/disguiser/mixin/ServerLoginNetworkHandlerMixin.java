package org.orsa.disguiser.mixin;

import com.google.common.collect.ArrayListMultimap;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.ProfileResult;
import org.orsa.disguiser.config.Config;
import org.orsa.disguiser.interfaces.DisguisedPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

import static org.orsa.disguiser.Disguiser.defaultProfiles;
import static org.orsa.disguiser.config.Config.CONFIG;


@Mixin(targets = "net.minecraft.server.network.ServerLoginPacketListenerImpl$1")
public class ServerLoginNetworkHandlerMixin {

    @WrapOperation(method = "run()V", at = @At(value="INVOKE", target="Lcom/mojang/authlib/yggdrasil/ProfileResult;profile()Lcom/mojang/authlib/GameProfile;"))
    private GameProfile injectDisguise(ProfileResult instance, Operation<GameProfile> original) {
        var profile = original.call(instance);
        UUID uuid = profile.id();

        defaultProfiles.put(uuid, profile);

        return profile;
    }
}
