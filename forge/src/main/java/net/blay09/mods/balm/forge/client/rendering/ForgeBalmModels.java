package net.blay09.mods.balm.forge.client.rendering;

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
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.SimpleModelState;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
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

public class ForgeBalmModels implements BalmModels {

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

        // TODO @SubscribeEvent
        // TODO public void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        // TODO     additionalModels.forEach(it -> event.register(it.getModelResourceLocation()));
        // TODO }

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
    private ModelBakery modelBakery;

    public void onBakeModels(ModelBakery modelBakery, ModelBakery.TextureGetter textureGetter) {
        this.modelBakery = modelBakery;
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

    @Override
    public DeferredObject<BakedModel> bakeModel(ModelResourceLocation identifier, UnbakedModel model) {
        DeferredModel deferredModel = new DeferredModel(identifier) {
            @Override
            public BakedModel resolve(ModelBakery bakery, Map<ModelResourceLocation, BakedModel> modelRegistry, ModelBakery.TextureGetter textureGetter) {
                ModelBaker baker = createBaker(identifier, textureGetter);
                return model.bake(baker, baker.getModelTextureGetter(), getModelState(Transformation.identity()));
            }
        };
        modelsToBake.add(deferredModel);
        return deferredModel;
    }

    @Override
    public DeferredObject<BakedModel> retexture(ModelResourceLocation identifier, Map<String, String> textureMap) {
        DeferredModel deferredModel = new DeferredModel(identifier) {
            @Override
            public BakedModel resolve(ModelBakery bakery, Map<ModelResourceLocation, BakedModel> modelRegistry, ModelBakery.TextureGetter textureGetter) {
                UnbakedModel model = retexture(bakery, identifier, textureMap);
                ModelBaker baker = createBaker(identifier, textureGetter);
                return model.bake(baker, baker.getModelTextureGetter(), getModelState(Transformation.identity()));
            }
        };
        modelsToBake.add(deferredModel);
        return deferredModel;
    }

    @Override
    public DeferredObject<BakedModel> loadDynamicModel(ModelResourceLocation identifier, Set<ModelResourceLocation> models, @Nullable Function<BlockState, ModelResourceLocation> modelFunction, @Nullable Function<BlockState, Map<String, String>> textureMapFunction, @Nullable BiConsumer<BlockState, Matrix4f> transformFunction, List<RenderType> renderTypes) {
        Function<BlockState, ModelResourceLocation> effectiveModelFunction = modelFunction != null ? modelFunction : (it -> identifier);

        DeferredModel deferredModel = new DeferredModel(identifier) {
            @Override
            public BakedModel resolve(ModelBakery bakery, Map<ModelResourceLocation, BakedModel> modelRegistry, ModelBakery.TextureGetter textureGetter) {
                final var unbakedModels = new HashMap<ModelResourceLocation, UnbakedModel>();
                for (final var modelId : models) {
                    unbakedModels.put(modelId, getUnbakedModelOrMissing(modelId.id()));
                }
                return new ForgeCachedDynamicModel(bakery,
                        unbakedModels,
                        effectiveModelFunction,
                        null,
                        textureMapFunction,
                        transformFunction,
                        renderTypes,
                        identifier,
                        textureGetter);
            }
        };
        modelsToBake.add(deferredModel);
        return deferredModel;
    }

    @Override
    public void overrideModel(Supplier<Block> block, Supplier<BakedModel> model) {
        getActiveRegistrations().overrides.add(Pair.of(block, model));
    }

    @Override
    public ModelState getModelState(Transformation transformation) {
        return new SimpleModelState(transformation);
    }

    @Override
    public UnbakedModel getUnbakedModelOrMissing(ResourceLocation location) {
        return ((ModelBakeryAccessor) modelBakery).getUnbakedModels().getOrDefault(location, ((ModelBakeryAccessor) modelBakery).getMissingModel());
    }

    @Override
    public UnbakedModel getUnbakedMissingModel() {
        return ((ModelBakeryAccessor) modelBakery).getMissingModel();
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

    @Override
    public ModelBaker createBaker(ModelResourceLocation location, ModelBakery.TextureGetter textureGetter) {
        try {
            Class<?> clazz = Class.forName("net.minecraft.client.resources.model.ModelBakery$ModelBakerImpl");
            Constructor<?> constructor = clazz.getDeclaredConstructor(ModelBakery.class, ModelBakery.TextureGetter.class, ModelResourceLocation.class);
            constructor.setAccessible(true);
            return (ModelBaker) constructor.newInstance(modelBakery, textureGetter, location);
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Balm failed to create model baker", e);
        }
    }
}