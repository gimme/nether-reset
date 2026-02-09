package dev.gimme.netherreset.domain.inventory;

import dev.gimme.netherreset.Main;
import dev.gimme.netherreset.application.PlayerAttachmentAccessor;
import dev.gimme.netherreset.domain.config.ServerConfig;
import dev.gimme.netherreset.domain.util.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Manages player inventory switching based on dimension.
 */
public class InventoryManager {

    private final PlayerAttachmentAccessor playerAttachmentAccessor;
    private final ServerConfig serverConfig;

    public InventoryManager(PlayerAttachmentAccessor playerAttachmentAccessor, ServerConfig serverConfig) {
        this.playerAttachmentAccessor = playerAttachmentAccessor;
        this.serverConfig = serverConfig;
    }

    /**
     * Switches the player's inventory based on the dimension they are moving to/from.
     */
    public void switchInventoryBasedOnDimension(ServerPlayer player, ResourceKey<Level> fromDimension, ResourceKey<Level> toDimension) {
        if (fromDimension == toDimension) return;

        storeInventory(player, fromDimension, InventorySnapshot.fromPlayer(player));
        InventorySnapshot newInv = getOrCreateInventory(player, toDimension);
        newInv.applyTo(player);
    }

    @NotNull
    private InventorySnapshot getOrCreateInventory(ServerPlayer player, ResourceKey<Level> dimension) {
        DimInvData data = playerAttachmentAccessor.getOrCreateDimInvData(player);
        if (dimension == Level.NETHER) {
            if (data.netherInv().isEmpty()) {
                var itemRegistry = player.registryAccess().lookupOrThrow(Registries.ITEM);
                var starterInv = serverConfig.getNetherStarterItems(itemRegistry);
                data = data.withNether(InventorySnapshot.of(starterInv));
                playerAttachmentAccessor.setDimInvData(player, data);
            }
            return data.netherInv().get();
        } else {
            return data.defaultInv();
        }
    }

    private void storeInventory(ServerPlayer player, ResourceKey<Level> dimension, InventorySnapshot snapshot) {
        DimInvData data = playerAttachmentAccessor.getOrCreateDimInvData(player);
        if (dimension == Level.NETHER) {
            if (data.netherInv().isEmpty()) return;
            data = data.withNether(snapshot);
        } else {
            data = data.withDefault(snapshot);
        }
        Constants.LOG.info("Storing inventory for player {} in dimension {}: {}", player.getName().getString(), dimension, snapshot);
        playerAttachmentAccessor.setDimInvData(player, data);
    }

    /**
     * Clears the stored inventory for the player's current dimension.
     */
    public void clearStoredInventoryInCurrentDimension(ServerPlayer player) {
        DimInvData data = playerAttachmentAccessor.getOrCreateDimInvData(player);
        if (player.level().dimension() == Level.NETHER) {
            var netherInv = Main.INSTANCE.getServerConfig().refreshNetherStarterItemsOnDeath()
                    ? null
                    : InventorySnapshot.empty();
            data = data.withNether(netherInv);
        } else {
            data = data.withDefault(InventorySnapshot.empty());
        }
        Constants.LOG.info("Clearing stored inventory for player {} in dimension {}", player.getName().getString(), player.level().dimension());
        playerAttachmentAccessor.setDimInvData(player, data);
    }

    /**
     * Sets the player's inventory to the state stored for the dimension they are in.
     */
    public void applyStoredInventory(ServerPlayer player) {
        var currentDimension = player.level().dimension();
        Constants.LOG.info("Applying stored inventory for player {} in dimension {}", player.getName().getString(), currentDimension);
        getOrCreateInventory(player, currentDimension).applyTo(player);
    }
}
