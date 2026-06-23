package dev.gimme.netherreset.fabric;

import dev.gimme.netherreset.application.VillagerAttachmentAccessor;
import dev.gimme.netherreset.domain.bargain.BargainData;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.entity.npc.villager.Villager;

public class FabricVillagerAttachmentAccessor implements VillagerAttachmentAccessor {

    private final AttachmentType<BargainData> bargainAttachment;

    public FabricVillagerAttachmentAccessor(AttachmentType<BargainData> bargainAttachment) {
        this.bargainAttachment = bargainAttachment;
    }

    @Override
    public BargainData getBargainData(Villager villager) {
        return villager.getAttachedOrCreate(bargainAttachment);
    }

    @Override
    public void setBargainData(Villager villager, BargainData value) {
        villager.setAttached(bargainAttachment, value);
    }
}
