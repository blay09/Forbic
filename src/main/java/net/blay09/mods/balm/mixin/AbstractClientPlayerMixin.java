package net.blay09.mods.balm.mixin;

import net.blay09.mods.balm.event.client.BalmClientEvents;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerMixin {

    @Inject(method = "getFieldOfViewModifier()F", at = @At("TAIL"), cancellable = true)
    private void getFieldOfViewModifier(CallbackInfoReturnable<Float> callbackInfo) {
        Float override = BalmClientEvents.FOV_UPDATE.invoker().handle((LivingEntity) (Object) this);
        if (override != null) {
            callbackInfo.setReturnValue(override);
        }
    }

}
