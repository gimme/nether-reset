package dev.gimme.netherreset.domain.inventory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Represents inventory data by dimension for a player.
 *
 * @param defaultInv            the inventory used in every non-Nether dimension
 * @param netherInv             the Nether inventory, absent until the player first enters the Nether
 * @param stashedOverworldEnder the Overworld Ender Chest contents, set aside while the player is in the Nether
 *                              so the Nether gets its own Ender Chest; present iff the player is currently isolated
 * @param netherEnder           the Nether Ender Chest stash. This persists across Nether trips and is what the
 *                              Recovery Compass / Echo Shard ritual spits back out in the Overworld
 */
public record DimInvData(
        InventorySnapshot defaultInv,
        Optional<InventorySnapshot> netherInv,
        Optional<InventorySnapshot> stashedOverworldEnder,
        Optional<InventorySnapshot> netherEnder
) {

    public static final Codec<DimInvData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            InventorySnapshot.CODEC.optionalFieldOf("defaultInv", InventorySnapshot.empty()).forGetter(DimInvData::defaultInv),
            InventorySnapshot.CODEC.optionalFieldOf("netherInv").forGetter(DimInvData::netherInv),
            InventorySnapshot.CODEC.optionalFieldOf("stashedOverworldEnder").forGetter(DimInvData::stashedOverworldEnder),
            InventorySnapshot.CODEC.optionalFieldOf("netherEnder").forGetter(DimInvData::netherEnder)
    ).apply(inst, DimInvData::new));

    public static DimInvData empty() {
        return new DimInvData(InventorySnapshot.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public DimInvData withDefault(@NotNull InventorySnapshot defaultInv) {
        return new DimInvData(defaultInv, netherInv, stashedOverworldEnder, netherEnder);
    }

    public DimInvData withNether(@Nullable InventorySnapshot netherInv) {
        return new DimInvData(defaultInv, Optional.ofNullable(netherInv), stashedOverworldEnder, netherEnder);
    }

    public DimInvData withStashedOverworldEnder(@Nullable InventorySnapshot stashedOverworldEnder) {
        return new DimInvData(defaultInv, netherInv, Optional.ofNullable(stashedOverworldEnder), netherEnder);
    }

    public DimInvData withNetherEnder(@Nullable InventorySnapshot netherEnder) {
        return new DimInvData(defaultInv, netherInv, stashedOverworldEnder, Optional.ofNullable(netherEnder));
    }
}
