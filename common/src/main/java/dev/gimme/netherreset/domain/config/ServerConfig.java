package dev.gimme.netherreset.domain.config;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Set;

public interface ServerConfig {

    boolean preventItemsFromTeleporting();
    boolean preventOtherEntitiesFromTeleporting();
    boolean allowEntitiesTeleportToNether();
    boolean allowEntitiesTeleportFromNether();
    boolean clearEntityItemsOnTeleport();
    boolean isolateNetherEnderChest();

    List<ItemStack> getNetherStarterItems(Registry<Item> itemRegistry);
    boolean refreshNetherStarterItemsOnDeath();

    Set<GraceEffect> getGraceEffects();

    boolean isExtraLootEnabled();

    boolean isAncientCityMapTradeEnabled();

    boolean isCartographerLevelingTradeEnabled();

    record GraceEffect(
            Identifier effectId,
            int duration,
            int amplifier
    ) {}
}
