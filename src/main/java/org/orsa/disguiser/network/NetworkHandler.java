package org.orsa.disguiser.network;

import com.mojang.authlib.properties.Property;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.Context;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.orsa.disguiser.interfaces.DisguisedPlayer;

import java.util.Optional;
import java.util.UUID;

import static org.orsa.disguiser.Disguiser.THREADPOOL;
import static org.orsa.disguiser.Disguiser.defaultProfiles;
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
            ((DisguisedPlayer) player).disguiser_setDisguise(nick, skinValue, skinSignature, false);
        });
    }
}
