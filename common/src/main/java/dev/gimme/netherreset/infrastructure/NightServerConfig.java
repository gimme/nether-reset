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

    @Override
    public boolean preventItemsFromTeleporting() {
        return PREVENT_ITEMS_FROM_TELEPORTING.get();
    }

    @Override
    public boolean preventEntitiesFromTeleporting() {
        return PREVENT_ENTITIES_FROM_TELEPORTING.get();
    }
}
