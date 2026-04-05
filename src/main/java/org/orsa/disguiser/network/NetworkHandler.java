package org.orsa.disguiser.network;

import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.orsa.disguiser.interfaces.DisguisedPlayer;
import org.orsa.disguiser.mixin.accessors.ClientboundPlayerInfoUpdatePacketAccessor;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.orsa.disguiser.Disguiser.*;
import static org.orsa.disguiser.config.Config.CONFIG;

public class NetworkHandler {

    public static void onInit(ServerGamePacketListenerImpl listener, MinecraftServer server) {
        var player = listener.getPlayer();
        UUID uuid = player.getUUID();
        var uuidStr = uuid.toString();
        var disguiseData = CONFIG().disguises.get(uuidStr);

        if (disguiseData == null || disguiseData.nickname.isEmpty() || disguiseData.skinValue.isEmpty() || disguiseData.skinSignature.isEmpty()) {
            return;
        }

        var nick = disguiseData.nickname;
        var skinValue = disguiseData.skinValue;
        var skinSignature = disguiseData.skinSignature;

        THREADPOOL.submit(() -> {
            ((DisguisedPlayer) player).disguiser_setDisguise(nick, skinValue, skinSignature);
        });
    }

    public static Component replaceInComponent(Component component, String find, String replacement) {
        MutableComponent result = component.plainCopy();

        if (component instanceof MutableComponent mutable) {
            ComponentContents contents = component.getContents();
            if (contents instanceof PlainTextContents.LiteralContents(String text)) {
                result = Component.literal(text.replace(find, replacement))
                        .withStyle(component.getStyle());
            }

            if (contents instanceof TranslatableContents translatable) {
                Object[] newArgs = new Object[translatable.getArgs().length];
                for (int i = 0; i < translatable.getArgs().length; i++) {
                    Object arg = translatable.getArgs()[i];
                    if (arg instanceof Component componentArg) {
                        newArgs[i] = replaceInComponent(componentArg, find, replacement);
                    } else {
                        newArgs[i] = arg;
                    }
                }
                result = MutableComponent.create(new TranslatableContents(
                        translatable.getKey(),
                        translatable.getFallback(),
                        newArgs
                )).withStyle(component.getStyle());
            }
        }

        for (Component sibling : component.getSiblings()) {
            result.append(replaceInComponent(sibling, find, replacement));
        }

        return result;
    }

