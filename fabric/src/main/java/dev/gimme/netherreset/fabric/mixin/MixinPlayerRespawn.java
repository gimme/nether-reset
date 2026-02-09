package dev.gimme.netherreset.fabric.mixin;

import dev.gimme.netherreset.Main;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets the mod know when a player respawns.
 */
@Mixin(PlayerList.class)
public class MixinPlayerRespawn {

    @Inject(method = "respawn", at = @At(value = "RETURN"), require = 1)
    private void onPlayerRespawn(ServerPlayer player, boolean keepInventory, Entity.RemovalReason reason, CallbackInfoReturnable<ServerPlayer> cir) {
        Main.INSTANCE.getPlayerHandler().onPlayerRespawn(cir.getReturnValue(), keepInventory);
    }
}
