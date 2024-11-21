package net.blay09.mods.balm.fabric.client;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.client.DisconnectedFromServerEvent;
import net.blay09.mods.balm.fabric.network.FabricBalmNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public class FabricBalmClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> FabricBalmNetworking.initializeClientHandlers());

        Balm.getEvents().onEvent(DisconnectedFromServerEvent.class, event -> Balm.getConfig().resetToBackingConfigs());

        // TODO ModelLoadingPlugin.register((FabricBalmModels) BalmClient.getModels());
    }
}
