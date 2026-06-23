package dev.gimme.netherreset.application;

import dev.gimme.netherreset.domain.config.ServerConfig;
import dev.gimme.netherreset.mixin.AbstractHorseAccessor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Decides what happens to non-player entities crossing the Nether boundary, mirroring the player-side
 * inventory isolation in {@link PlayerHandler}. Covers mobs, vehicles and dropped items.
 */
public class EntityHandler {

    private final ServerConfig serverConfig;

    public EntityHandler(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    /**
     * Decides what happens when {@code entity} tries to teleport from {@code fromDimension} to
     * {@code toDimension}.
     *
     * <p>Returns {@code true} when the crossing must be blocked. When the crossing is allowed and the
     * entity is not itself a dropped item, its carried items are wiped (if configured) so it can't be used
     * to smuggle loot past the per-dimension inventory reset.
     *
     * @return {@code true} if the teleport should be prevented, {@code false} to let it proceed
     */
    public boolean shouldBlockNetherTeleport(Entity entity, ResourceKey<Level> fromDimension, ResourceKey<Level> toDimension) {
        if (entity instanceof Player) return false;
        if (fromDimension == toDimension) return false;
        if (fromDimension != Level.NETHER && toDimension != Level.NETHER) return false;

        if (!isTeleportAllowed(entity, fromDimension, toDimension)) return true;

        if (!(entity instanceof ItemEntity) && serverConfig.clearEntityItemsOnTeleport()) {
            clearCarriedItems(entity);
        }
        return false;
    }

    private boolean isTeleportAllowed(Entity entity, ResourceKey<Level> fromDimension, ResourceKey<Level> toDimension) {
        if (toDimension == Level.NETHER && serverConfig.allowEntitiesTeleportToNether()) return true;
        if (fromDimension == Level.NETHER && serverConfig.allowEntitiesTeleportFromNether()) return true;
        if (entity instanceof ItemEntity) {
            return !serverConfig.preventItemsFromTeleporting();
        }
        return !serverConfig.preventOtherEntitiesFromTeleporting();
    }

    /**
     * Empties every place a non-player entity can stash items: worn/held equipment (covers mobs that picked
     * up gear, animal body armor and saddles), an {@link InventoryCarrier} pickup inventory (villagers,
     * allays), a chested horse/donkey/mule/llama's storage, and chest-bearing vehicles (minecarts, boats).
     */
    private static void clearCarriedItems(Entity entity) {
        if (entity instanceof LivingEntity living) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                living.setItemSlot(slot, ItemStack.EMPTY);
            }
        }
        if (entity instanceof InventoryCarrier carrier) {
            carrier.getInventory().clearContent();
        }
        if (entity instanceof AbstractHorseAccessor horse) {
            horse.getInventory().clearContent();
        }
        if (entity instanceof ContainerEntity container) {
            container.clearContent();
        }
    }
}
