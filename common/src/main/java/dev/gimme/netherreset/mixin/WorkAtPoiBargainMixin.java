package dev.gimme.netherreset.mixin;

import dev.gimme.netherreset.Main;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.WorkAtPoi;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A villager with no bargains has nothing to restock, so there is no reason to work. Suppressing the work behavior
 * (which also covers {@code WorkAtComposter}, a subclass that inherits this check) nudges them to keep socializing
 * instead. With the bargain feature off, the check is a no-op and vanilla work resumes.
 */
@Mixin(WorkAtPoi.class)
public class WorkAtPoiBargainMixin {

    @Inject(method = "checkExtraStartConditions(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/villager/Villager;)Z", at = @At("HEAD"), cancellable = true)
    private void nether_reset$skipWorkWithoutBargains(ServerLevel level, Villager body, CallbackInfoReturnable<Boolean> cir) {
        if (Main.INSTANCE.getVillagerHandler().shouldSkipWork(body)) {
            cir.setReturnValue(false);
        }
    }
}
