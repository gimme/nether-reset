package dev.gimme.netherreset.application;

import dev.gimme.netherreset.domain.bargain.BargainData;
import net.minecraft.world.entity.npc.villager.Villager;

/**
 * Loader-agnostic access to the per-villager {@link BargainData} attachment, mirroring
 * {@link PlayerAttachmentAccessor}. Implemented per loader (Fabric/NeoForge) since attachment registration differs.
 */
public interface VillagerAttachmentAccessor {

    BargainData getBargainData(Villager villager);
    void setBargainData(Villager villager, BargainData value);
}
