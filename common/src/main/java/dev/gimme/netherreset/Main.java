package dev.gimme.netherreset;

import dev.gimme.netherreset.application.EntityHandler;
import dev.gimme.netherreset.application.PlayerAttachmentAccessor;
import dev.gimme.netherreset.application.PlayerHandler;
import dev.gimme.netherreset.domain.inventory.InventoryManager;
import dev.gimme.netherreset.domain.config.ServerConfig;
import dev.gimme.netherreset.infrastructure.FcapServerConfig;

public class Main {

    public static Main INSTANCE;

    public static Main init(PlayerAttachmentAccessor playerAttachmentAccessor) {
        INSTANCE = new Main(playerAttachmentAccessor);
        return INSTANCE;
    }

    private final ServerConfig serverConfig;
    private final PlayerHandler playerHandler;
    private final EntityHandler entityHandler;

    private Main(PlayerAttachmentAccessor playerAttachmentAccessor) {
        this.serverConfig = new FcapServerConfig();
        this.playerHandler = new PlayerHandler(new InventoryManager(playerAttachmentAccessor, serverConfig));
        this.entityHandler = new EntityHandler(serverConfig);
    }

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public PlayerHandler getPlayerHandler() {
        return playerHandler;
    }

    public EntityHandler getEntityHandler() {
        return entityHandler;
    }
}
