package net.blay09.mods.balm.fabric.client.rendering;

import net.blay09.mods.balm.api.DeferredObject;
import net.blay09.mods.balm.api.client.rendering.BalmModels;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class FabricBalmModels implements BalmModels, ModelLoadingPlugin {

    private final List<ResourceLocation> additionalModels = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void initialize(Context context) {
        context.addModels(additionalModels);
    }

    @Override
    public DeferredObject<BakedModel> loadModel(final ResourceLocation identifier) {
        final var deferredObject = new DeferredObject<BakedModel>(identifier) {
            @Override
            public BakedModel resolve() {
                return Minecraft.getInstance().getModelManager().getModel(identifier);
            }

            @Override
            public boolean canResolve() {
                final var modelManager = Minecraft.getInstance().getModelManager();
                final var foundModel = modelManager.getModel(identifier);
                return foundModel != modelManager.getMissingModel();
            }
        };
        additionalModels.add(identifier);
        return deferredObject;
    }

}
