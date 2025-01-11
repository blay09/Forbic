package net.blay09.mods.balm.fabric;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.config.AbstractBalmConfig;
import net.blay09.mods.balm.api.container.BalmContainerProvider;
import net.blay09.mods.balm.api.energy.EnergyStorage;
import net.blay09.mods.balm.api.entity.BalmEntity;
import net.blay09.mods.balm.api.fluid.BalmFluidTankProvider;
import net.blay09.mods.balm.api.fluid.FluidTank;
import net.blay09.mods.balm.api.proxy.SidedProxy;
import net.blay09.mods.balm.config.ExampleConfig;
import net.blay09.mods.balm.fabric.fluid.BalmFluidStorage;
import net.blay09.mods.balm.fabric.provider.FabricBalmProviders;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;

import static net.blay09.mods.balm.api.Balm.sidedProxy;

public class FabricBalm implements ModInitializer {

    private static final SidedProxy<FabricBalmProxy> proxy = sidedProxy("net.blay09.mods.balm.fabric.FabricBalmProxy",
            "net.blay09.mods.balm.fabric.client.FabricBalmClientProxy");

    @Override
    public void onInitialize() {
        ((FabricBalmHooks) Balm.getHooks()).initialize();
        ((AbstractBalmConfig) Balm.getConfig()).initialize();
        ExampleConfig.initialize();

        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            CompoundTag data = ((BalmEntity) oldPlayer).getFabricBalmData();
            ((BalmEntity) newPlayer).setFabricBalmData(data);
        });

        var providers = ((FabricBalmProviders) Balm.getProviders());
        providers.registerProvider(new ResourceLocation("balm", "container"), Container.class);
        providers.registerProvider(new ResourceLocation("balm", "fluid_tank"), FluidTank.class);
        providers.registerProvider(new ResourceLocation("balm", "energy_storage"), EnergyStorage.class);

        ItemStorage.SIDED.registerFallback((world, pos, state, blockEntity, direction) -> {
            if (blockEntity instanceof BalmContainerProvider containerProvider) {
                final var container = containerProvider.getContainer(direction);
                if (container != null) {
                    return InventoryStorage.of(container, direction);
                }
            }

            return null;
        });

        FluidStorage.SIDED.registerFallback((world, pos, state, blockEntity, direction) -> {
            if (blockEntity instanceof BalmFluidTankProvider fluidTankProvider) {
                final var fluidTank = fluidTankProvider.getFluidTank(direction);
                if (fluidTank != null) {
                    return new BalmFluidStorage(fluidTank);
                }
            }

            return null;
        });

        Balm.initializeIfLoaded("team_reborn_energy", "net.blay09.mods.balm.fabric.compat.energy.RebornEnergy");
    }

    public static FabricBalmProxy getProxy() {
        return proxy.get();
    }
}
