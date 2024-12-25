package net.blay09.mods.balm.forge.block;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.forge.DeferredRegisters;
import net.blay09.mods.balm.api.DeferredObject;
import net.blay09.mods.balm.api.block.BalmBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;

public class ForgeBalmBlocks implements BalmBlocks {
    @Override
    public DeferredObject<Block> registerBlock(Function<ResourceLocation, Block> supplier, ResourceLocation identifier) {
        final var register = DeferredRegisters.get(Registries.BLOCK, identifier.getNamespace());
        final var registryObject = register.register(identifier.getPath(), () -> supplier.apply(identifier));
        return new DeferredObject<>(identifier, registryObject, registryObject::isPresent);
    }

    @Override
    public DeferredObject<Item> registerBlockItem(Function<ResourceLocation, BlockItem> supplier, ResourceLocation identifier, @Nullable ResourceLocation creativeTab) {
        return Balm.getItems().registerItem(supplier::apply, identifier, creativeTab);
    }

    @Override
    public void register(Function<ResourceLocation, Block> blockSupplier, BiFunction<Block, ResourceLocation, BlockItem> blockItemSupplier, ResourceLocation identifier, @Nullable ResourceLocation creativeTab) {
        final var deferredBlock = registerBlock(blockSupplier, identifier);
        registerBlockItem((id) -> blockItemSupplier.apply(deferredBlock.get(), id), identifier, creativeTab);
    }
}
