package org.orsa.disguiser.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static org.orsa.disguiser.config.Config.CONFIG;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @WrapOperation(
            method = "placeNewPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/String;equalsIgnoreCase(Ljava/lang/String;)Z"
            )
    )
    private boolean wrapNameChangedCheck(String a, String b, Operation<Boolean> original, @Local(argsOnly = true) ServerPlayer player) {
        if (CONFIG().disguises.containsKey(player.getUUID())) {
            return true;
        }

        return original.call(a, b);
    }
}
