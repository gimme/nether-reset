package dev.gimme.netherreset.fabric;

import dev.gimme.netherreset.Main;
import dev.gimme.netherreset.domain.loot.ModLootConfig;
import dev.gimme.netherreset.domain.util.Constants;
import dev.gimme.netherreset.infrastructure.FcapServerConfig;
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.EnderChestBlock;
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

        // Pump the deferred-task scheduler once per server tick.
        ServerTickEvents.END_SERVER_TICK.register(server -> Main.INSTANCE.getScheduler().tick());

        // Recovery ritual: right-clicking an Ender Chest with the key item spits out the player's Nether stash.
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (level.isClientSide() || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            BlockPos pos = hitResult.getBlockPos();
            if (!(level.getBlockState(pos).getBlock() instanceof EnderChestBlock)) return InteractionResult.PASS;
            boolean handled = Main.INSTANCE.getPlayerHandler()
                    .onUseEnderChest(serverPlayer, player.getItemInHand(hand), pos);
            return handled ? InteractionResult.SUCCESS : InteractionResult.PASS;
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
