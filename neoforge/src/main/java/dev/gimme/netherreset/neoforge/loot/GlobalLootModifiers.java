package dev.gimme.netherreset.neoforge.loot;

import dev.gimme.netherreset.domain.util.Constants;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.AddTableLootModifier;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;

import java.util.concurrent.CompletableFuture;

public class GlobalLootModifiers extends GlobalLootModifierProvider {

    public GlobalLootModifiers(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, Constants.MOD_ID);
    }

    @Override
    protected void start() {
        this.add(
                FortressLootProvider.KEY.identifier().getPath() + "_modifier",
                new AddTableLootModifier(
                        new LootItemCondition[]{LootTableIdCondition.builder(BuiltInLootTables.NETHER_BRIDGE.identifier()).build()},
                        FortressLootProvider.KEY
                )
        );
    }
}
