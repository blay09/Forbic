package net.blay09.mods.balm.forge.block.entity;

import net.blay09.mods.balm.api.DeferredObject;
import net.blay09.mods.balm.api.block.BalmBlockEntities;
import net.blay09.mods.balm.api.block.entity.BalmBlockEntityFactory;
import net.blay09.mods.balm.forge.DeferredRegisters;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ForgeBalmBlockEntities implements BalmBlockEntities {
    @Override
    public <T extends BlockEntity> DeferredObject<BlockEntityType<T>> registerBlockEntity(ResourceLocation identifier, BalmBlockEntityFactory<T> factory, Supplier<Block[]> blocks) {
        final var register = DeferredRegisters.get(Registries.BLOCK_ENTITY_TYPE, identifier.getNamespace());
        final var registryObject = register.register(identifier.getPath(), () -> {
            final var resolvedBlocks = Set.of(blocks.get());
            return new BlockEntityType<>(factory::create, resolvedBlocks);
        });
        return new DeferredObject<>(identifier, registryObject, registryObject::isPresent);
    }

    @Override
    public <T extends BlockEntity> DeferredObject<BlockEntityType<T>> registerBlockEntity(ResourceLocation identifier, BalmBlockEntityFactory<T> factory, DeferredObject<Block>... blocks) {
        final var register = DeferredRegisters.get(Registries.BLOCK_ENTITY_TYPE, identifier.getNamespace());
        final var registryObject = register.register(identifier.getPath(), () -> {
            final var resolvedBlocks = Arrays.stream(blocks).map(DeferredObject::get).collect(Collectors.toSet());
            return new BlockEntityType<>(factory::create, resolvedBlocks);
        });
        return new DeferredObject<>(identifier, registryObject, registryObject::isPresent);
    }
}
