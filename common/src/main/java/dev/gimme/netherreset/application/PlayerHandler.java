package dev.gimme.netherreset.application;

import dev.gimme.netherreset.domain.inventory.EnderChestManager;
import dev.gimme.netherreset.domain.inventory.InventoryManager;
import dev.gimme.netherreset.domain.inventory.NetherRespawnManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;

public class PlayerHandler {

    private final InventoryManager inventoryManager;
    private final EnderChestManager enderChestManager;
    private final NetherRespawnManager netherRespawnManager;

    public PlayerHandler(InventoryManager inventoryManager, EnderChestManager enderChestManager,
                         NetherRespawnManager netherRespawnManager) {
        this.inventoryManager = inventoryManager;
        this.enderChestManager = enderChestManager;
        this.netherRespawnManager = netherRespawnManager;
    }

    public void onPlayerChangeWorld(ServerPlayer player, ResourceKey<Level> fromDimension, ResourceKey<Level> toDimension) {
        inventoryManager.switchInventoryBasedOnDimension(player, fromDimension, toDimension);
        enderChestManager.onChangeDimension(player, fromDimension, toDimension);
        if (toDimension == Level.NETHER && fromDimension != Level.NETHER) {
            netherRespawnManager.recordNetherEntry(player);
        }
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

    /**
     * Picks the position a respawning player should land at, redirecting a Nether death back into the Nether when
     * the feature is on (otherwise returns the position vanilla already computed). Called from the respawn mixin
     * before the new player is built, so the player is created straight into the Nether — no post-respawn teleport.
     */
    public TeleportTransition resolveRespawn(ServerPlayer player, ResourceKey<Level> deathDimension, TeleportTransition original) {
        return netherRespawnManager.resolveRespawn(player, deathDimension, original);
    }
}
