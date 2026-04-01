package org.orsa.disguiser.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.orsa.disguiser.mixin.accessors.PlayerAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.orsa.disguiser.Disguiser.defaultProfiles;
import static org.orsa.disguiser.config.Config.CONFIG;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @WrapOperation(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Ljava/lang/String;equalsIgnoreCase(Ljava/lang/String;)Z"))
    private boolean wrapNameChangedCheck(String a, String b, Operation<Boolean> original, @Local(argsOnly = true) ServerPlayer player) {
        if (CONFIG().disguises.containsKey(player.getUUID().toString())) {
            return true;
        }

        return original.call(a, b);
    }

    @Redirect(method = "broadcastAll(Lnet/minecraft/network/protocol/Packet;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V"))
    private void redirectBroadcastSend(ServerGamePacketListenerImpl connection, Packet packet) {
        if (!(packet instanceof ClientboundPlayerInfoUpdatePacket playerInfoUpdatePacket)) {
            connection.send(packet);
            return;
        }

        if (!playerInfoUpdatePacket.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER)) {
            connection.send(packet);
            return;
        }

        var firstEntry = playerInfoUpdatePacket.entries().getFirst();
        UUID senderUUID = firstEntry.profileId();
        boolean senderIsDisguised = CONFIG().disguises.containsKey(senderUUID.toString());

        if (!senderIsDisguised) {
            connection.send(packet);
            return;
        }

        ServerPlayer receiverPlayer = connection.player;
        UUID receiverUUID = receiverPlayer.getUUID();

        if (receiverUUID != senderUUID) {
            connection.send(packet);
            return;
        }

        var defaultGameProfile = defaultProfiles.get(senderUUID);
        var senderGameProfile = firstEntry.profile();

        ((PlayerAccessor) receiverPlayer).setGameProfile(defaultGameProfile);

        var serverPlayerCollection = List.of(receiverPlayer);
        var newPacket = ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(serverPlayerCollection);
        connection.send(newPacket);

        ((PlayerAccessor) receiverPlayer).setGameProfile(senderGameProfile);
    }
}
