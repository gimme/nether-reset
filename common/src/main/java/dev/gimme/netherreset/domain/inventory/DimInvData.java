package dev.gimme.netherreset.domain.inventory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Represents inventory data by dimension for a player.
 */
public record DimInvData(
        InventorySnapshot defaultInv,
        Optional<InventorySnapshot> netherInv
) {

    public static final Codec<DimInvData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            InventorySnapshot.CODEC.optionalFieldOf("defaultInv", InventorySnapshot.empty()).forGetter(DimInvData::defaultInv),
            InventorySnapshot.CODEC.optionalFieldOf("netherInv").forGetter(DimInvData::netherInv)
    ).apply(inst, DimInvData::new));

    public static DimInvData empty() {
        return new DimInvData(InventorySnapshot.empty(), Optional.empty());
    }

    public DimInvData withDefault(@NotNull InventorySnapshot defaultInv) {
        return new DimInvData(defaultInv, netherInv);
    }

    public DimInvData withNether(@Nullable InventorySnapshot netherInv) {
        return new DimInvData(defaultInv, Optional.ofNullable(netherInv));
    }
}
