package net.blay09.mods.balm.neoforge.provider;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.blay09.mods.balm.api.provider.BalmProviders;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BaseCapability;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class NeoForgeBalmProviders implements BalmProviders {

    private final Multimap<Class<?>, BiFunction<BlockEntity, Direction, ?>> fallbackBlockCapabilities = ArrayListMultimap.create();
    private final Map<Class<?>, BaseCapability<?, ?>> blockCapabilities = new HashMap<>();
    private final Map<Class<?>, ItemCapability<?, ?>> itemCapabilities = new HashMap<>();
    private final Map<Class<?>, EntityCapability<?, ?>> entityCapabilities = new HashMap<>();

    @Override
    public <T> T getProvider(BlockEntity blockEntity, Class<T> clazz) {
        return getProvider(blockEntity, null, clazz);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProvider(BlockEntity blockEntity, @Nullable Direction direction, Class<T> clazz) {
        final var capability = (BlockCapability<T, Direction>) getBlockCapability(clazz);
        final var provider = blockEntity.getLevel().getCapability(capability, blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity, direction);
        if (provider != null) {
            return provider;
        }
        final var fallbacks = fallbackBlockCapabilities.get(clazz);
        for (final var fallback : fallbacks) {
            final var fallbackProvider = fallback.apply(blockEntity, direction);
            if (fallbackProvider != null) {
                return (T) fallbackProvider;
            }
        }
        return null;
    }

    @Override
    public <T> T getProvider(Entity entity, Class<T> clazz) {
        final var capability = getEntityCapability(clazz);
        return entity.getCapability(capability, null);
    }

    public <T> void registerBlockProvider(Class<T> clazz, BlockCapability<T, ?> capability) {
        blockCapabilities.put(clazz, capability);
    }

    public void registerFallbackBlockProvider(Class<?> clazz, BiFunction<BlockEntity, Direction, ?> fallback) {
        fallbackBlockCapabilities.put(clazz, fallback);
    }

    public <T> void registerItemProvider(Class<T> clazz, ItemCapability<T, ?> capability) {
        itemCapabilities.put(clazz, capability);
    }

    public <T> void registerEntityProvider(Class<T> clazz, EntityCapability<T, ?> capability) {
        entityCapabilities.put(clazz, capability);
    }

    @SuppressWarnings("unchecked")
    public <T> BlockCapability<T, ?> getBlockCapability(Class<T> clazz) {
        return (BlockCapability<T, ?>) blockCapabilities.get(clazz);
    }

    @SuppressWarnings("unchecked")
    public <T> ItemCapability<T, ?> getItemCapability(Class<T> clazz) {
        return (ItemCapability<T, ?>) itemCapabilities.get(clazz);
    }

    @SuppressWarnings("unchecked")
    public <T> EntityCapability<T, ?> getEntityCapability(Class<T> clazz) {
        return (EntityCapability<T, ?>) entityCapabilities.get(clazz);
    }
}
