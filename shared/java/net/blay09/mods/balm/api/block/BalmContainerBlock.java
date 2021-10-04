package net.blay09.mods.balm.api.block;

import net.blay09.mods.balm.api.container.BalmContainerProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public interface BalmContainerBlock extends WorldlyContainerHolder {
    @Override
    default WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof BalmContainerProvider) {
            // ((BalmContainerProvider) blockEntity).getContainer();
            // ((BalmContainerProvider) blockEntity).getContainer(direction);
        }
        return null; // TODO
    }
}