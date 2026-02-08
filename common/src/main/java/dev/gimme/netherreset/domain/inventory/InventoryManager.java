package dev.gimme.netherreset.domain.inventory;

import dev.gimme.netherreset.application.PlayerAttachmentAccessor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Manages player inventories across dimensions, specifically handling the Nether and the default dimension. It allows for
 * storing and applying inventory snapshots when players switch dimensions, die, or respawn.
 */
public class InventoryManager {

    private final PlayerAttachmentAccessor playerAttachmentAccessor;

    public InventoryManager(PlayerAttachmentAccessor playerAttachmentAccessor) {
        this.playerAttachmentAccessor = playerAttachmentAccessor;
    }

    /**
     * Switches the player's inventory based on the dimension they are moving to/from.
     */
    public void switchInventoryBasedOnDimension(ServerPlayer player, ResourceKey<Level> fromDimension, ResourceKey<Level> toDimension) {
        if (fromDimension == toDimension) return;

        if (toDimension == Level.NETHER) {
            swapToNetherInventory(player);
        } else if (fromDimension == Level.NETHER) {
            swapToDefaultInventory(player);
        }
    }

    /**
     * Clears the stored inventory for the player's current dimension. This is typically called on player death to ensure
     * that they don't retain their inventory in the dimension they died in.
     */
    public void clearStoredInventoryInCurrentDimension(ServerPlayer player) {
        DimInvData data = playerAttachmentAccessor.getOrCreateDimInvData(player);
        if (player.level().dimension() == Level.NETHER) {
            data = data.withNether(InventorySnapshot.empty());
        } else {
            data = data.withDefault(InventorySnapshot.empty());
        }
        playerAttachmentAccessor.setDimInvData(player, data);
    }

    /**
     * Sets the player's inventory to the state stored for the dimension they are in. This is typically called on player
     * respawn to ensure that they keep their inventory if they died in a different dimension.
     */
    public void applyStoredInventory(ServerPlayer player) {
        DimInvData data = playerAttachmentAccessor.getOrCreateDimInvData(player);
        if (player.level().dimension() == Level.NETHER) {
            data.netherInv().applyTo(player);
        } else {
            data.defaultInv().applyTo(player);
        }
    }

    /**
     * Swaps the player's inventory to their Nether-specific inventory.
     */
    private void swapToNetherInventory(ServerPlayer player) {
        DimInvData data = playerAttachmentAccessor.getOrCreateDimInvData(player);

        InventorySnapshot inventorySnapshot = InventorySnapshot.fromPlayer(player);
        data.netherInv().applyTo(player);

        DimInvData updated = data.withDefault(inventorySnapshot);
        playerAttachmentAccessor.setDimInvData(player, updated);
    }

    /**
     * Swaps the player's inventory back to their default inventory.
     */
    private void swapToDefaultInventory(ServerPlayer player) {
        DimInvData data = playerAttachmentAccessor.getOrCreateDimInvData(player);

        InventorySnapshot netherSnapshot = InventorySnapshot.fromPlayer(player);
        data.defaultInv().applyTo(player);

        DimInvData updated = data.withNether(netherSnapshot);
        playerAttachmentAccessor.setDimInvData(player, updated);
    }
}
