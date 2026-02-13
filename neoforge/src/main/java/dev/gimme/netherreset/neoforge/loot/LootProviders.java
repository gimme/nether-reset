package dev.gimme.netherreset.neoforge.loot;

import dev.gimme.netherreset.domain.loot.ModLootConfig;
import dev.gimme.netherreset.domain.util.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.AnyOfCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.AddTableLootModifier;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Set;

/**
 * Generates loot tables and loot modifiers that add the custom loot pools defined in {@link ModLootConfig} to existing loot tables.
 */
public class LootProviders {

    @SubscribeEvent
    private void gatherData(GatherDataEvent.Server event) {
        var packOutput = event.getGenerator().getPackOutput();
        var lookupProvider = event.getLookupProvider();

        // Generate custom loot tables
        event.addProvider(
            new LootTableProvider(
                packOutput,
                Set.of(),
                ModLootConfig.EXTRA_LOOT_POOLS.stream().map(extraPool ->
                    new LootTableProvider.SubProviderEntry(
                        provider -> output -> output.accept(getLootTableKey(extraPool), extraPool.toLootTable()),
                        extraPool.context()
                    )
                ).toList(),
                lookupProvider
            )
        );

        // Generate NeoForge loot modifiers that merge the custom loot tables with existing loot tables.
        event.addProvider(new GlobalLootModifierProvider(packOutput, lookupProvider, Constants.MOD_ID) {
            @Override
            protected void start() {
                ModLootConfig.EXTRA_LOOT_POOLS.forEach(extraPool -> this.add(
                    extraPool.name() + "_loot_table_modifier",
                    new AddTableLootModifier(
                        new LootItemCondition[]{createLootTableIdCondition(extraPool)},
                        getLootTableKey(extraPool)
                    )
                ));
            }
        });
    }

    private LootItemCondition createLootTableIdCondition(ModLootConfig.ExtraLootPool extraPool) {
        var tables = extraPool.tablesToModify();
        if (tables.size() == 1) {
            return LootTableIdCondition.builder(tables.iterator().next().identifier()).build();
        } else {
            return new AnyOfCondition.Builder(tables.stream().map(table -> LootTableIdCondition.builder(table.identifier())).toArray(LootItemCondition.Builder[]::new)).build();
        }
    }

    private static ResourceKey<LootTable> getLootTableKey(ModLootConfig.ExtraLootPool extraPool) {
        return ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Constants.MOD_ID, extraPool.name() + "_extra_loot_table"));
    }
}
