package dev.gimme.netherreset.domain.loot;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.Set;

/**
 * Configuration of extra loot pools to be added to existing loot tables.
 * Note: NeoForge requires the data gen task for changes to take effect.
 */
public class ModLootConfig {

    private static final LootPool.Builder BASTION_LOOT = LootPool.lootPool()
        .setRolls(ConstantValue.exactly(3))
        .add(EmptyLootItem.emptyItem().setWeight(20))
        .add(LootItem.lootTableItem(Items.MELON_SLICE).setWeight(2).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
        .add(LootItem.lootTableItem(Items.MELON_SLICE).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
        .add(LootItem.lootTableItem(Items.GUNPOWDER).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))));

    private static final LootPool.Builder BASTION_TREASURE_LOOT = LootPool.lootPool()
        .setRolls(ConstantValue.exactly(3))
        .add(EmptyLootItem.emptyItem().setWeight(20))
        .add(LootItem.lootTableItem(Items.MELON).setWeight(2).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
        .add(LootItem.lootTableItem(Items.MELON_SLICE).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 5))))
        .add(LootItem.lootTableItem(Items.GUNPOWDER).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 8))));

    private static final LootPool.Builder FORTRESS_LOOT = LootPool.lootPool()
        .setRolls(ConstantValue.exactly(1))
        .add(EmptyLootItem.emptyItem().setWeight(7))
        .add(LootItem.lootTableItem(Items.POTION).setWeight(4).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
        .add(LootItem.lootTableItem(Items.SPLASH_POTION).setWeight(1).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))));

    public static final Set<ExtraLootPool> EXTRA_LOOT_POOLS = Set.of(
        new ExtraLootPool("bastion", BASTION_LOOT, LootContextParamSets.CHEST, Set.of(
            BuiltInLootTables.BASTION_BRIDGE,
            BuiltInLootTables.BASTION_HOGLIN_STABLE,
            BuiltInLootTables.BASTION_OTHER
        )),
        new ExtraLootPool("bastion_treasure", BASTION_TREASURE_LOOT, LootContextParamSets.CHEST, Set.of(BuiltInLootTables.BASTION_TREASURE)),
        new ExtraLootPool("fortress", FORTRESS_LOOT, LootContextParamSets.CHEST, Set.of(BuiltInLootTables.NETHER_BRIDGE))
    );

    public record ExtraLootPool(
        String name,
        LootPool.Builder content,
        ContextKeySet context,
        Set<ResourceKey<LootTable>> tablesToModify
    ) {
        public LootTable.Builder toLootTable() {
            return LootTable.lootTable().withPool(content);
        }
    }
}
