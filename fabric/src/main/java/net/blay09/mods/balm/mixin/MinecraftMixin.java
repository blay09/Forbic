package net.blay09.mods.balm.mixin;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.LevelLoadingEvent;
import net.blay09.mods.balm.api.event.client.OpenScreenEvent;
import net.blay09.mods.balm.api.event.client.UseItemInputEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.HitResult;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Shadow
    public HitResult hitResult;
    @Shadow
    public ClientLevel level;

    @Unique
    private static final ThreadLocal<UseItemInputEvent> balmCurrentUseEvent = new ThreadLocal<>();

    @ModifyVariable(method = "setScreen(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;", opcode = Opcodes.GETFIELD, shift = At.Shift.AFTER), argsOnly = true)
    public Screen modifyScreen(Screen screen) {
        OpenScreenEvent event = new OpenScreenEvent(screen);
        Balm.getEvents().fireEvent(event);
        return event.getNewScreen() != null ? event.getNewScreen() : screen;
    }

    @Inject(method = "startUseItem()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;", shift = At.Shift.AFTER), cancellable = true)
    public void startUseItem(CallbackInfo callbackInfo) {
        final var event = balmCurrentUseEvent.get();
        if (event != null && event.isCanceled()) {
            callbackInfo.cancel();
        }
        balmCurrentUseEvent.remove();
    }

    @ModifyArg(method = "startUseItem()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;"), index = 0)
    public InteractionHand modifyHand(InteractionHand hand) {
        if (this.hitResult != null && this.hitResult.getType() != HitResult.Type.MISS) {
            UseItemInputEvent event = new UseItemInputEvent(hand);
            Balm.getEvents().fireEvent(event);
            balmCurrentUseEvent.set(event);
        } else {
            balmCurrentUseEvent.remove();
        }
        return hand;
    }

    @Inject(method = "clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"))
    public void clearLevel(Screen p_91321_, CallbackInfo ci) {
        if (this.level != null) {
            Balm.getEvents().fireEvent(new LevelLoadingEvent.Unload(this.level));
        }
    }

    @Inject(method = "setLevel(Lnet/minecraft/client/multiplayer/ClientLevel;)V", at = @At("HEAD"))
    public void onSetLevel(ClientLevel clientLevel, CallbackInfo ci) {
        if (this.level != null) {
            Balm.getEvents().fireEvent(new LevelLoadingEvent.Unload(this.level));
        }
    }



}
