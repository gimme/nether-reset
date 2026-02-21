package dev.gimme.netherreset.neoforge.loot;

import com.mojang.serialization.MapCodec;
import dev.gimme.netherreset.Main;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

public record ConfigCondition() implements LootItemCondition {

    public static final ConfigCondition INSTANCE = new ConfigCondition();
    public static final MapCodec<ConfigCondition> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public LootItemConditionType getType() {
        return ModLootConditionTypes.CONFIG_CONDITION_TYPE.get();
    }

    @Override
    public boolean test(LootContext lootContext) {
        return Main.INSTANCE.getServerConfig().isExtraLootEnabled();
    }
}
