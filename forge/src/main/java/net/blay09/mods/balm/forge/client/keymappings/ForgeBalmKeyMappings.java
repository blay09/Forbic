package net.blay09.mods.balm.forge.client.keymappings;

import com.mojang.blaze3d.platform.InputConstants;
import net.blay09.mods.balm.api.client.keymappings.KeyConflictContext;
import net.blay09.mods.balm.api.client.keymappings.KeyModifier;
import net.blay09.mods.balm.api.client.keymappings.KeyModifiers;
import net.blay09.mods.balm.common.client.keymappings.CommonBalmKeyMappings;
import net.blay09.mods.balm.forge.client.rendering.ForgeBalmModels;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

public class ForgeBalmKeyMappings extends CommonBalmKeyMappings {
    private static class Registrations {
        public final List<KeyMapping> keyMappings = new ArrayList<>();

        @SubscribeEvent
        public void registerKeyMappings(RegisterKeyMappingsEvent event) {
            keyMappings.forEach(event::register);
        }
    }

    private final Map<String, Registrations> registrations = new ConcurrentHashMap<>();

    @Override
    public KeyMapping registerKeyMapping(String name, KeyConflictContext conflictContext, KeyModifier modifier, InputConstants.Type type, int keyCode, String category) {
        KeyMapping keyMapping = new KeyMapping(name, toForge(conflictContext), toForge(modifier), type, keyCode, category);
        getActiveRegistrations().keyMappings.add(keyMapping);
        return keyMapping;
    }

    @Override
    public KeyMapping registerKeyMapping(String name, KeyConflictContext conflictContext, KeyModifiers modifiers, InputConstants.Type type, int keyCode, String category) {
        var keyModifiers = modifiers.asList();
        var mainModifier = !keyModifiers.isEmpty() ? keyModifiers.get(0) : KeyModifier.NONE;
        KeyMapping keyMapping = new KeyMapping(name, toForge(conflictContext), toForge(mainModifier), type, keyCode, category);
        getActiveRegistrations().keyMappings.add(keyMapping);
        if (keyModifiers.size() > 1) {
            registerModifierKeyMappings(keyMapping, conflictContext, keyModifiers.subList(1, keyModifiers.size()));
        }
        if (modifiers.hasCustomModifiers()) {
            registerCustomModifierKeyMappings(keyMapping, conflictContext, modifiers.getCustomModifiers());
        }
        return keyMapping;
    }

    @Override
    public boolean isActiveAndMatches(@Nullable KeyMapping keyMapping, InputConstants.Key input) {
        return isActive(keyMapping) && keyMapping.isActiveAndMatches(input);
    }

    @Override
    public boolean isActiveAndMatches(@Nullable KeyMapping keyMapping, int keyCode, int scanCode) {
        return isActive(keyMapping) && keyMapping.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode));
    }

    @Override
    public boolean isActiveAndMatches(@Nullable KeyMapping keyMapping, InputConstants.Type type, int keyCode, int scanCode) {
        if (!isActive(keyMapping)) {
            return false;
        }

        return type == InputConstants.Type.MOUSE ? keyMapping.isActiveAndMatches(InputConstants.Type.MOUSE.getOrCreate(keyCode)) : keyMapping.isActiveAndMatches(
                InputConstants.getKey(keyCode, scanCode));
    }

    private boolean isActiveAndMatchesStrictModifier(@Nullable KeyMapping keyMapping, int keyCode, int scanCode) {
        if (!isActive(keyMapping)) {
            return false;
        }

        if (keyMapping.getKeyModifier() == net.minecraftforge.client.settings.KeyModifier.NONE) {
            if (net.minecraftforge.client.settings.KeyModifier.SHIFT.isActive(keyMapping.getKeyConflictContext())
                    || net.minecraftforge.client.settings.KeyModifier.CONTROL.isActive(keyMapping.getKeyConflictContext())
                    || net.minecraftforge.client.settings.KeyModifier.ALT.isActive(keyMapping.getKeyConflictContext())) {
                return false;
            }
        }

        return keyMapping.matches(keyCode, scanCode);
    }

    @Override
    protected boolean isContextActive(KeyMapping keyMapping) {
        return keyMapping.getKeyConflictContext().isActive();
    }

    private static IKeyConflictContext toForge(KeyConflictContext context) {
        return switch (context) {
            case UNIVERSAL -> net.minecraftforge.client.settings.KeyConflictContext.UNIVERSAL;
            case GUI -> net.minecraftforge.client.settings.KeyConflictContext.GUI;
            case INGAME -> net.minecraftforge.client.settings.KeyConflictContext.IN_GAME;
        };
    }

    private static net.minecraftforge.client.settings.KeyModifier toForge(KeyModifier modifier) {
        return switch (modifier) {
            case SHIFT -> net.minecraftforge.client.settings.KeyModifier.SHIFT;
            case CONTROL -> net.minecraftforge.client.settings.KeyModifier.CONTROL;
            case ALT -> net.minecraftforge.client.settings.KeyModifier.ALT;
            default -> net.minecraftforge.client.settings.KeyModifier.NONE;
        };
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
