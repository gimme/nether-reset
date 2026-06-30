package dev.gimme.netherreset.domain.inventory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.Vec3;

/**
 * Where a player re-enters the Nether after dying there. Captured as the safe portal-exit spot the last time the
 * player crossed <em>into</em> the Nether, and handed back as the respawn position when the {@code respawnInNether}
 * feature redirects a Nether death (see {@link NetherRespawnManager}). Only the yaw is kept — a respawning player
 * faces the horizon, so the pitch is always reset to level.
 */
public record NetherRespawnPoint(
        Vec3 pos,
        float yRot
) {

    public static final Codec<NetherRespawnPoint> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Vec3.CODEC.fieldOf("pos").forGetter(NetherRespawnPoint::pos),
            Codec.FLOAT.optionalFieldOf("yRot", 0f).forGetter(NetherRespawnPoint::yRot)
    ).apply(inst, NetherRespawnPoint::new));
}
