package dev.gimme.netherreset.mixin;

import dev.gimme.netherreset.Main;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
        if (fromLevel.dimension() == toLevel.dimension()) return;
        if (fromLevel.dimension() != Level.NETHER && toLevel.dimension() != Level.NETHER) return;

        if (toLevel.dimension() == Level.NETHER && Main.INSTANCE.getServerConfig().allowTeleportToNether()) return;
        if (fromLevel.dimension() == Level.NETHER && Main.INSTANCE.getServerConfig().allowTeleportFromNether()) return;

        Entity instance = (Entity) (Object) this;
        if (instance.getType() == EntityType.ITEM) {
            if (!Main.INSTANCE.getServerConfig().preventItemsFromTeleporting()) return;
        } else if (instance.getType() != EntityType.PLAYER) {
            if (!Main.INSTANCE.getServerConfig().preventEntitiesFromTeleporting()) return;
        }

        cir.setReturnValue(false);
    }
}
