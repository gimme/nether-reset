package dev.gimme.netherreset.mixin;

import dev.gimme.netherreset.Main;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevents items and other entities from teleporting between the Nether and other dimensions.
 */
@Mixin(Entity.class)
public class ItemTeleportMixin {

    @Inject(method = "canTeleport", at = @At("HEAD"), cancellable = true)
    private void preventEntitiesFromTeleportingToOrFromNether(Level fromLevel, Level toLevel, CallbackInfoReturnable<Boolean> cir) {
        if (fromLevel.isClientSide()) return;
        Entity instance = (Entity) (Object) this;

        if (instance instanceof Player) return;
        if (fromLevel.dimension() == toLevel.dimension()) return;
        if (fromLevel.dimension() != Level.NETHER && toLevel.dimension() != Level.NETHER) return;

        if (toLevel.dimension() == Level.NETHER && Main.INSTANCE.getServerConfig().allowEntitiesTeleportToNether()) return;
        if (fromLevel.dimension() == Level.NETHER && Main.INSTANCE.getServerConfig().allowEntitiesTeleportFromNether()) return;

        if (instance instanceof ItemEntity) {
            if (!Main.INSTANCE.getServerConfig().preventItemsFromTeleporting()) return;
        } else {
            if (!Main.INSTANCE.getServerConfig().preventOtherEntitiesFromTeleporting()) return;
        }

        cir.setReturnValue(false);
    }
}
