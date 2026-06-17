package dev.gimme.netherreset.fabric;

import dev.gimme.netherreset.Main;
import dev.gimme.netherreset.domain.loot.ModLootConfig;
import dev.gimme.netherreset.domain.util.Constants;
import dev.gimme.netherreset.infrastructure.FcapServerConfig;
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.neoforged.fml.config.ModConfig;

public class FabricMod implements ModInitializer {

    @Override
    public void onInitialize() {
        ConfigRegistry.INSTANCE.register(Constants.MOD_ID, ModConfig.Type.COMMON, FcapServerConfig.SPEC, FcapServerConfig.FILE_NAME);

        Main.init(new FabricAttachmentAccessor(FabricAttachments.DIM_INV));

        // Register events
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> {
            Main.INSTANCE.getPlayerHandler().onPlayerChangeWorld(player, origin.dimension(), destination.dimension());
        });

        // Modify loot tables
        LootTableEvents.MODIFY.register((resourceKey, builder, lootTableSource, provider) -> {
            if (Main.INSTANCE.getServerConfig().isExtraLootEnabled()) {
                ModLootConfig.EXTRA_LOOT_POOLS.stream()
                    .filter(extraPool -> extraPool.tablesToModify().contains(resourceKey))
                    .forEach(extraPool -> builder.withPool(extraPool.content()));
            }
        });
    }
}
