package dev.gimme.netherreset;

import dev.gimme.netherreset.application.PlayerAttachmentAccessor;
import dev.gimme.netherreset.application.PlayerHandler;
import dev.gimme.netherreset.domain.inventory.InventoryManager;
import dev.gimme.netherreset.domain.config.ServerConfig;
import dev.gimme.netherreset.domain.util.Constants;
import dev.gimme.netherreset.infrastructure.NightServerConfig;

import java.nio.file.Path;

public class Main {

    public static Main INSTANCE;

    public static Main init(Path configDir, PlayerAttachmentAccessor playerAttachmentAccessor) {
        INSTANCE = new Main(configDir, playerAttachmentAccessor);
        return INSTANCE;
    }

    private final ServerConfig serverConfig;
    private final PlayerHandler playerHandler;

    private Main(Path configDir, PlayerAttachmentAccessor playerAttachmentAccessor) {
        NightServerConfig.SPEC.init(configDir, Constants.MOD_ID + "-server.toml");
        this.serverConfig = new NightServerConfig();
        this.playerHandler = new PlayerHandler(new InventoryManager(playerAttachmentAccessor, serverConfig));
    }

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public PlayerHandler getPlayerHandler() {
        return playerHandler;
    }
}
