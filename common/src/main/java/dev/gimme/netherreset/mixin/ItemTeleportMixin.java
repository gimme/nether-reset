package dev.gimme.netherreset.mixin;

import dev.gimme.netherreset.Main;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gates non-player entities crossing the Nether boundary. Blocks items and other entities from teleporting
 * to/from the Nether (per config), and wipes the carried items of any entity that is allowed through, so
 * mobs and vehicles can't be used to smuggle loot past the per-dimension reset. See
 * {@link dev.gimme.netherreset.application.EntityHandler}.
 */
@Mixin(Entity.class)
public class ItemTeleportMixin {

    @Inject(method = "canTeleport", at = @At("HEAD"), cancellable = true)
    private void preventEntitiesFromTeleportingToOrFromNether(Level fromLevel, Level toLevel, CallbackInfoReturnable<Boolean> cir) {
        if (fromLevel.isClientSide()) return;
        Entity instance = (Entity) (Object) this;

        if (Main.INSTANCE.getEntityHandler().shouldBlockNetherTeleport(instance, fromLevel.dimension(), toLevel.dimension())) {
            cir.setReturnValue(false);
        }
    }
}
