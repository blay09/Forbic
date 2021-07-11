package net.blay09.mods.balm.client;

import net.blay09.mods.balm.network.BalmNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public class BalmClient implements ClientModInitializer {
    public static Player getClientPlayer() {
        return Minecraft.getInstance().player;
    }

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            BalmNetworking.initializeClientHandlers();
        });
    }
}