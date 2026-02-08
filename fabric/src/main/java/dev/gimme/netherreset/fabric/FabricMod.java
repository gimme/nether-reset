package dev.gimme.netherreset.fabric;

import dev.gimme.netherreset.Main;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

public class FabricMod implements ModInitializer {

    @Override
    public void onInitialize() {
        // Register server starting event
        ServerLifecycleEvents.SERVER_STARTED.register(mainServer -> {
            Main.init(FabricLoader.getInstance().getConfigDir(), new FabricAttachmentAccessor(FabricAttachments.DIM_INV));
        });

        // Register events
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            Main.INSTANCE.getPlayerHandler().onPlayerChangeWorld(player, origin.dimension(), destination.dimension());
        });
    }
}
