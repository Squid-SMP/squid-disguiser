package org.orsa.disguiser.mixin;

import me.neznamy.tab.shared.features.injection.NettyPipelineInjector;
import me.neznamy.tab.shared.platform.TabPlayer;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import static org.orsa.disguiser.network.NetworkHandler.*;

@Mixin(value = NettyPipelineInjector.TabChannelDuplexHandler.class, remap = false)
public abstract class TabChannelDuplexHandlerMixin {

    @Final
    @Shadow
    protected TabPlayer player;

    @ModifyArg(
            method = "write",
            at = @At(value = "INVOKE", target = "Lio/netty/channel/ChannelDuplexHandler;write(Lio/netty/channel/ChannelHandlerContext;Ljava/lang/Object;Lio/netty/channel/ChannelPromise;)V")
    )
    private Object onSendPacket(Object packet) {
        switch (packet) {
//            case ClientboundSetPlayerTeamPacket setPlayerTeamPacket -> {
//                var newPacket = modifySetPlayerTeamPacket(setPlayerTeamPacket, player.getUniqueId());
//                return newPacket;
//            }
//            case ClientboundSetScorePacket setScorePacket -> {
//                var newPacket = modifySetScorePacket(setScorePacket, player.getUniqueId());
//                return newPacket;
//            }
            case ClientboundPlayerInfoUpdatePacket playerInfoUpdatePacket -> {
                return modifyPlayerInfoUpdatePacket(playerInfoUpdatePacket, player.getUniqueId());
            }
            default -> {
            }
        }

        return packet;
    }
}