    public static ClientboundPlayerInfoUpdatePacket modifyPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket packet, UUID receiverUUID) {
        if (packet.entries().isEmpty()) {
            return packet;
        }

        var firstEntry = packet.entries().getFirst();
        UUID senderUUID = firstEntry.profileId();
        boolean senderIsDisguised = CONFIG().disguises.containsKey(senderUUID.toString());

        if (!senderIsDisguised) {
            return packet;
        }

        if (!receiverUUID.equals(senderUUID)) {
            return packet;
        }

        if (!CONFIG().selfVisibility.contains(senderUUID)) {
            return packet;
        }

        var defaultGameProfile = defaultProfiles.get(senderUUID);
        var accessor = (ClientboundPlayerInfoUpdatePacketAccessor) packet;

        List<ClientboundPlayerInfoUpdatePacket.Entry> modified = accessor.getEntries().stream()
                .map(entry ->
                {
                    var entryDisplayName = entry.displayName();
                    var newName = Component.literal(defaultGameProfile.name());

                    if (entryDisplayName != null) {
                        newName.withStyle(entry.displayName().getStyle());
                    }

                    return new ClientboundPlayerInfoUpdatePacket.Entry(
                            entry.profileId(),
                            defaultGameProfile,
                            entry.listed(),
                            entry.latency(),
                            entry.gameMode(),
                            newName,
                            entry.showHat(),
                            entry.listOrder(),
                            entry.chatSession()
                    );
                })
                .toList();

        ClientboundPlayerInfoUpdatePacket newPacket = new ClientboundPlayerInfoUpdatePacket(
                packet.actions(),
                Collections.emptyList()
        );

        ((ClientboundPlayerInfoUpdatePacketAccessor) newPacket).setEntries(modified);

        return newPacket;
    }

    public static ClientboundSetPlayerTeamPacket modifySetPlayerTeamPacket(ClientboundSetPlayerTeamPacket packet, UUID receiverUUID) {
        var players = packet.getPlayers();
        if (players.size() != 1) {
            return packet;
        }

        var senderName = players.stream().findFirst().get();

        var senderUUIDStr = CONFIG().disguiseNames.get(senderName);

        if (senderUUIDStr == null) {
            return packet;
        }

        var senderUUID = UUID.fromString(senderUUIDStr);

        if (!receiverUUID.equals(senderUUID)) {
            return packet;
        }

        if (!CONFIG().selfVisibility.contains(senderUUID)) {
            return packet;
        }

        var defaultGameProfile = defaultProfiles.get(senderUUID);
        var newName = defaultGameProfile.name();

        packet.name = packet.name.replace(senderName,newName);
        packet.players = List.of(newName);
        packet.getParameters().ifPresent(params -> {
            params.displayName = replaceInComponent(params.displayName, senderName, newName);
        });

        return packet;
    }

    public static ClientboundSetScorePacket modifySetScorePacket(ClientboundSetScorePacket packet, UUID receiverUUID) {
        var senderName = packet.owner();
        var senderUUID = UUID.fromString(CONFIG().disguiseNames.get(senderName));

        if (!receiverUUID.equals(senderUUID)) {
            return packet;
        }

        if (!CONFIG().selfVisibility.contains(senderUUID)) {
            return packet;
        }

        var defaultGameProfile = defaultProfiles.get(senderUUID);
        var newName = defaultGameProfile.name();

        packet.owner = newName;

        return packet;
    }

    public static ClientboundPlayerCombatKillPacket modifyPlayerCombatKillPacket(ClientboundPlayerCombatKillPacket packet, UUID receiverUUID) {
        var senderEntityID = packet.playerId();

        ServerPlayer senderPlayer = SERVER.getPlayerList().getPlayers().stream()
                .filter(p -> p.getId() == senderEntityID)
                .findFirst()
                .orElse(null);

        if (senderPlayer == null) {
            return packet;
        }

        var senderUUID = senderPlayer.getUUID();

        if (!receiverUUID.equals(senderUUID)) {
            return packet;
        }

        if (!CONFIG().selfVisibility.contains(senderUUID)) {
            return packet;
        }

        var defaultGameProfile = defaultProfiles.get(senderUUID);
        var realName = defaultGameProfile.name();

        String disguisedName = CONFIG().disguises.get(senderUUID.toString()).nickname;

        packet.message = replaceInComponent(packet.message, disguisedName, realName);

        return packet;
    }

    public static ClientboundPlayerChatPacket modifyPlayerChatPacket(ClientboundPlayerChatPacket packet, UUID receiverUUID) {
        UUID senderUUID = packet.sender();
        boolean senderIsDisguised = CONFIG().disguises.containsKey(senderUUID.toString());

        if (!senderIsDisguised) {
            return packet;
        }

        if (!receiverUUID.equals(senderUUID)) {
            return packet;
        }

        if (!CONFIG().selfVisibility.contains(senderUUID)) {
            return packet;
        }

        var defaultGameProfile = defaultProfiles.get(senderUUID);
        String realName = defaultGameProfile.name();

        String disguisedName = CONFIG().disguises.get(senderUUID.toString()).nickname;

        ChatType.Bound originalBound = packet.chatType();
        Component originalBoundName = originalBound.name();

        var newName = NetworkHandler.replaceInComponent(originalBoundName, disguisedName, realName);

        ChatType.Bound modifiedBound = new ChatType.Bound(
                originalBound.chatType(),
                newName,
                originalBound.targetName()
        );

        ClientboundPlayerChatPacket modifiedPacket = new ClientboundPlayerChatPacket(
                packet.globalIndex(),
                packet.sender(),
                packet.index(),
                packet.signature(),
                packet.body(),
                packet.unsignedContent(),
                packet.filterMask(),
                modifiedBound
        );

        return modifiedPacket;
    }

    public static ClientboundSystemChatPacket modifySystemChatPacket(ClientboundSystemChatPacket packet, UUID receiverUUID) {
        boolean receiverIsDisguised = CONFIG().disguises.containsKey(receiverUUID.toString());

        if (!receiverIsDisguised) {
            return packet;
        }

        if (!CONFIG().selfVisibility.contains(receiverUUID)) {
            return packet;
        }

        var defaultGameProfile = defaultProfiles.get(receiverUUID);
        String realName = defaultGameProfile.name();

        String disguisedName = CONFIG().disguises.get(receiverUUID.toString()).nickname;

        var newContent = NetworkHandler.replaceInComponent(packet.content(), disguisedName, realName);

        ClientboundSystemChatPacket modifiedPacket = new ClientboundSystemChatPacket(newContent, packet.overlay());

        return modifiedPacket;
    }
}
