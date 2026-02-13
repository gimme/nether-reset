package dev.gimme.netherreset.domain.loot;

import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

/**
 * Configuration of extra loot pools to be added to existing loot tables.
 */
public class ExtraLootPools {

    public static LootPool.Builder FORTRESS = LootPool.lootPool()
            .setRolls(ConstantValue.exactly(1))
            .add(EmptyLootItem.emptyItem().setWeight(8))
            .add(LootItem.lootTableItem(Items.POTION).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
            .add(LootItem.lootTableItem(Items.SPLASH_POTION).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))));

    public static LootPool.Builder BASTION = LootPool.lootPool()
            .setRolls(UniformGenerator.between(1, 2))
            .add(EmptyLootItem.emptyItem().setWeight(10))
            .add(LootItem.lootTableItem(Items.MELON_SLICE).setWeight(1).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
            .add(LootItem.lootTableItem(Items.MELON_SLICE).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
            .add(LootItem.lootTableItem(Items.GUNPOWDER).setWeight(1).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))));

    public static LootPool.Builder BASTION_TREASURE = LootPool.lootPool()
            .setRolls(UniformGenerator.between(2, 3))
            .add(EmptyLootItem.emptyItem().setWeight(10))
            .add(LootItem.lootTableItem(Items.MELON).setWeight(2).apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
            .add(LootItem.lootTableItem(Items.GUNPOWDER).setWeight(1).apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 8))));
}
