package dev.gimme.netherreset.neoforge;

import dev.gimme.netherreset.domain.inventory.DimInvData;
import dev.gimme.netherreset.domain.util.Constants;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.attachment.AttachmentType;

import java.util.function.Supplier;

public final class NeoForgeAttachments {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Constants.MOD_ID);

    public static final Supplier<AttachmentType<DimInvData>> DIM_INV =
            ATTACHMENT_TYPES.register(Constants.PLAYER_DIM_INVENTORY_ID,
                    () -> AttachmentType.builder(DimInvData::empty)
                            .serialize(DimInvData.CODEC.fieldOf("data"))
                            .copyOnDeath()
                            .build()
            );

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}
