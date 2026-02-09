package dev.gimme.netherreset.application;

import dev.gimme.netherreset.domain.inventory.InventoryManager;
import dev.gimme.netherreset.domain.util.Constants;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class PlayerHandler {

    private final InventoryManager inventoryManager;

    public PlayerHandler(InventoryManager inventoryManager) {
        this.inventoryManager = inventoryManager;
    }

    public void onPlayerChangeWorld(ServerPlayer player, ResourceKey<Level> fromDimension, ResourceKey<Level> toDimension) {
        Constants.LOG.info("onPlayerChangeWorld: player={} (alive={}), from={}, to={}", player.getName().getString(), player.isAlive(), fromDimension, toDimension);
        inventoryManager.switchInventoryBasedOnDimension(player, fromDimension, toDimension);
    }

    public void onPlayerDeath(ServerPlayer player) {
        Constants.LOG.info("onPlayerDeath: player={}", player.getName().getString());
        inventoryManager.clearStoredInventoryInCurrentDimension(player);
    }

    public void onPlayerRespawn(ServerPlayer player, boolean isEndConquered) {
        Constants.LOG.info("onPlayerRespawn: player={}, isEndConquered={}", player.getName().getString(), isEndConquered);
        if (isEndConquered) {
            onPlayerChangeWorld(player, Level.END, player.level().dimension());
        } else {
            inventoryManager.applyStoredInventory(player);
        }
    }
}
