package org.orsa.disguiser.network;

import com.mojang.authlib.properties.Property;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.Context;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.orsa.disguiser.interfaces.DisguisedPlayer;
import org.orsa.disguiser.mixin.accessors.ClientboundPlayerInfoUpdatePacketAccessor;
import org.spongepowered.asm.mixin.Unique;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.orsa.disguiser.Disguiser.*;
import static org.orsa.disguiser.config.Config.CONFIG;
import static org.orsa.disguiser.util.SkinFetcher.fetchSkinByName;

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

        newPacket.entries().forEach(e ->
                LOGGER.info("Actions: {} | profileId: {} | displayName: {} | profile: {}",
                        newPacket.actions(),
                        e.profileId(),
                        e.displayName(),
                        e.profile() != null ? e.profile().name() : "null"));

        return newPacket;
    }

    public static ClientboundSetPlayerTeamPacket modifySetPlayerTeamPacket(ClientboundSetPlayerTeamPacket packet, UUID receiverUUID) {
        var players = packet.getPlayers();
        if (players.size() != 1) {
            return packet;
        }

        var senderName = players.stream().findFirst().get();
        var senderUUID = UUID.fromString(CONFIG().disguiseNames.get(senderName));

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
            params.displayName = NetworkHandler.replaceInComponent(params.displayName, senderName, newName);
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
}
