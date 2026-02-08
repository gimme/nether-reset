package dev.gimme.netherreset.neoforge;

import dev.gimme.netherreset.application.PlayerAttachmentAccessor;
import dev.gimme.netherreset.domain.inventory.DimInvData;
import net.minecraft.server.level.ServerPlayer;

public class NeoForgeAttachmentAccessor implements PlayerAttachmentAccessor {

    @Override
    public DimInvData getOrCreateDimInvData(ServerPlayer player) {
        return player.getData(NeoForgeAttachments.DIM_INV.get());
    }

    @Override
    public void setDimInvData(ServerPlayer player, DimInvData value) {
        player.setData(NeoForgeAttachments.DIM_INV.get(), value);
    }
}
