package org.orsa.disguiser.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.orsa.disguiser.mixin.accessors.ClientboundPlayerInfoUpdatePacketAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.UUID;

import static org.orsa.disguiser.Disguiser.LOGGER;
import static org.orsa.disguiser.Disguiser.defaultProfiles;
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

    @Redirect(method = "broadcastAll(Lnet/minecraft/network/protocol/Packet;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V"))
    private void redirectBroadcastSend(ServerGamePacketListenerImpl connection, Packet<?> packet) {
        if (!(packet instanceof ClientboundPlayerInfoUpdatePacket playerInfoUpdatePacket)) {
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

        if (!receiverUUID.equals(senderUUID)) {
            connection.send(packet);
            return;
        }

        if (!CONFIG().selfVisibility.contains(senderUUID)) {
            connection.send(packet);
            return;
        }

        var defaultGameProfile = defaultProfiles.get(senderUUID);
        var accessor = (ClientboundPlayerInfoUpdatePacketAccessor) packet;

        var original = accessor.getEntries();

        List<ClientboundPlayerInfoUpdatePacket.Entry> modified = accessor.getEntries().stream()
                .map(entry -> new ClientboundPlayerInfoUpdatePacket.Entry(
                        entry.profileId(),
                        defaultGameProfile,
                        entry.listed(),
                        entry.latency(),
                        entry.gameMode(),
                        Component.literal(defaultGameProfile.name()),
                        entry.showHat(),
                        entry.listOrder(),
                        entry.chatSession()
                ))
                .toList();

        ClientboundPlayerInfoUpdatePacket newPacket = new ClientboundPlayerInfoUpdatePacket(
                playerInfoUpdatePacket.actions(),
                List.of(receiverPlayer)
        );

        ((ClientboundPlayerInfoUpdatePacketAccessor) newPacket).setEntries(modified);

        connection.send(newPacket);
    }

//    @Redirect(method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;sendChatMessage(Lnet/minecraft/network/chat/OutgoingChatMessage;ZLnet/minecraft/network/chat/ChatType$Bound;)V"))
//    private void redirectChatSend(ServerPlayer recipient, OutgoingChatMessage outgoingChatMessage, boolean bl, ChatType.Bound bound, @Local(argsOnly = true) ServerPlayer sender) {
//        UUID senderUUID = sender.getUUID();
//        boolean senderIsDisguised = CONFIG().disguises.containsKey(senderUUID.toString());
//
//        if (!senderIsDisguised) {
//            recipient.sendChatMessage(outgoingChatMessage, bl, bound);
//            return;
//        }
//
//        UUID recipientUUID = recipient.getUUID();
//
//        if (!recipientUUID.equals(senderUUID)) {
//            recipient.sendChatMessage(outgoingChatMessage, bl, bound);
//            return;
//        }
//
//        if (!CONFIG().selfVisibility.contains(senderUUID)) {
//            recipient.sendChatMessage(outgoingChatMessage, bl, bound);
//            return;
//        }
//
//        var defaultGameProfile = defaultProfiles.get(senderUUID);
//
//        ChatType.Bound modifiedBound = new ChatType.Bound(
//                bound.chatType(),
//                Component.literal(defaultGameProfile.name()),
//                bound.targetName()
//        );
//
//        recipient.sendChatMessage(outgoingChatMessage, bl, modifiedBound);
//    }
}
