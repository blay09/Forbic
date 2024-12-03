package net.blay09.mods.balm.neoforge.client.rendering;

import net.blay09.mods.balm.api.DeferredObject;
import net.blay09.mods.balm.api.client.rendering.BalmModels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NeoForgeBalmModels implements BalmModels {

    private static class Registrations {
        public final List<ResourceLocation> additionalModels = new ArrayList<>();

        @SubscribeEvent
        public void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
            additionalModels.forEach(event::register);
        }
    }

    private final Map<String, Registrations> registrations = new ConcurrentHashMap<>();

    @Override
    public DeferredObject<BakedModel> loadModel(ResourceLocation identifier) {
        final var modelResourceLocation = new ModelResourceLocation(identifier, "standalone");
        final var deferredModel = new DeferredObject<BakedModel>(identifier) {
            @Override
            public BakedModel resolve() {
                return Minecraft.getInstance().getModelManager().getModel(modelResourceLocation);
            }

            @Override
            public boolean canResolve() {
                final var modelManager = Minecraft.getInstance().getModelManager();
                final var foundModel = modelManager.getModel(modelResourceLocation);
                return foundModel != modelManager.getMissingModel();
            }
        };
        getActiveRegistrations().additionalModels.add(identifier);
        return deferredModel;
    }

    public void register(String modId, IEventBus eventBus) {
        eventBus.register(getRegistrations(modId));
    }

    private Registrations getActiveRegistrations() {
        return getRegistrations(ModLoadingContext.get().getActiveNamespace());
    }

    private Registrations getRegistrations(String modId) {
        return registrations.computeIfAbsent(modId, it -> new Registrations());
    }

}
