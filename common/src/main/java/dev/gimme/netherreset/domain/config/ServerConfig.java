package dev.gimme.netherreset.domain.config;

import net.minecraft.core.Registry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface ServerConfig {

    boolean preventItemsFromTeleporting();
    boolean preventOtherEntitiesFromTeleporting();
    boolean allowEntitiesTeleportToNether();
    boolean allowEntitiesTeleportFromNether();

    List<ItemStack> getNetherStarterItems(Registry<Item> itemRegistry);
    boolean refreshNetherStarterItemsOnDeath();
}
