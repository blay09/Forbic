package net.blay09.mods.balm.forge.client.rendering;

import net.blay09.mods.balm.api.DeferredObject;
import net.blay09.mods.balm.api.client.rendering.BalmModels;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ForgeBalmModels implements BalmModels {

    private static class Registrations {
        public final List<ResourceLocation> additionalModels = new ArrayList<>();
        public Map<ResourceLocation, BakedModel> bakedStandaloneModels = new HashMap<>();

        @SubscribeEvent
        public void onRegisterAdditionalModels(ModelEvent.RegisterModelStateDefinitions event) {
            additionalModels.forEach(it ->
                    event.register(it, new StateDefinition.Builder<Block, BlockState>(Blocks.AIR).create(Block::defaultBlockState, BlockState::new)));
        }

        @SubscribeEvent
        public void onBakingCompleted(ModelEvent.BakingCompleted event) {
            final var modelManager = event.getModelManager();
            bakedStandaloneModels = additionalModels.stream()
                    .collect(Collectors.toMap(it -> it, it -> modelManager.getModel(new ModelResourceLocation(it, ""))));
        }
    }

    private final Map<String, Registrations> registrations = new ConcurrentHashMap<>();

    @Override
    public DeferredObject<BakedModel> loadModel(ResourceLocation identifier) {
        final var deferredModel = new DeferredObject<BakedModel>(identifier) {
            @Override
            public BakedModel resolve() {
                return getRegistrations(identifier.getNamespace()).bakedStandaloneModels.get(identifier);
            }

            @Override
            public boolean canResolve() {
                return getRegistrations(identifier.getNamespace()).bakedStandaloneModels.containsKey(identifier);
            }
        };
        getRegistrations(identifier.getNamespace()).additionalModels.add(identifier);
        return deferredModel;
    }

    public void register(String modId, IEventBus eventBus) {
        eventBus.register(getRegistrations(modId));
    }

    private Registrations getRegistrations(String modId) {
        return registrations.computeIfAbsent(modId, it -> new Registrations());
    }
}
