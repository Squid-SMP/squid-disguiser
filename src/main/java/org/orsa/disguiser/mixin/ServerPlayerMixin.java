package org.orsa.disguiser.mixin;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import org.orsa.disguiser.interfaces.DisguisedPlayer;
import org.orsa.disguiser.mixin.accessors.ChunkMapAccessor;
import org.orsa.disguiser.mixin.accessors.PlayerAccessor;
import org.orsa.disguiser.mixin.accessors.TrackedEntityAccessor;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player implements DisguisedPlayer {

    @Unique
    private final ServerPlayer self = (ServerPlayer) (Object) this;
    @Shadow
    public ServerGamePacketListenerImpl connection;

    @Shadow
    protected abstract void completeUsingItem();

    @Shadow
    @Final
    private static Logger LOGGER;

    public ServerPlayerMixin(Level level, GameProfile gameProfile) {
        super(level, gameProfile);
    }

    @Override
    public void disguiser_reloadDisguise() {
        LOGGER.debug("Reloading skin for player " + self.getName().getString());

        // Refreshing in tablist for each player
        PlayerList playerManager = self.level().getServer().getPlayerList();
        playerManager.broadcastAll(new ClientboundPlayerInfoRemovePacket(new ArrayList<>(Collections.singleton(self.getUUID()))));
        playerManager.broadcastAll(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(Collections.singleton(self)));

        ServerChunkCache manager = self.level().getChunkSource();
        ChunkMap storage = manager.chunkMap;
        TrackedEntityAccessor trackerEntry = ((ChunkMapAccessor) storage).getEntityTrackers().get(self.getId());

        // Refreshing skin in world for all that see the player
        trackerEntry.getSeenBy().forEach(tracking -> trackerEntry.getServerEntity().addPairing(tracking.getPlayer()));

        // need to change the player entity on the client
        LOGGER.debug("Reloading player skin on player's client.");
        ServerLevel level = self.level();
        this.connection.send(new ClientboundRespawnPacket(
                        new CommonPlayerSpawnInfo(
                                level.dimensionTypeRegistration(),
                                level.dimension(),
                                BiomeManager.obfuscateSeed(level.getSeed()),
                                self.gameMode.getGameModeForPlayer(),
                                self.gameMode.getPreviousGameModeForPlayer(),
                                level.isDebug(),
                                level.isFlat(),
                                self.getLastDeathLocation(),
                                this.getPortalCooldown(),
                                level.getSeaLevel()
                        ),
                        ClientboundRespawnPacket.KEEP_ALL_DATA
                )
        );

        this.connection.send(new ClientboundPlayerPositionPacket(0, PositionMoveRotation.of(self), Collections.emptySet()));
        this.connection.send(new ClientboundSetCursorItemPacket(this.getInventory().getSelectedItem()));

        this.connection.send(new ClientboundChangeDifficultyPacket(level.getDifficulty(), level.getLevelData().isDifficultyLocked()));
        this.connection.send(new ClientboundSetExperiencePacket(this.experienceProgress, this.totalExperience, this.experienceLevel));
        playerManager.sendLevelInfo(self, level);
        playerManager.sendPlayerPermissionLevel(self);
        
        for (MobEffectInstance statusEffect : this.getActiveEffects()) {
            this.connection.send(new ClientboundUpdateMobEffectPacket(self.getId(), statusEffect, false));
        }

        var equipmentList = new ArrayList<Pair<EquipmentSlot, ItemStack>>();
        for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
            ItemStack itemStack = self.getItemBySlot(equipmentSlot);
            if (!itemStack.isEmpty()) {
                equipmentList.add(Pair.of(equipmentSlot, itemStack.copy()));
            }
        }

        if (!equipmentList.isEmpty()) {
            this.connection.send(new ClientboundSetEquipmentPacket(self.getId(), equipmentList));
        }


        if (!self.getPassengers().isEmpty()) {
            this.connection.send(new ClientboundSetPassengersPacket(self));
        }
        if (self.isPassenger()) {
            this.connection.send(new ClientboundSetPassengersPacket(self.getVehicle()));
        }

        this.onUpdateAbilities();
        playerManager.sendAllPlayerInfo(self);
    }

    @Override
    public void disguiser_setDisguise(String nickname, Property skinData, boolean reload) {
        LOGGER.debug("Setting skin for player " + self.getName().getString());
        try {
            LOGGER.debug("Clearing existing skin for player");

            Multimap<String, Property> properties = ArrayListMultimap.create(this.getGameProfile().properties());
            properties.removeAll(DisguisedPlayer.PROPERTY_TEXTURES);

            ((PlayerAccessor) this).setGameProfile(new GameProfile(
                    this.getGameProfile().id(),
                    nickname,
                    new PropertyMap(properties)
            ));
        } catch (Exception ignored) {
            // Player has no skin data, no worries
        }

        try {
            Multimap<String, Property> properties = ArrayListMultimap.create(this.getGameProfile().properties());
            properties.put(DisguisedPlayer.PROPERTY_TEXTURES, skinData);

            ((PlayerAccessor) this).setGameProfile(new GameProfile(
                    this.getGameProfile().id(),
                    nickname,
                    new PropertyMap(properties)
            ));

            // Reloading skin
            if (reload) {
                this.disguiser_reloadDisguise();
            }
        } catch (Error e) {
            // Something went wrong when trying to set the skin
            LOGGER.error(e.getMessage());
        }
    }

    @Override
    public void disguiser_setDisguise(String nickname, String value, String signature, boolean reload) {
        var property = new Property(DisguisedPlayer.PROPERTY_TEXTURES, value, signature);
        this.disguiser_setDisguise(nickname, property, reload);
    }
}
