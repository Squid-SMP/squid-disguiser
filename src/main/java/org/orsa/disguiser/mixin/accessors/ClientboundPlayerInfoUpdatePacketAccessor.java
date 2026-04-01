package org.orsa.disguiser.mixin.accessors;

import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ClientboundPlayerInfoUpdatePacket.class)
public interface ClientboundPlayerInfoUpdatePacketAccessor {

    @Accessor("entries")
    List<ClientboundPlayerInfoUpdatePacket.Entry> getEntries();

    @Accessor("entries")
    @Final
    @Mutable
    void setEntries(List<ClientboundPlayerInfoUpdatePacket.Entry> entries);
}
