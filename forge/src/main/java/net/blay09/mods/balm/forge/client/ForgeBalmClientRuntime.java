package net.blay09.mods.balm.forge.client;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.client.BalmClientRuntime;
import net.blay09.mods.balm.api.client.keymappings.BalmKeyMappings;
import net.blay09.mods.balm.api.client.rendering.BalmModels;
import net.blay09.mods.balm.api.client.rendering.BalmRenderers;
import net.blay09.mods.balm.api.client.rendering.BalmTextures;
import net.blay09.mods.balm.api.client.screen.BalmScreens;
import net.blay09.mods.balm.forge.ForgeLoadContext;
import net.blay09.mods.balm.forge.event.ForgeBalmEvents;
import net.blay09.mods.balm.forge.client.keymappings.ForgeBalmKeyMappings;
import net.blay09.mods.balm.forge.client.rendering.ForgeBalmModels;
import net.blay09.mods.balm.forge.client.rendering.ForgeBalmRenderers;
import net.blay09.mods.balm.forge.client.rendering.ForgeBalmTextures;
import net.blay09.mods.balm.forge.client.screen.ForgeBalmScreens;
import net.blay09.mods.balm.forge.event.ForgeBalmClientEvents;

public class ForgeBalmClientRuntime implements BalmClientRuntime<ForgeLoadContext> {

    private final BalmRenderers renderers = new ForgeBalmRenderers();
    private final BalmTextures textures = new ForgeBalmTextures();
    private final BalmScreens screens = new ForgeBalmScreens();
    private final BalmKeyMappings keyMappings = new ForgeBalmKeyMappings();
    private final BalmModels models = new ForgeBalmModels();

    public ForgeBalmClientRuntime() {
        ForgeBalmClientEvents.registerEvents(((ForgeBalmEvents) Balm.getEvents()));
    }

    @Override
    public BalmRenderers getRenderers() {
        return renderers;
    }

    @Override
    public BalmTextures getTextures() {
        return textures;
    }

    @Override
    public BalmScreens getScreens() {
        return screens;
    }

    @Override
    public BalmModels getModels() {
        return models;
    }

    @Override
    public BalmKeyMappings getKeyMappings() {
        return keyMappings;
    }

    @Override
    public void initialize(String modId, ForgeLoadContext context, Runnable initializer) {
        ((ForgeBalmRenderers) renderers).register();
        ((ForgeBalmScreens) screens).register();
        ((ForgeBalmModels) models).register(modId, context.modBus());
        ((ForgeBalmKeyMappings) keyMappings).register();

        initializer.run();
    }
}
