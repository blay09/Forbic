package net.blay09.mods.balm.fabric.client.rendering;

import net.blay09.mods.balm.api.DeferredObject;
import net.blay09.mods.balm.api.client.rendering.BalmModels;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class FabricBalmModels implements BalmModels/*, TODO ModelLoadingPlugin*/ {

    private final List<ResourceLocation> additionalModels = Collections.synchronizedList(new ArrayList<>());

    /*TODO@Override
    public void initialize(Context context) {
        context.addModels(additionalModels);
    }*/

    @Override
    public DeferredObject<BakedModel> loadModel(final ResourceLocation identifier) {
        // fabric_resource is what Fabric uses as variant for additional models
        final var modelResourceLocation = new ModelResourceLocation(identifier, "fabric_resource");
        final var deferredObject = new DeferredObject<BakedModel>(identifier) {
            @Override
            public BakedModel resolve() {
                // TODO return ((ModelBakeryAccessor) modelBakery).getBakedCache().get(modelResourceLocation);
                return null;
            }

            @Override
            public boolean canResolve() {
                // TODO return ((ModelBakeryAccessor) modelBakery).getBakedCache().containsKey(modelResourceLocation);
                return false;
            }
        };
        additionalModels.add(identifier);
        return deferredObject;
    }

}
