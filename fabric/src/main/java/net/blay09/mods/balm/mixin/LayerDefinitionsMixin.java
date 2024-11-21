package net.blay09.mods.balm.mixin;

import com.google.common.collect.ImmutableMap;
import net.blay09.mods.balm.api.client.BalmClient;
import net.blay09.mods.balm.fabric.client.rendering.FabricBalmRenderers;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.LayerDefinitions;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(LayerDefinitions.class)
public class LayerDefinitionsMixin {

    @Inject(method = "createRoots", at = @At("RETURN"))
    private static void createRoots(CallbackInfoReturnable<Map<ModelLayerLocation, LayerDefinition>> callbackInfo) {
        final var originalRoots = callbackInfo.getReturnValue();
        final var moddedRoots = ImmutableMap.<ModelLayerLocation, LayerDefinition>builder()
                .putAll(originalRoots)
                .putAll(((FabricBalmRenderers) BalmClient.getRenderers()).createRoots())
                .build();
        callbackInfo.setReturnValue(moddedRoots);
    }

}
