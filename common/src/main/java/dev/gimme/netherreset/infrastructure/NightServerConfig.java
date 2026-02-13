package dev.gimme.netherreset.infrastructure;

import dev.gimme.netherreset.domain.config.ServerConfig;
import dev.gimme.netherreset.domain.util.Constants;
import dev.gimme.netherreset.infrastructure.ModConfigSpec.ConfigValue;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class NightServerConfig implements ServerConfig {

    public static final ModConfigSpec SPEC = new ModConfigSpec();

    private static final ConfigValue<Boolean> PREVENT_ITEMS_FROM_TELEPORTING = SPEC.variable()
            .comment("If items should be prevented from traveling through portals.")
            .define("preventItemsFromTeleporting", true);

    private static final ConfigValue<Boolean> PREVENT_OTHER_ENTITIES_FROM_TELEPORTING = SPEC.variable()
            .comment("If other entities (e.g., mobs) should be prevented from traveling through portals.")
            .define("preventOtherEntitiesFromTeleporting", true);

    private static final ConfigValue<Boolean> ALLOW_ENTITIES_TELEPORT_TO_NETHER = SPEC.variable()
            .comment("If items and other entities should always be allowed to teleport TO the Nether (one direction).")
            .define("allowEntitiesTeleportToNether", false);

    private static final ConfigValue<Boolean> ALLOW_ENTITIES_TELEPORT_FROM_NETHER = SPEC.variable()
            .comment("If items and other entities should always be allowed to teleport FROM the Nether (one direction).")
            .define("allowEntitiesTeleportFromNether", false);

    private static final ConfigValue<List<String>> NETHER_STARTER_ITEMS = SPEC.variable()
            .comment("""
                    List of items players get when they first enter the Nether.
                    Format: "itemId,amount"
                    Example: ["minecraft:ender_pearl,1", "minecraft:wooden_pickaxe"]""")
            .define("netherStarterItems", List.of());

    private static final ConfigValue<Boolean> REFRESH_NETHER_STARTER_ITEMS_ON_DEATH = SPEC.variable()
            .comment("""
                    If enabled, players will receive the nether starter items again after having died in the Nether.
                    Otherwise, they only receive them the first time they enter the Nether.""")
            .define("refreshNetherStarterItemsOnDeath", false);

    private static final ConfigValue<List<String>> GRACE_EFFECTS = SPEC.variable()
            .comment("""
                    List of effects players get when they first enter the Nether.
                    Format: "effectId,durationSeconds[60],amplifier[0]"
                    Example: ["fire_resistance,120", "absorption,60,1", "haste,60,3"]""")
            .define("graceEffects", List.of("fire_resistance"));

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
                        } catch (NumberFormatException e) {
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
                        Constants.LOG.warn("Invalid effectId for graceEffect: \"{}\"", effectString);
                        return null;
                    }

                    double durationSeconds = 60;
                    if (parts.length > 1) {
                        try {
                            durationSeconds = Double.parseDouble(parts[1].trim());
                        } catch (Exception e) {
                            Constants.LOG.warn("Invalid durationSeconds for graceEffect: \"{}\"", effectString);
                        }
                    }

                    int amplifier = 0;
                    if (parts.length > 2) {
                        try {
                            amplifier = Integer.parseInt(parts[2].trim());
                        } catch (NumberFormatException e) {
                            Constants.LOG.warn("Invalid amplifier for graceEffect: \"{}\"", effectString);
                        }
                    }

                    return new GraceEffect(effectId, (int) (durationSeconds * 20), amplifier);
                })
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
    }
}
