package dev.gimme.netherreset.mixin;

import dev.gimme.netherreset.Main;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Redirects where a player respawns so that dying in the Nether drops them back into the Nether (at the spot they
 * last entered it) instead of sending them to their Overworld spawn — the {@code respawnInNether} feature. See
 * {@link dev.gimme.netherreset.domain.inventory.NetherRespawnManager}.
 *
 * <p>This wraps the call that decides the respawn position ({@code findRespawnPositionAndUseSpawnBlock}) rather than
 * teleporting after the fact, so the new player is built straight into the Nether — exactly how a respawn anchor
 * already works. The dying player is still in its death dimension at this point, which is how we know where it died;
 * a charged Nether respawn anchor wins because vanilla then already returns a Nether transition (left untouched).
 */
@Mixin(PlayerList.class)
public class PlayerRespawnPositionMixin {

    @Redirect(
            method = "respawn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;findRespawnPositionAndUseSpawnBlock(ZLnet/minecraft/world/level/portal/TeleportTransition$PostTeleportTransition;)Lnet/minecraft/world/level/portal/TeleportTransition;"
            ),
            require = 1
    )
    private TeleportTransition redirectNetherRespawn(ServerPlayer dyingPlayer, boolean consumeSpawnBlock,
                                                     TeleportTransition.PostTeleportTransition postTeleportTransition) {
        TeleportTransition vanilla = dyingPlayer.findRespawnPositionAndUseSpawnBlock(consumeSpawnBlock, postTeleportTransition);
        return Main.INSTANCE.getPlayerHandler().resolveRespawn(dyingPlayer, dyingPlayer.level().dimension(), vanilla);
    }
}
