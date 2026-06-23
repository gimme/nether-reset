package dev.gimme.netherreset.infrastructure;

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
