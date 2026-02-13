package dev.gimme.netherreset.fabric.loot;

import dev.gimme.netherreset.domain.loot.ExtraLootPools;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class LootTableModifiers {

    public static void modifyLootTables() {
        LootTableEvents.MODIFY.register((resourceKey, builder, lootTableSource, provider) -> {
            if (BuiltInLootTables.BASTION_BRIDGE.equals(resourceKey)) {
                builder.withPool(ExtraLootPools.BASTION);
            } else if (BuiltInLootTables.BASTION_HOGLIN_STABLE.equals(resourceKey)) {
                builder.withPool(ExtraLootPools.BASTION);
            } else if (BuiltInLootTables.BASTION_OTHER.equals(resourceKey)) {
                builder.withPool(ExtraLootPools.BASTION);
            } else if (BuiltInLootTables.BASTION_TREASURE.equals(resourceKey)) {
                builder.withPool(ExtraLootPools.BASTION_TREASURE);
            } else if (BuiltInLootTables.NETHER_BRIDGE.equals(resourceKey)) {
                builder.withPool(ExtraLootPools.FORTRESS);
            }
        });
    }
}
