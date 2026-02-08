package dev.gimme.netherreset.application;

import dev.gimme.netherreset.domain.inventory.DimInvData;
import net.minecraft.server.level.ServerPlayer;

public interface PlayerAttachmentAccessor {

    DimInvData getOrCreateDimInvData(ServerPlayer player);
    void setDimInvData(ServerPlayer player, DimInvData value);
}
