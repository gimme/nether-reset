package dev.gimme.netherreset.fabric;

import dev.gimme.netherreset.application.PlayerAttachmentAccessor;
import dev.gimme.netherreset.domain.inventory.DimInvData;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.server.level.ServerPlayer;

public class FabricAttachmentAccessor implements PlayerAttachmentAccessor {

    private final AttachmentType<DimInvData> dimInvAttachment;

    public FabricAttachmentAccessor(AttachmentType<DimInvData> dimInvAttachment) {
        this.dimInvAttachment = dimInvAttachment;
    }

    @Override
    public DimInvData getOrCreateDimInvData(ServerPlayer player) {
        return player.getAttachedOrCreate(dimInvAttachment);
    }

    @Override
    public void setDimInvData(ServerPlayer player, DimInvData value) {
        player.setAttached(dimInvAttachment, value);
    }
}
