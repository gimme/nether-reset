package dev.gimme.netherreset.fabric.loot;

import dev.gimme.netherreset.domain.loot.LootPools;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class LootTableModifiers {

    public static void modifyLootTables() {
        LootTableEvents.MODIFY.register((resourceKey, builder, lootTableSource, provider) -> {
            if (BuiltInLootTables.NETHER_BRIDGE.equals(resourceKey)) {
                builder.withPool(LootPools.FORTRESS);
            }
        });
    }
}
