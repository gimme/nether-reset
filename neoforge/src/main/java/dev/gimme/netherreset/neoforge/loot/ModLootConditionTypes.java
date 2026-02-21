package dev.gimme.netherreset.neoforge.loot;

import dev.gimme.netherreset.domain.util.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModLootConditionTypes {

    public static final DeferredRegister<LootItemConditionType> REGISTRY = DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, Constants.MOD_ID);

    public static final Supplier<LootItemConditionType> CONFIG_CONDITION_TYPE = REGISTRY.register("config", () -> new LootItemConditionType(ConfigCondition.CODEC));
}
