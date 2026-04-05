package org.orsa.disguiser.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import static org.orsa.disguiser.network.NetworkHandler.*;

@Mixin(value = ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonPacketListenerImplMixin {

    @Shadow
    protected abstract GameProfile playerProfile();

    @ModifyArg(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V"),
            index = 0
    )
    private Packet<?> onSendPacket(Packet<?> packet) {
//        LOGGER.info("Packet: {} | {}", packet.getClass().getSimpleName(), packet);

        switch (packet) {
            case ClientboundSetPlayerTeamPacket setPlayerTeamPacket -> {
                var newPacket = modifySetPlayerTeamPacket(setPlayerTeamPacket, playerProfile().id());
                return newPacket;
            }
            case ClientboundSetScorePacket setScorePacket -> {
                var newPacket = modifySetScorePacket(setScorePacket, playerProfile().id());
                return newPacket;
            }
            case ClientboundPlayerInfoUpdatePacket playerInfoUpdatePacket -> {
                return modifyPlayerInfoUpdatePacket(playerInfoUpdatePacket, playerProfile().id());
            }
            case ClientboundPlayerCombatKillPacket playerCombatKillPacket -> {
                return modifyPlayerCombatKillPacket(playerCombatKillPacket, playerProfile().id());
            }
            case ClientboundPlayerChatPacket playerChatPacket -> {
                return modifyPlayerChatPacket(playerChatPacket, playerProfile().id());
            }
            case ClientboundSystemChatPacket systemChatPacket -> {
                return modifySystemChatPacket(systemChatPacket, playerProfile().id());
            }
            default -> {
            }
        }

        return packet;
    }
}
