package dev.gimme.netherreset.application;

import dev.gimme.netherreset.domain.inventory.EnderChestManager;
import dev.gimme.netherreset.domain.inventory.InventoryManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class PlayerHandler {

    private final InventoryManager inventoryManager;
    private final EnderChestManager enderChestManager;

    public PlayerHandler(InventoryManager inventoryManager, EnderChestManager enderChestManager) {
        this.inventoryManager = inventoryManager;
        this.enderChestManager = enderChestManager;
    }

    public void onPlayerChangeWorld(ServerPlayer player, ResourceKey<Level> fromDimension, ResourceKey<Level> toDimension) {
        inventoryManager.switchInventoryBasedOnDimension(player, fromDimension, toDimension);
        enderChestManager.onChangeDimension(player, fromDimension, toDimension);
    }

    public void onPlayerDeath(ServerPlayer player) {
        inventoryManager.clearStoredInventoryInCurrentDimension(player);
    }

    /**
     * Handles a right-click on an Ender Chest. Returns {@code true} if the interaction was consumed (the chest
     * should not open), {@code false} to let the chest open as normal.
     */
    public boolean onUseEnderChest(ServerPlayer player, ItemStack heldItem, BlockPos chestPos) {
        return enderChestManager.onUseEnderChest(player, heldItem, chestPos);
    }

    public void onPlayerRespawn(ServerPlayer player, boolean isEndConquered) {
        if (isEndConquered) {
            onPlayerChangeWorld(player, Level.END, player.level().dimension());
        } else {
            inventoryManager.applyStoredInventory(player);
            enderChestManager.onRespawn(player);
        }
    }
}
