package dev.gimme.netherreset.neoforge;

import dev.gimme.netherreset.Main;
import dev.gimme.netherreset.domain.util.Constants;
import dev.gimme.netherreset.infrastructure.FcapServerConfig;
import dev.gimme.netherreset.neoforge.loot.LootProviders;
import dev.gimme.netherreset.neoforge.loot.ModLootConditionTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.EnderChestBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@Mod(Constants.MOD_ID)
public class NeoForgeMod {

    public NeoForgeMod(ModContainer container, IEventBus modBus) {
        container.registerConfig(ModConfig.Type.COMMON, FcapServerConfig.SPEC, FcapServerConfig.FILE_NAME);

        Main.init(new NeoForgeAttachmentAccessor());
        NeoForge.EVENT_BUS.register(this);
        ModLootConditionTypes.REGISTRY.register(modBus);
        modBus.register(new LootProviders());
        NeoForgeAttachments.register(modBus);
    }

    @SubscribeEvent
    public void onChangeWorld(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Main.INSTANCE.getPlayerHandler().onPlayerChangeWorld(player, event.getFrom(), event.getTo());
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Main.INSTANCE.getPlayerHandler().onPlayerDeath(player);
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        BlockPos pos = event.getPos();
        if (!(event.getLevel().getBlockState(pos).getBlock() instanceof EnderChestBlock)) return;
        boolean handled = Main.INSTANCE.getPlayerHandler().onUseEnderChest(player, event.getItemStack(), pos);
        if (handled) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Main.INSTANCE.getPlayerHandler().onPlayerRespawn(player, event.isEndConquered());
    }
}
