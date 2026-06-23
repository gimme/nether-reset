package dev.gimme.netherreset;

import dev.gimme.netherreset.application.PlayerAttachmentAccessor;
import dev.gimme.netherreset.application.PlayerHandler;
import dev.gimme.netherreset.application.VillagerAttachmentAccessor;
import dev.gimme.netherreset.application.VillagerHandler;
import dev.gimme.netherreset.domain.bargain.BargainManager;
import dev.gimme.netherreset.domain.inventory.InventoryManager;
import dev.gimme.netherreset.domain.config.ServerConfig;
import dev.gimme.netherreset.infrastructure.FcapServerConfig;

public class Main {

    public static Main INSTANCE;

    public static Main init(PlayerAttachmentAccessor playerAttachmentAccessor, VillagerAttachmentAccessor villagerAttachmentAccessor) {
        INSTANCE = new Main(playerAttachmentAccessor, villagerAttachmentAccessor);
        return INSTANCE;
    }

    private final ServerConfig serverConfig;
    private final PlayerHandler playerHandler;
    private final VillagerHandler villagerHandler;

    private Main(PlayerAttachmentAccessor playerAttachmentAccessor, VillagerAttachmentAccessor villagerAttachmentAccessor) {
        this.serverConfig = new FcapServerConfig();
        this.playerHandler = new PlayerHandler(new InventoryManager(playerAttachmentAccessor, serverConfig));
        this.villagerHandler = new VillagerHandler(new BargainManager(villagerAttachmentAccessor, serverConfig));
    }

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public PlayerHandler getPlayerHandler() {
        return playerHandler;
    }

    public VillagerHandler getVillagerHandler() {
        return villagerHandler;
    }
}
