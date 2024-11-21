package net.blay09.mods.balm.mixin;

import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ModelBakery.class)
public interface ModelBakeryAccessor {
    @Accessor
    UnbakedModel getMissingModel();

    @Accessor
    Map<ResourceLocation, UnbakedModel> getUnbakedPlainModels();
}
