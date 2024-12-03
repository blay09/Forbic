package net.blay09.mods.balm.neoforge.client.rendering;

import com.mojang.datafixers.util.Pair;
import net.blay09.mods.balm.api.client.rendering.BalmRenderers;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class NeoForgeBalmRenderers implements BalmRenderers {

    private static class ColorRegistration<THandler, TObject> {
        private final THandler color;
        private final Supplier<TObject[]> objects;

        public ColorRegistration(THandler color, Supplier<TObject[]> objects) {
            this.color = color;
            this.objects = objects;
        }

        public THandler getColor() {
            return color;
        }

        public Supplier<TObject[]> getObjects() {
            return objects;
        }
    }

    private static class Registrations {
        public final Map<ModelLayerLocation, Supplier<LayerDefinition>> layerDefinitions = new HashMap<>();
        public final List<Pair<Supplier<BlockEntityType<?>>, BlockEntityRendererProvider<BlockEntity>>> blockEntityRenderers = new ArrayList<>();
        public final List<Pair<Supplier<EntityType<?>>, EntityRendererProvider<Entity>>> entityRenderers = new ArrayList<>();
        public final List<ColorRegistration<BlockColor, Block>> blockColors = new ArrayList<>();

        @SubscribeEvent
        public void setupClient(FMLClientSetupEvent event) {
        }

        @SubscribeEvent
        public void initRenderers(EntityRenderersEvent.RegisterRenderers event) {
            for (Pair<Supplier<BlockEntityType<?>>, BlockEntityRendererProvider<BlockEntity>> entry : blockEntityRenderers) {
                event.registerBlockEntityRenderer(entry.getFirst().get(), entry.getSecond());
            }

            for (Pair<Supplier<EntityType<?>>, EntityRendererProvider<Entity>> entry : entityRenderers) {
                event.registerEntityRenderer(entry.getFirst().get(), entry.getSecond());
            }
        }

        @SubscribeEvent
        public void initLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
            for (Map.Entry<ModelLayerLocation, Supplier<LayerDefinition>> entry : layerDefinitions.entrySet()) {
                event.registerLayerDefinition(entry.getKey(), entry.getValue());
            }
        }

        @SubscribeEvent
        public void initBlockColors(RegisterColorHandlersEvent.Block event) {
            for (ColorRegistration<BlockColor, Block> blockColor : blockColors) {
                event.register(blockColor.getColor(), blockColor.getObjects().get());
            }
        }
    }

    private final Map<String, Registrations> registrations = new ConcurrentHashMap<>();

    @Override
    public ModelLayerLocation registerModel(ResourceLocation location, Supplier<LayerDefinition> layerDefinition) {
        ModelLayerLocation modelLayerLocation = new ModelLayerLocation(location, "main");
        getActiveRegistrations().layerDefinitions.put(modelLayerLocation, layerDefinition);
        return modelLayerLocation;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> void registerEntityRenderer(Supplier<EntityType<T>> type, EntityRendererProvider<? super T> provider) {
        getActiveRegistrations().entityRenderers.add(Pair.of(type::get, (EntityRendererProvider<Entity>) provider));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> void registerBlockEntityRenderer(Supplier<BlockEntityType<T>> type, BlockEntityRendererProvider<? super T> provider) {
        getActiveRegistrations().blockEntityRenderers.add(Pair.of(type::get, (BlockEntityRendererProvider<BlockEntity>) provider));
    }

    @Override
    public void registerBlockColorHandler(BlockColor color, Supplier<Block[]> blocks) {
        getActiveRegistrations().blockColors.add(new ColorRegistration<>(color, blocks));
    }

    @Override
    public void setBlockRenderType(Supplier<Block> block, RenderType renderType) {
        // Do nothing in Forge. Forge unfortunately changes the Vanilla model format,
        // so we have to have both this call (for Fabric) and change the JSON (for Forge).
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
