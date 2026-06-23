package dev.gimme.netherreset.infrastructure;

import dev.gimme.netherreset.domain.config.ServerConfig;
import dev.gimme.netherreset.domain.util.Constants;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * {@link ServerConfig} backed by the NeoForge config system. The spec is defined once here in the common module and
 * registered per loader (natively on NeoForge, via Forge Config API Port on Fabric) as a {@code COMMON} config.
 */
public class FcapServerConfig implements ServerConfig {

    public static final String FILE_NAME = Constants.MOD_ID + "-server.toml";

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final BooleanValue PREVENT_ITEMS_FROM_TELEPORTING = BUILDER
            .comment("If items should be prevented from traveling through portals.")
            .define("preventItemsFromTeleporting", true);

    static final BooleanValue PREVENT_OTHER_ENTITIES_FROM_TELEPORTING = BUILDER
            .comment("If other entities (e.g., mobs) should be prevented from traveling through portals.")
            .define("preventOtherEntitiesFromTeleporting", true);

    static final BooleanValue ALLOW_ENTITIES_TELEPORT_TO_NETHER = BUILDER
            .comment("If items and other entities should always be allowed to teleport TO the Nether (one direction).")
            .define("allowEntitiesTeleportToNether", false);

    private static final BooleanValue ALLOW_ENTITIES_TELEPORT_FROM_NETHER = BUILDER
            .comment("If items and other entities should always be allowed to teleport FROM the Nether (one direction).")
            .define("allowEntitiesTeleportFromNether", false);

    static final BooleanValue CLEAR_ENTITY_ITEMS_ON_TELEPORT = BUILDER
            .comment("""
                    If a non-player entity that is allowed to travel through a Nether portal should have its carried
                    items wiped on the way (held/worn equipment, plus any container inventory such as a chested horse
                    or chest minecart). Prevents using mobs and vehicles to smuggle items past the per-dimension reset.
                    Only takes effect when something above lets the entity teleport in the first place.""")
            .define("clearEntityItemsOnTeleport", true);

    static final ModConfigSpec.ConfigValue<List<? extends String>> NETHER_STARTER_ITEMS = BUILDER
            .comment("""
                    List of items players get when they first enter the Nether.
                    Format: "itemId,amount"
                    Example: ["minecraft:ender_pearl,1", "minecraft:wooden_pickaxe"]""")
            .defineList("netherStarterItems", List.of(), () -> "", o -> o instanceof String);

    private static final BooleanValue REFRESH_NETHER_STARTER_ITEMS_ON_DEATH = BUILDER
            .comment("""
                    If enabled, players will receive the nether starter items again after having died in the Nether.
                    Otherwise, they only receive them the first time they enter the Nether.""")
            .define("refreshNetherStarterItemsOnDeath", false);

    static final ModConfigSpec.ConfigValue<List<? extends String>> GRACE_EFFECTS = BUILDER
            .comment("""
                    List of effects players get when they first enter the Nether.
                    Format: "effectId,durationSeconds[60],level[1]"
                    Example: ["fire_resistance,120", "absorption,60,1", "haste,60,2"]""")
            .defineList("graceEffects", List.of("fire_resistance"), () -> "", o -> o instanceof String);

    private static final BooleanValue EXTRA_LOOT_ENABLED = BUILDER
            .comment("""
                    When true, the mod injects extra loot pools with Overworld-related items (e.g. Water Bottles and Glistering Melon Slices)
                    into Nether structure chest and Piglin bartering loot tables. Setting this to false disables these custom additions.""")
            .define("extraLootEnabled", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    @Override
    public boolean preventItemsFromTeleporting() {
        return PREVENT_ITEMS_FROM_TELEPORTING.get();
    }

    @Override
    public boolean preventOtherEntitiesFromTeleporting() {
        return PREVENT_OTHER_ENTITIES_FROM_TELEPORTING.get();
    }

    @Override
    public boolean allowEntitiesTeleportToNether() {
        return ALLOW_ENTITIES_TELEPORT_TO_NETHER.get();
    }

    @Override
    public boolean allowEntitiesTeleportFromNether() {
        return ALLOW_ENTITIES_TELEPORT_FROM_NETHER.get();
    }

    @Override
    public boolean clearEntityItemsOnTeleport() {
        return CLEAR_ENTITY_ITEMS_ON_TELEPORT.get();
    }

    @Override
    public List<ItemStack> getNetherStarterItems(Registry<Item> itemRegistry) {
        return NETHER_STARTER_ITEMS.get().stream()
                .map(itemString -> {
                    String[] parts = itemString.split(",");

                    String itemIdString = parts[0].trim();
                    Holder.Reference<Item> item = null;
                    var itemIdentifier = Identifier.tryParse(itemIdString);
                    if (itemIdentifier != null) {
                        item = itemRegistry.get(itemIdentifier).orElse(null);
                    }
                    if (item == null) {
                        Constants.LOG.warn("Invalid item in starterNetherInventory: \"{}\"", itemIdString);
                        return null;
                    }

                    int amount = 1;
                    if (parts.length > 1) {
                        try {
                            amount = Integer.parseInt(parts[1].trim());
                        } catch (NumberFormatException _) {
                            Constants.LOG.warn("Invalid amount for item in starterNetherInventory: \"{}\"", itemString);
                        }
                    }

                    return new ItemStack(item.value(), amount);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public boolean refreshNetherStarterItemsOnDeath() {
        return REFRESH_NETHER_STARTER_ITEMS_ON_DEATH.get();
    }

    @Override
    public Set<GraceEffect> getGraceEffects() {
        return GRACE_EFFECTS.get().stream()
                .map(effectString -> {
                    String[] parts = effectString.split(",");

                    Identifier effectId = Identifier.tryParse(parts[0].trim());
                    if (effectId == null) {
                        Constants.LOG.warn("Invalid effectId for graceEffects: \"{}\"", effectString);
                        return null;
                    }

                    double durationSeconds = 60;
                    if (parts.length > 1) {
                        try {
                            durationSeconds = Double.parseDouble(parts[1].trim());
                        } catch (Exception _) {
                            Constants.LOG.warn("Invalid durationSeconds for graceEffects: \"{}\"", effectString);
                        }
                    }

                    int amplifier = 0;
                    if (parts.length > 2) {
                        try {
                            amplifier = Integer.parseInt(parts[2].trim()) - 1; // Config is 1-based for user-friendliness, but MobEffectInstance expects 0-based
                        } catch (NumberFormatException _) {
                            Constants.LOG.warn("Invalid level for graceEffects: \"{}\"", effectString);
                        }
                    }

                    return new GraceEffect(effectId, (int) (durationSeconds * 20), amplifier);
                })
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public boolean isExtraLootEnabled() {
        return EXTRA_LOOT_ENABLED.get();
    }
}
