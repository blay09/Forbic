package net.blay09.mods.balm.fabric.client.rendering;

import net.blay09.mods.balm.api.client.rendering.BalmRenderers;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class FabricBalmRenderers implements BalmRenderers {
    private final Map<ModelLayerLocation, Supplier<LayerDefinition>> layerDefinitions = new HashMap<>();

    @Override
    public ModelLayerLocation registerModel(ResourceLocation location, Supplier<LayerDefinition> layerDefinition) {
        ModelLayerLocation modelLayerLocation = new ModelLayerLocation(location, "main");
        layerDefinitions.put(modelLayerLocation, layerDefinition);
        return modelLayerLocation;
    }

    public Map<ModelLayerLocation, LayerDefinition> createRoots() {
        return layerDefinitions.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, it -> it.getValue().get()));
    }

    @Override
    public <T extends Entity> void registerEntityRenderer(Supplier<EntityType<T>> type, EntityRendererProvider<? super T> provider) {
        EntityRendererRegistry.register(type.get(), provider);
    }

    @Override
    public <T extends BlockEntity> void registerBlockEntityRenderer(Supplier<BlockEntityType<T>> type, BlockEntityRendererProvider<? super T> provider) {
        BlockEntityRendererRegistry.register(type.get(), provider);
    }

    @Override
    public void registerBlockColorHandler(BlockColor color, Supplier<Block[]> blocks) {
        ColorProviderRegistry.BLOCK.register(color, blocks.get());
    }

    @Override
    public void setBlockRenderType(Supplier<Block> block, RenderType renderType) {
        BlockRenderLayerMap.INSTANCE.putBlock(block.get(), renderType);
    }
}
