package org.orsa.disguiser.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static org.orsa.disguiser.Config.CONFIG;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {

    @WrapOperation(
            method = "onPlayerConnect",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/String;equalsIgnoreCase(Ljava/lang/String;)Z"
            )
    )
    private boolean wrapNameChangedCheck(String a, String b, Operation<Boolean> original, @Local(argsOnly = true) ServerPlayerEntity player) {
        if (CONFIG.nicknames.containsKey(player.getUuid())) {
            return true;
        }

        return original.call(a, b);
    }
}
