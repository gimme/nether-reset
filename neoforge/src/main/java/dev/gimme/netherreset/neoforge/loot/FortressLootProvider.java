package dev.gimme.netherreset.neoforge.loot;

import dev.gimme.netherreset.domain.loot.ExtraLootPools;
import dev.gimme.netherreset.domain.util.Constants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class FortressLootProvider extends LootTableProvider {

    public static final ResourceKey<LootTable> KEY = ResourceKey.create(
            Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "fortress_loot_table")
    );

    public FortressLootProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(
                output,
                Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(provider -> FortressLootProvider::generate, LootContextParamSets.CHEST)),
                registries
        );
    }

    private static void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        output.accept(KEY, LootTable.lootTable().withPool(ExtraLootPools.FORTRESS));
    }
}
