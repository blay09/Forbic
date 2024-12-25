package net.blay09.mods.balm.forge.entity;

import net.blay09.mods.balm.api.DeferredObject;
import net.blay09.mods.balm.api.entity.BalmEntities;
import net.blay09.mods.balm.forge.DeferredRegisters;
import net.blay09.mods.balm.forge.world.ForgeBalmWorldGen;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ForgeBalmEntities implements BalmEntities {

    private static class Registrations {
        public final Map<EntityType<?>, AttributeSupplier> attributeSuppliers = new HashMap<>();

        @SubscribeEvent
        @SuppressWarnings("unchecked")
        public void registerAttributes(EntityAttributeCreationEvent event) {
            for (Map.Entry<EntityType<?>, AttributeSupplier> entry : attributeSuppliers.entrySet()) {
                event.put((EntityType<? extends LivingEntity>) entry.getKey(), entry.getValue());
            }
        }
    }

    private final Map<String, Registrations> registrations = new ConcurrentHashMap<>();

    @Override
    public <T extends Entity> DeferredObject<EntityType<T>> registerEntity(ResourceLocation identifier, EntityType.Builder<T> typeBuilder) {
        final var register = DeferredRegisters.get(Registries.ENTITY_TYPE, identifier.getNamespace());
        final var registryObject = register.register(identifier.getPath(), () -> typeBuilder.build(ResourceKey.create(Registries.ENTITY_TYPE, identifier)));
        return new DeferredObject<>(identifier, registryObject, registryObject::isPresent);
    }

    @Override
    public <T extends LivingEntity> DeferredObject<EntityType<T>> registerEntity(ResourceLocation identifier, EntityType.Builder<T> typeBuilder, Supplier<AttributeSupplier.Builder> attributeBuilder) {
        final var register = DeferredRegisters.get(Registries.ENTITY_TYPE, identifier.getNamespace());
        final var registrations = getActiveRegistrations();
        final var registryObject = register.register(identifier.getPath(), () -> {
            EntityType<T> entityType = typeBuilder.build(ResourceKey.create(Registries.ENTITY_TYPE, identifier));
            registrations.attributeSuppliers.put(entityType, attributeBuilder.get().build());
            return entityType;
        });
        return new DeferredObject<>(identifier, registryObject, registryObject::isPresent);
    }

    public void register(String modId, IEventBus eventBus) {
        eventBus.register(getRegistrations(modId));
    }

    private Registrations getActiveRegistrations() {
        return getRegistrations(ModLoadingContext.get().getActiveNamespace());
    }

    private Registrations getRegistrations(String modId) {
        return registrations.computeIfAbsent(modId, it -> new Registrations());
    }
}
