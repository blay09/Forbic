package net.blay09.mods.balm.fabric.client;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.client.BalmClient;
import net.blay09.mods.balm.api.event.client.ConnectedToServerEvent;
import net.blay09.mods.balm.api.event.client.DisconnectedFromServerEvent;
import net.blay09.mods.balm.api.network.ServerboundModListMessage;
import net.blay09.mods.balm.fabric.client.rendering.FabricBalmModels;
import net.blay09.mods.balm.fabric.network.FabricBalmNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.loader.api.FabricLoader;

import java.util.HashMap;

public class FabricBalmClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> FabricBalmNetworking.initializeClientHandlers());

        Balm.getEvents().onEvent(DisconnectedFromServerEvent.class, event -> Balm.getConfig().resetToBackingConfigs());
        
        ModelLoadingPlugin.register((FabricBalmModels) BalmClient.getModels());

        Balm.getEvents().onEvent(ConnectedToServerEvent.class, event -> {
            final var networking = (FabricBalmNetworking) Balm.getNetworking();
            final var modVersions = new HashMap<String, String>();
            for (final var modId : networking.getRegisteredMods()) {
                FabricLoader.getInstance().getModContainer(modId).ifPresent(modContainer -> {
                    final var version = modContainer.getMetadata().getVersion().toString();
                    if (!networking.isClientOnly(modId) && !networking.isServerOnly(modId)) {
                        modVersions.put(modId, version);
                    }
                });
            }
            Balm.getNetworking().sendToServer(new ServerboundModListMessage(modVersions));
        });
    }
}
