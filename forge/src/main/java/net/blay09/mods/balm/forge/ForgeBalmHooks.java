package net.blay09.mods.balm.forge;

import net.blay09.mods.balm.api.BalmHooks;
import net.blay09.mods.balm.api.entity.BalmEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ForgeBalmHooks implements BalmHooks {

    public final Map<Item, Integer> burnTimes = new HashMap<>();

    public ForgeBalmHooks() {
        MinecraftForge.EVENT_BUS.addListener(this::furnaceFuelBurnTime);
    }

    private void furnaceFuelBurnTime(FurnaceFuelBurnTimeEvent event) {
        final var found = burnTimes.get(event.getItemStack().getItem());
        if (found != null) {
            event.setBurnTime(found);
        }
    }

    @Override
    public boolean blockGrowFeature(Level level, RandomSource random, BlockPos pos, @Nullable Holder<ConfiguredFeature<?, ?>> holder) {
        return !ForgeEventFactory.blockGrowFeature(level, random, pos, holder).getResult().equals(Event.Result.DENY);
    }

    @Override
    public boolean growCrop(ItemStack itemStack, Level level, BlockPos pos, Player player) {
        if (player != null) {
            return BoneMealItem.applyBonemeal(itemStack, level, pos, player);
        } else {
            return BoneMealItem.growCrop(itemStack, level, pos);
        }
    }

    @Override
    public CompoundTag getPersistentData(Entity entity) {
        CompoundTag persistentData = entity.getPersistentData();
        if (entity instanceof ServerPlayer) {
            CompoundTag persistedTag = persistentData.getCompound(ServerPlayer.PERSISTED_NBT_TAG);
            persistentData.put(ServerPlayer.PERSISTED_NBT_TAG, persistedTag);
            persistentData = persistedTag;
        }

        CompoundTag balmData = persistentData.getCompound("BalmData");
        if (balmData.isEmpty()) {
            // If we have no data, try to import from Fabric in case the world was migrated
            balmData = ((BalmEntity) entity).getFabricBalmData();
        }
        if (!balmData.isEmpty()) {
            persistentData.put("BalmData", balmData);
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
        return itemStack.getCraftingRemainder();
    }

    @Override
    public DyeColor getColor(ItemStack itemStack) {
        return DyeColor.getColor(itemStack);
    }

    @Override
    public boolean canItemsStack(ItemStack first, ItemStack second) {
        return ItemHandlerHelper.canItemStacksStack(first, second);
    }

    @Override
    public int getBurnTime(Level level, ItemStack itemStack) {
        return level.fuelValues().burnDuration(itemStack);
    }

    @Override
    public void setBurnTime(Item item, int burnTime) {
        burnTimes.put(item, burnTime);
    }

    @Override
    public void firePlayerCraftingEvent(Player player, ItemStack crafted, Container craftMatrix) {
        ForgeEventFactory.firePlayerCraftingEvent(player, crafted, craftMatrix);
    }

    @Override
    public boolean useFluidTank(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        return FluidUtil.interactWithFluidHandler(player, hand, level, pos, hitResult.getDirection());
    }

    @Override
    public boolean isShield(ItemStack itemStack) {
        return itemStack.getItem().canPerformAction(itemStack, ToolActions.SHIELD_BLOCK);
    }

    @Override
    public boolean isRepairable(ItemStack itemStack) {
        final var repairCost = itemStack.getItem().components().get(DataComponents.REPAIR_COST);
        return repairCost != null && repairCost > 0;
    }

    @Override
    public void setForcedPose(Player player, Pose pose) {
        player.setForcedPose(pose);
    }

    @Override
    public MinecraftServer getServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    @Override
    public double getBlockReachDistance(Player player) {
        return 4.5 + (player.isCreative() ? 0.5 : 0);
    }
}
