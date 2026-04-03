package org.orsa.disguiser.mixin;

import com.google.common.graph.Network;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.orsa.disguiser.network.NetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.UUID;

import static org.orsa.disguiser.Disguiser.defaultProfiles;
import static org.orsa.disguiser.config.Config.CONFIG;

@Mixin(value = ServerGamePacketListenerImpl.class, priority = 9999999)
public class ServerGamePacketListenerImplMixin {
    @Redirect(
            method = "sendPlayerChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Lnet/minecraft/network/chat/ChatType$Bound;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V")
    )
    private void redirectChatPacketSend(ServerGamePacketListenerImpl connection, Packet<?> packet) {
        if (!(packet instanceof ClientboundPlayerChatPacket playerChatPacket)) {
            connection.send(packet);
            return;
        }

        UUID senderUUID = playerChatPacket.sender();
        boolean senderIsDisguised = CONFIG().disguises.containsKey(senderUUID.toString());

        if (!senderIsDisguised) {
            connection.send(packet);
            return;
        }

        UUID recipientUUID = connection.player.getUUID();
        if (!recipientUUID.equals(senderUUID)) {
            connection.send(packet);
            return;
        }

        if (!CONFIG().selfVisibility.contains(senderUUID)) {
            connection.send(packet);
            return;
        }

        var defaultGameProfile = defaultProfiles.get(senderUUID);
        String realName = defaultGameProfile.name();

        String disguisedName = CONFIG().disguises.get(senderUUID.toString()).nickname;

        ChatType.Bound originalBound = playerChatPacket.chatType();
        Component originalBoundName = originalBound.name();

        var newName = NetworkHandler.replaceInComponent(originalBoundName, disguisedName, realName);

        ChatType.Bound modifiedBound = new ChatType.Bound(
                originalBound.chatType(),
                newName,
                originalBound.targetName()
        );

        ClientboundPlayerChatPacket modifiedPacket = new ClientboundPlayerChatPacket(
                playerChatPacket.globalIndex(),
                playerChatPacket.sender(),
                playerChatPacket.index(),
                playerChatPacket.signature(),
                playerChatPacket.body(),
                playerChatPacket.unsignedContent(),
                playerChatPacket.filterMask(),
                modifiedBound
        );

        connection.send(modifiedPacket);
    }
    }
