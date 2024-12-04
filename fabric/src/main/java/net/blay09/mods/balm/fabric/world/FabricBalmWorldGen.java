package net.blay09.mods.balm.fabric.world;

import net.blay09.mods.balm.api.DeferredObject;
import net.blay09.mods.balm.api.world.BalmWorldGen;
import net.blay09.mods.balm.api.world.BiomePredicate;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import java.util.function.Supplier;

public class FabricBalmWorldGen implements BalmWorldGen {
    @Override
    public <T extends Feature<?>> DeferredObject<T> registerFeature(ResourceLocation identifier, Supplier<T> supplier) {
        return new DeferredObject<>(identifier, () -> {
            T feature = supplier.get();
            Registry.register(BuiltInRegistries.FEATURE, identifier, feature);
            return feature;
        }).resolveImmediately();
    }

    @Override
    public <T extends PlacementModifierType<?>> DeferredObject<T> registerPlacementModifier(ResourceLocation identifier, Supplier<T> supplier) {
        return new DeferredObject<>(identifier, () -> {
            T placementModifierType = supplier.get();
            Registry.register(BuiltInRegistries.PLACEMENT_MODIFIER_TYPE, identifier, placementModifierType);
            return placementModifierType;
        }).resolveImmediately();
    }

    @Override
    public <T extends PoiType> DeferredObject<T> registerPoiType(ResourceLocation identifier, Supplier<T> supplier) {
        return new DeferredObject<>(identifier, () -> {
            T poiType = supplier.get();
            Registry.register(BuiltInRegistries.POINT_OF_INTEREST_TYPE, identifier, poiType);
            return poiType;
        }).resolveImmediately();
    }

    @Override
    public void addFeatureToBiomes(BiomePredicate biomePredicate, GenerationStep.Decoration step, ResourceLocation placedFeatureIdentifier) {
        BiomeModifications.addFeature(it -> biomePredicate.test(it.getBiomeKey().location(), it.getBiomeRegistryEntry()),
                step, ResourceKey.create(Registries.PLACED_FEATURE, placedFeatureIdentifier));
    }
}
