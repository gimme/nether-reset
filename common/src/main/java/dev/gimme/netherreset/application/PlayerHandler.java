package dev.gimme.netherreset.application;

import dev.gimme.netherreset.domain.inventory.InventoryManager;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class PlayerHandler {

    private final InventoryManager inventoryManager;

    public PlayerHandler(InventoryManager inventoryManager) {
        this.inventoryManager = inventoryManager;
    }

    public void onPlayerChangeWorld(ServerPlayer player, ResourceKey<Level> fromDimension, ResourceKey<Level> toDimension) {
        inventoryManager.switchInventoryBasedOnDimension(player, fromDimension, toDimension);
    }

    public void onPlayerDeath(ServerPlayer player) {
        inventoryManager.clearStoredInventoryInCurrentDimension(player);
    }

    public void onPlayerRespawn(ServerPlayer player) {
        inventoryManager.applyStoredInventory(player);
    }
}
