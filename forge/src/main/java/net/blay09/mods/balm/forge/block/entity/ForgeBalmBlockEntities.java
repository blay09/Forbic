package net.blay09.mods.balm.forge.block.entity;

import net.blay09.mods.balm.api.DeferredObject;
import net.blay09.mods.balm.api.block.BalmBlockEntities;
import net.blay09.mods.balm.api.block.entity.BalmBlockEntityFactory;
import net.blay09.mods.balm.forge.DeferredRegisters;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Supplier;

public class ForgeBalmBlockEntities implements BalmBlockEntities {
    @Override
    public <T extends BlockEntity> DeferredObject<BlockEntityType<T>> registerBlockEntity(ResourceLocation identifier, BalmBlockEntityFactory<T> factory, Supplier<Block[]> blocks) {
        DeferredRegister<BlockEntityType<?>> register = DeferredRegisters.get(ForgeRegistries.BLOCK_ENTITY_TYPES, identifier.getNamespace());
        RegistryObject<BlockEntityType<T>> registryObject = register.register(identifier.getPath(), () -> {
            Block[] resolvedBlocks = blocks.get();
            return new BlockEntityType<>(factory::create, Set.of(resolvedBlocks));
        });
        return new DeferredObject<>(identifier, registryObject, registryObject::isPresent);
    }

    @Override
    public <T extends BlockEntity> DeferredObject<BlockEntityType<T>> registerBlockEntity(ResourceLocation identifier, BalmBlockEntityFactory<T> factory, DeferredObject<Block>... blocks) {
        DeferredRegister<BlockEntityType<?>> register = DeferredRegisters.get(ForgeRegistries.BLOCK_ENTITY_TYPES, identifier.getNamespace());
        RegistryObject<BlockEntityType<T>> registryObject = register.register(identifier.getPath(), () -> {
            Block[] resolvedBlocks = Arrays.stream(blocks).map(DeferredObject::get).toArray(Block[]::new);
            return new BlockEntityType<>(factory::create, Set.of(resolvedBlocks));
        });
        return new DeferredObject<>(identifier, registryObject, registryObject::isPresent);
    }
}
