package dev.gimme.netherreset.mixin;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes {@link AbstractHorse}'s otherwise-protected storage container (the chest slots of a chested
 * donkey/mule/llama) so it can be wiped when a mount is allowed through a Nether portal. The saddle and
 * body armor live in equipment slots and are handled separately.
 */
@Mixin(AbstractHorse.class)
public interface AbstractHorseAccessor {

    @Accessor("inventory")
    SimpleContainer getInventory();
}
