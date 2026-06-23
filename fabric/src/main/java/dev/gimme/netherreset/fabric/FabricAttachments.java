package dev.gimme.netherreset.fabric;

import dev.gimme.netherreset.domain.inventory.DimInvData;
import dev.gimme.netherreset.domain.util.Constants;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

public final class FabricAttachments {

    public static final AttachmentType<DimInvData> DIM_INV = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, Constants.PLAYER_DIM_INVENTORY_ID),
            builder -> builder
                    .initializer(DimInvData::empty)
                    .persistent(DimInvData.CODEC)
                    .copyOnDeath()
    );
}
