package dev.gimme.netherreset.domain.config;

public abstract class ServerConfig {

    public abstract boolean preventItemsFromTeleporting();
    public abstract boolean preventEntitiesFromTeleporting();
    public abstract boolean allowTeleportToNether();
    public abstract boolean allowTeleportFromNether();
}
