package dev.gimme.netherreset.domain.inventory;

import dev.gimme.netherreset.application.PlayerAttachmentAccessor;
import dev.gimme.netherreset.domain.config.ServerConfig;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Keeps players who die in the Nether in the Nether, mirroring how a respawn anchor works: rather than teleporting
 * the player after they respawn, the respawn position itself is redirected so the new player is built straight into
 * the Nether (see {@code PlayerRespawnPositionMixin}).
 *
 * <p>The target is the safe portal-exit spot recorded each time the player crosses <em>into</em> the Nether. A
 * charged respawn anchor in the Nether still wins — vanilla already keeps such players in the Nether, so the
 * redirect bows out whenever the computed respawn is already a Nether one. With no recorded entry (or no Nether at
 * all), it falls back to vanilla and the player respawns in the Overworld as before.
 *
 * <p>Once the player is dropped back into the Nether, the existing respawn handling does the rest: the per-dimension
 * inventory swap applies the (death-reset) Nether inventory, and the Ender Chest stays isolated because the player
 * never left the Nether.
 */
public class NetherRespawnManager {

    private final PlayerAttachmentAccessor playerAttachmentAccessor;
    private final ServerConfig serverConfig;

    public NetherRespawnManager(PlayerAttachmentAccessor playerAttachmentAccessor, ServerConfig serverConfig) {
        this.playerAttachmentAccessor = playerAttachmentAccessor;
        this.serverConfig = serverConfig;
    }

    /**
     * Records where the player just arrived in the Nether as the spot to respawn them at after a Nether death. Always
     * stored (the config only gates the redirect), so toggling the feature on mid-playthrough works right away.
     */
    public void recordNetherEntry(ServerPlayer player) {
        DimInvData data = playerAttachmentAccessor.getOrCreateDimInvData(player);
        NetherRespawnPoint entry = new NetherRespawnPoint(player.position(), player.getYRot());
        playerAttachmentAccessor.setDimInvData(player, data.withNetherRespawn(entry));
    }

    /**
     * Given the respawn position vanilla computed for a player, returns the position to actually use. Redirects to
     * the player's recorded Nether entry when they died in the Nether and the feature is on; otherwise returns the
     * original transition unchanged.
     *
     * @param deathDimension the dimension the player died in (read from the dying player before respawn relocates it)
     */
    public TeleportTransition resolveRespawn(ServerPlayer player, ResourceKey<Level> deathDimension, TeleportTransition original) {
        if (!serverConfig.respawnInNether()) return original;
        if (deathDimension != Level.NETHER) return original;                    // didn't die in the Nether
        if (original.newLevel().dimension() == Level.NETHER) return original;   // anchor/bed already keeps them there

        Optional<NetherRespawnPoint> entry = playerAttachmentAccessor.getOrCreateDimInvData(player).netherRespawn();
        if (entry.isEmpty()) return original;                                   // no entry recorded — fall back to vanilla

        MinecraftServer server = original.newLevel().getServer();
        ServerLevel nether = server.getLevel(Level.NETHER);
        if (nether == null) return original;                                    // no Nether loaded — fall back to vanilla

        NetherRespawnPoint point = entry.get();
        return new TeleportTransition(nether, point.pos(), Vec3.ZERO, point.yRot(), 0f, original.postTeleportTransition());
    }
}
