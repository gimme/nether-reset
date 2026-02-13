package dev.gimme.netherreset.domain.loot;

import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public class LootPools {

    /**
     * Extra loot for Nether Fortress chests.
     */
    public static LootPool.Builder FORTRESS = LootPool.lootPool()
            .setRolls(ConstantValue.exactly(1))
            .add(EmptyLootItem.emptyItem().setWeight(8))
            .add(LootItem.lootTableItem(Items.POTION).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
            .add(LootItem.lootTableItem(Items.SPLASH_POTION).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))));
}
