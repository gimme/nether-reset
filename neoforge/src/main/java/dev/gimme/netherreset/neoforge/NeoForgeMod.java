package dev.gimme.netherreset.neoforge;

import dev.gimme.netherreset.Main;
import dev.gimme.netherreset.domain.util.Constants;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(Constants.MOD_ID)
public class NeoForgeMod {

    public NeoForgeMod(IEventBus modBus) {
        NeoForge.EVENT_BUS.register(this);
        NeoForgeAttachments.register(modBus);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        Main.init(FMLPaths.CONFIGDIR.get(), new NeoForgeAttachmentAccessor());
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
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Main.INSTANCE.getPlayerHandler().onPlayerRespawn(player, event.isEndConquered());
    }
}
