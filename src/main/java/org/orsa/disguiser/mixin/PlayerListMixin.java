package org.orsa.disguiser.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static org.orsa.disguiser.config.Config.CONFIG;

@Mixin(value = PlayerList.class, priority = 9999999)
public abstract class PlayerListMixin {
    @WrapOperation(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Ljava/lang/String;equalsIgnoreCase(Ljava/lang/String;)Z"))
    private boolean wrapNameChangedCheck(String a, String b, Operation<Boolean> original, @Local(argsOnly = true) ServerPlayer player) {
        if (CONFIG().disguises.containsKey(player.getUUID().toString())) {
            return true;
        }

        return original.call(a, b);
    }

//    @Redirect(method = "broadcastAll(Lnet/minecraft/network/protocol/Packet;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V"))
//    private void redirectBroadcastSend(ServerGamePacketListenerImpl connection, Packet<?> packet) {
//        if (!(packet instanceof ClientboundPlayerInfoUpdatePacket playerInfoUpdatePacket)) {
//            connection.send(packet);
//            return;
//        }
//
//        var firstEntry = playerInfoUpdatePacket.entries().getFirst();
//        UUID senderUUID = firstEntry.profileId();
//        boolean senderIsDisguised = CONFIG().disguises.containsKey(senderUUID.toString());
//
//        if (!senderIsDisguised) {
//            connection.send(packet);
//            return;
//        }
//
//        ServerPlayer receiverPlayer = connection.player;
//        UUID receiverUUID = receiverPlayer.getUUID();
//
//        if (!receiverUUID.equals(senderUUID)) {
//            connection.send(packet);
//            return;
//        }
//
//        if (!CONFIG().selfVisibility.contains(senderUUID)) {
//            connection.send(packet);
//            return;
//        }
//
//        var defaultGameProfile = defaultProfiles.get(senderUUID);
//        var accessor = (ClientboundPlayerInfoUpdatePacketAccessor) packet;
//
//        var original = accessor.getEntries();
//
//        List<ClientboundPlayerInfoUpdatePacket.Entry> modified = accessor.getEntries().stream()
//                .map(entry -> new ClientboundPlayerInfoUpdatePacket.Entry(
//                        entry.profileId(),
//                        defaultGameProfile,
//                        entry.listed(),
//                        entry.latency(),
//                        entry.gameMode(),
//                        Component.literal(defaultGameProfile.name()),
//                        entry.showHat(),
//                        entry.listOrder(),
//                        entry.chatSession()
//                ))
//                .toList();
//
//        ClientboundPlayerInfoUpdatePacket newPacket = new ClientboundPlayerInfoUpdatePacket(
//                playerInfoUpdatePacket.actions(),
//                List.of(receiverPlayer)
//        );
//
//        ((ClientboundPlayerInfoUpdatePacketAccessor) newPacket).setEntries(modified);
//
//        connection.send(newPacket);
//    }
}
