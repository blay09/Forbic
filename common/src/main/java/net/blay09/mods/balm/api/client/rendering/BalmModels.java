package net.blay09.mods.balm.api.client.rendering;

import net.blay09.mods.balm.api.DeferredObject;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;

public interface BalmModels {
    DeferredObject<BakedModel> loadModel(ResourceLocation identifier);
}
