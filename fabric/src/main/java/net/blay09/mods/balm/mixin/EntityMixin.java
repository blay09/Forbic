package net.blay09.mods.balm.mixin;

import net.blay09.mods.balm.api.entity.BalmEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin implements BalmEntity {

    private CompoundTag balmData = new CompoundTag();
    private CompoundTag forgeBalmData = new CompoundTag();
    private CompoundTag neoforgeBalmData = new CompoundTag();

    @Inject(method = "load(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("HEAD"))
    private void load(CompoundTag compound, CallbackInfo callbackInfo) {
        if (compound.contains("BalmData")) {
            balmData = compound.getCompound("BalmData");
            if (balmData.size() == 0) {
                CompoundTag forgeData = compound.getCompound("ForgeData");
                CompoundTag playerPersisted = forgeData.getCompound("PlayerPersisted");
                balmData = playerPersisted.getCompound("BalmData");
            }
        }
        if (compound.contains("ForgeData")) {
            forgeBalmData = compound.getCompound("ForgeData").getCompound("PlayerPersisted").getCompound("BalmData");
        }
        if (compound.contains("NeoForgeData")) {
            neoforgeBalmData = compound.getCompound("NeoForgeData").getCompound("PlayerPersisted").getCompound("BalmData");
        }
    }

    @Inject(method = "saveWithoutId(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;", at = @At("HEAD"))
    private void saveWithoutId(CompoundTag compound, CallbackInfoReturnable<CompoundTag> callbackInfo) {
        if (!balmData.isEmpty()) {
            compound.put("BalmData", balmData);
        }
    }

    @Override
    public CompoundTag getFabricBalmData() {
        return balmData;
    }

    @Override
    public void setFabricBalmData(CompoundTag tag) {
        this.balmData = tag;
    }

    @Override
    public CompoundTag getForgeBalmData() {
        return forgeBalmData;
    }

    @Override
    public void setForgeBalmData(CompoundTag tag) {
        this.forgeBalmData = tag;
    }

    @Override
    public CompoundTag getNeoForgeBalmData() {
        return neoforgeBalmData;
    }

    @Override
    public void setNeoForgeBalmData(CompoundTag tag) {
        this.neoforgeBalmData = tag;
    }
}
