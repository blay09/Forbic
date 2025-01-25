package net.blay09.mods.balm.api.event.server;

import net.blay09.mods.balm.api.event.BalmEvent;
import net.minecraft.server.MinecraftServer;

/** Fired before any levels are loaded */
public class ServerBeforeStartingEvent extends BalmEvent {
    private final MinecraftServer server;

    public ServerBeforeStartingEvent(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer getServer() {
        return server;
    }
}
