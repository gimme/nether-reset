package dev.gimme.netherreset.infrastructure;

import dev.gimme.netherreset.domain.config.ServerConfig;
import dev.gimme.netherreset.infrastructure.ModConfigSpec.ConfigValue;

public class NightServerConfig extends ServerConfig {

    public static final ModConfigSpec SPEC = new ModConfigSpec();

    private static final ConfigValue<Boolean> PREVENT_ITEMS_FROM_TELEPORTING = SPEC.variable()
            .comment("If items should be prevented from traveling through portals.")
            .define("preventItemsFromTeleporting", true);

    private static final ConfigValue<Boolean> PREVENT_ENTITIES_FROM_TELEPORTING = SPEC.variable()
            .comment("If other entities (e.g., mobs) should be prevented from traveling through portals.")
            .define("preventEntitiesFromTeleporting", true);

    private static final ConfigValue<Boolean> ALLOW_TELEPORT_TO_NETHER = SPEC.variable()
            .comment("If items and entities should always be allowed to teleport TO the Nether (one direction).")
            .define("allowTeleportToNether", false);

    private static final ConfigValue<Boolean> ALLOW_TELEPORT_FROM_NETHER = SPEC.variable()
            .comment("If items and entities should always be allowed to teleport FROM the Nether (one direction).")
            .define("allowTeleportFromNether", false);

    @Override
    public boolean preventItemsFromTeleporting() {
        return PREVENT_ITEMS_FROM_TELEPORTING.get();
    }

    @Override
    public boolean preventEntitiesFromTeleporting() {
        return PREVENT_ENTITIES_FROM_TELEPORTING.get();
    }

    @Override
    public boolean allowTeleportToNether() {
        return ALLOW_TELEPORT_TO_NETHER.get();
    }

    @Override
    public boolean allowTeleportFromNether() {
        return ALLOW_TELEPORT_FROM_NETHER.get();
    }
}
