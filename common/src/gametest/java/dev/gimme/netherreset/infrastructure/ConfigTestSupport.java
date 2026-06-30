package dev.gimme.netherreset.infrastructure;

import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

import java.util.List;

/**
 * Test-only handles to {@link FcapServerConfig} values. Lives in the gametest source set's
 * {@code infrastructure} package so it can reach the package-private config fields; production code
 * still exposes only the read-only getters.
 */
public final class ConfigTestSupport {

    public static final ConfigValue<List<? extends String>> GRACE_EFFECTS = FcapServerConfig.GRACE_EFFECTS;

    public static final ConfigValue<List<? extends String>> NETHER_STARTER_ITEMS = FcapServerConfig.NETHER_STARTER_ITEMS;

    public static final BooleanValue PREVENT_OTHER_ENTITIES_FROM_TELEPORTING = FcapServerConfig.PREVENT_OTHER_ENTITIES_FROM_TELEPORTING;

    public static final BooleanValue ALLOW_ENTITIES_TELEPORT_TO_NETHER = FcapServerConfig.ALLOW_ENTITIES_TELEPORT_TO_NETHER;

    public static final BooleanValue CLEAR_ENTITY_ITEMS_ON_TELEPORT = FcapServerConfig.CLEAR_ENTITY_ITEMS_ON_TELEPORT;

    public static final BooleanValue ISOLATE_NETHER_ENDER_CHEST = FcapServerConfig.ISOLATE_NETHER_ENDER_CHEST;

    public static final BooleanValue RESPAWN_IN_NETHER = FcapServerConfig.RESPAWN_IN_NETHER;

    private ConfigTestSupport() {
    }

    /**
     * A restore handle whose {@code close()} throws nothing, so it reads cleanly in try-with-resources.
     */
    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }

    /**
     * Mutates the live config and returns a scope that restores the previous value on close, keeping tests isolated.
     */
    public static <T> Scope override(ConfigValue<T> config, T value) {
        T previous = config.get();
        config.set(value);
        return () -> config.set(previous);
    }
}
