package org.orsa.disguiser.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ProfileResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

import static org.orsa.disguiser.Config.CONFIG;
import static org.orsa.disguiser.Disguiser.LOGGER;


@Mixin(targets = "net.minecraft.server.network.ServerLoginPacketListenerImpl$1")
public class ServerLoginNetworkHandlerMixin {

    @WrapOperation(method = "run()V", at = @At(value="INVOKE", target="Lcom/mojang/authlib/yggdrasil/ProfileResult;profile()Lcom/mojang/authlib/GameProfile;"))
    private GameProfile injectNickname(ProfileResult instance, Operation<GameProfile> original) {
        var profile = original.call(instance);
        UUID id = profile.id();

        if (!CONFIG.nicknames.containsKey(id) || CONFIG.nicknames.get(id).isEmpty()) {
            return profile;
        }

        var nick = CONFIG.nicknames.get(id);
        LOGGER.info("Overriding username for user {} to {}", id, nick);

        var newProfile = withNickname(profile, nick);

        return newProfile;
    }

    @Unique
    private static GameProfile withNickname(GameProfile profile, String nick) {
        return new GameProfile(profile.id(), nick, profile.properties());
    }
}
