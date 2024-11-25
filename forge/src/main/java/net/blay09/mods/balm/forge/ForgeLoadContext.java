package net.blay09.mods.balm.forge;

import net.blay09.mods.balm.api.BalmRuntimeLoadContext;
import net.minecraftforge.eventbus.api.IEventBus;

public record ForgeLoadContext(IEventBus modBus) implements BalmRuntimeLoadContext {
}