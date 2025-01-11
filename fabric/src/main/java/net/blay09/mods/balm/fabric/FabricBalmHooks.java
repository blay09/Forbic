package net.blay09.mods.balm.fabric;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.BalmHooks;
import net.blay09.mods.balm.api.entity.BalmEntity;
import net.blay09.mods.balm.api.entity.BalmPlayer;
import net.blay09.mods.balm.api.event.server.ServerStartedEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;
import net.blay09.mods.balm.api.fluid.FluidTank;
import net.fabricmc.fabric.api.registry.FuelRegistryEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

public class FabricBalmHooks implements BalmHooks {

    private final AtomicReference<MinecraftServer> currentServer = new AtomicReference<>();

    public void initialize() {
        Balm.getEvents().onEvent(ServerStartedEvent.class, event -> currentServer.set(event.getServer()));
        Balm.getEvents().onEvent(ServerStoppedEvent.class, event -> currentServer.set(null));
    }

    @Override
    public boolean blockGrowFeature(Level level, RandomSource random, BlockPos pos, @Nullable Holder<ConfiguredFeature<?, ?>> holder) {
        return true;
    }

    @Override
    public boolean growCrop(ItemStack itemStack, Level level, BlockPos pos, @Nullable Player player) {
        return BoneMealItem.growCrop(itemStack, level, pos);
    }

    @Override
    public CompoundTag getPersistentData(Entity entity) {
        var balmData = ((BalmEntity) entity).getFabricBalmData();
        if (balmData.isEmpty()) {
            // If we have no data, try to import from NeoForge in case the world was migrated
            balmData = ((BalmEntity) entity).getNeoForgeBalmData();
            if (!balmData.isEmpty()) {
                ((BalmEntity) entity).setFabricBalmData(balmData);
            }
        }
        if (balmData.isEmpty()) {
            // If we still have no data, try to import from Forge in case the world was migrated
            balmData = ((BalmEntity) entity).getForgeBalmData();
            if (!balmData.isEmpty()) {
                ((BalmEntity) entity).setFabricBalmData(balmData);
            }
        }
        return balmData;
    }

    @Override
    public void curePotionEffects(LivingEntity entity, ItemStack curativeItem) {
        entity.removeAllEffects();
    }

    @Override
    public boolean isFakePlayer(Player player) {
        return false;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack itemStack) {
        return itemStack.getRecipeRemainder();
    }

    @Override
    public DyeColor getColor(ItemStack itemStack) {
        if (itemStack.getItem() instanceof DyeItem) {
            return ((DyeItem) itemStack.getItem()).getDyeColor();
        }

        return null;
    }

    @Override
    public boolean canItemsStack(ItemStack first, ItemStack second) {
        return !first.isEmpty() && ItemStack.isSameItemSameComponents(first, second);
    }

    @Override
    public int getBurnTime(Level level, ItemStack itemStack) {
        return level.fuelValues().burnDuration(itemStack);
    }

    @Override
    public void setBurnTime(Item item, int burnTime) {
        FuelRegistryEvents.BUILD.register((builder, context) -> builder.add(item, burnTime));
    }

    @Override
    public void firePlayerCraftingEvent(Player player, ItemStack crafted, Container craftMatrix) {
    }

    @Override
    public boolean useFluidTank(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        FluidTank fluidTank = Balm.getProviders().getProvider(blockEntity, FluidTank.class);
        if (fluidTank != null) {
            ItemStack handItem = player.getItemInHand(hand);
            if (handItem.getItem() == Items.BUCKET) {
                int drained = fluidTank.drain(fluidTank.getFluid(), 1000, true);
                if (drained >= 1000) {
                    Item bucketItem = fluidTank.getFluid().getBucket();
                    if (bucketItem != null && bucketItem != Items.AIR) {
                        ItemStack bucketItemStack = new ItemStack(bucketItem);
                        if (handItem.getCount() > 1) {
                            if (player.addItem(bucketItemStack)) {
                                fluidTank.getFluid().getPickupSound().ifPresent(sound -> player.playSound(sound, 1f, 1f));
                                handItem.shrink(1);
                                fluidTank.drain(fluidTank.getFluid(), 1000, false);
                                return true;
                            }
                        } else {
                            fluidTank.getFluid().getPickupSound().ifPresent(sound -> player.playSound(sound, 1f, 1f));
                            player.setItemInHand(hand, bucketItemStack);
                            fluidTank.drain(fluidTank.getFluid(), 1000, false);
                            return true;
                        }
                    }
                }
            } else {
                Fluid fluid = BuiltInRegistries.FLUID.stream().filter(it -> it.getBucket() == handItem.getItem()).findFirst().orElse(null);
                if (fluid != null && !fluid.isSame(Fluids.EMPTY)) {
                    int filled = fluidTank.fill(fluid, 1000, true);
                    if (filled >= 1000) {
                        if (handItem.getCount() > 1) {
                            ItemStack restItem = Balm.getHooks().getCraftingRemainingItem(handItem);
                            if (player.addItem(restItem)) {
                                player.playSound(SoundEvents.BUCKET_EMPTY, 1f, 1f);
                                fluidTank.getFluid().getPickupSound().ifPresent(sound -> player.playSound(sound, 1f, 1f));
                                handItem.shrink(1);
                                fluidTank.fill(fluid, 1000, false);
                                return true;
                            }
                        } else {
                            player.playSound(SoundEvents.BUCKET_EMPTY, 1f, 1f);
                            player.setItemInHand(hand, Balm.getHooks().getCraftingRemainingItem(handItem));
                            fluidTank.fill(fluid, 1000, false);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean isShield(ItemStack itemStack) {
        return itemStack.getItem() instanceof ShieldItem;
    }

    @Override
    public boolean isRepairable(ItemStack itemStack) {
        final var repairCost = itemStack.getItem().components().get(DataComponents.REPAIR_COST);
        return repairCost != null && repairCost > 0;
    }

    @Override
    public void setForcedPose(Player player, Pose pose) {
        ((BalmPlayer) player).setForcedPose(pose);
    }

    @Override
    public MinecraftServer getServer() {
        return currentServer.get();
    }

    @Override
    public double getBlockReachDistance(Player player) {
        return 4.5 + (player.isCreative() ? 0.5 : 0);
    }
}
