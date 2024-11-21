package net.blay09.mods.balm.neoforge.client.rendering;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.math.Transformation;
import net.blay09.mods.balm.api.DeferredObject;
import net.blay09.mods.balm.api.client.rendering.BalmModels;
import net.blay09.mods.balm.mixin.ModelBakeryAccessor;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.SimpleModelState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class NeoForgeBalmModels implements BalmModels {

    private static final Logger LOGGER = LogUtils.getLogger();

    private abstract static class DeferredModel extends DeferredObject<BakedModel> {
        private final ModelResourceLocation modelResourceLocation;

        public DeferredModel(ModelResourceLocation modelResourceLocation) {
            super(modelResourceLocation.id());
            this.modelResourceLocation = modelResourceLocation;
        }

        public void resolveAndSet(ModelBakery modelBakery, Map<ModelResourceLocation, BakedModel> modelRegistry, ModelBakery.TextureGetter textureGetter) {
            try {
                set(resolve(modelBakery, modelRegistry, textureGetter));
            } catch (Exception exception) {
                LOGGER.warn("Unable to bake model: '{}':", getIdentifier(), exception);
                set(modelBakery.getBakedTopLevelModels().get(MissingBlockModel.VARIANT));
            }
        }

        public abstract BakedModel resolve(ModelBakery modelBakery, Map<ModelResourceLocation, BakedModel> modelRegistry, ModelBakery.TextureGetter textureGetter);

        public ModelResourceLocation getModelResourceLocation() {
            return modelResourceLocation;
        }
    }

    public final List<DeferredModel> modelsToBake = Collections.synchronizedList(new ArrayList<>());

    private static class Registrations {
        public final List<DeferredModel> additionalModels = new ArrayList<>();
        public final List<Pair<Supplier<Block>, Supplier<BakedModel>>> overrides = new ArrayList<>();
        private ModelBakery.TextureGetter textureGetter;

        public void setTextureGetter(ModelBakery.TextureGetter textureGetter) {
            this.textureGetter = textureGetter;
        }

        @SubscribeEvent
        public void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
            additionalModels.forEach(it -> event.register(it.getModelResourceLocation()));
        }

        @SubscribeEvent
        public void onModelBakingCompleted(ModelEvent.ModifyBakingResult event) {
            for (Pair<Supplier<Block>, Supplier<BakedModel>> override : overrides) {
                Block block = override.getFirst().get();
                BakedModel bakedModel = override.getSecond().get();
                block.getStateDefinition().getPossibleStates().forEach(state -> {
                    ModelResourceLocation modelLocation = BlockModelShaper.stateToModelLocation(state);
                    event.getModels().put(modelLocation, bakedModel);
                });
            }
        }

        @SubscribeEvent
        public void onModelBakingCompleted(ModelEvent.BakingCompleted event) {
            for (DeferredModel deferredModel : additionalModels) {
                deferredModel.resolveAndSet(event.getModelBakery(), event.getModels(), textureGetter);
            }

            textureGetter = null;
        }
    }

    private final Map<String, Registrations> registrations = new ConcurrentHashMap<>();

    public void onBakeModels(ModelBakery modelBakery, ModelBakery.TextureGetter textureGetter) {
        registrations.values().forEach(it -> it.setTextureGetter(textureGetter));

        synchronized (modelsToBake) {
            for (DeferredModel deferredModel : modelsToBake) {
                deferredModel.resolveAndSet(modelBakery, modelBakery.getBakedTopLevelModels(), textureGetter);
            }
        }
    }

    @Override
    public DeferredObject<BakedModel> loadModel(ResourceLocation identifier) {
        DeferredModel deferredModel = new DeferredModel(new ModelResourceLocation(identifier, "standalone")) {
            @Override
            public BakedModel resolve(ModelBakery bakery, Map<ModelResourceLocation, BakedModel> modelRegistry, ModelBakery.TextureGetter textureGetter) {
                return modelRegistry.get(getModelResourceLocation());
            }
        };
        getActiveRegistrations().additionalModels.add(deferredModel);
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
