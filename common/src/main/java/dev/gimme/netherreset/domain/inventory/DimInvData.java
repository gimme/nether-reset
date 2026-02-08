package dev.gimme.netherreset.domain.inventory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * Data class for storing both the default and nether inventories together, so they can be easily serialized and deserialized when saving to disk.
 */
public record DimInvData(
        InventorySnapshot defaultInv,
        InventorySnapshot netherInv
) {

    public static final Codec<DimInvData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            InventorySnapshot.CODEC.optionalFieldOf("defaultInv", InventorySnapshot.empty()).forGetter(DimInvData::defaultInv),
            InventorySnapshot.CODEC.optionalFieldOf("netherInv", InventorySnapshot.empty()).forGetter(DimInvData::netherInv)
    ).apply(inst, DimInvData::new));

    public static DimInvData empty() {
        return new DimInvData(InventorySnapshot.empty(), InventorySnapshot.empty());
    }

    public DimInvData withDefault(@NotNull InventorySnapshot defaultInv) {
        return new DimInvData(defaultInv, netherInv);
    }

    public DimInvData withNether(@NotNull InventorySnapshot netherInv) {
        return new DimInvData(defaultInv, netherInv);
    }
}
