package dev.gimme.netherreset.neoforge;

import dev.gimme.netherreset.application.VillagerAttachmentAccessor;
import dev.gimme.netherreset.domain.bargain.BargainData;
import net.minecraft.world.entity.npc.villager.Villager;

public class NeoForgeVillagerAttachmentAccessor implements VillagerAttachmentAccessor {

    @Override
    public BargainData getBargainData(Villager villager) {
        return villager.getData(NeoForgeAttachments.BARGAIN.get());
    }

    @Override
    public void setBargainData(Villager villager, BargainData value) {
        villager.setData(NeoForgeAttachments.BARGAIN.get(), value);
    }
}
