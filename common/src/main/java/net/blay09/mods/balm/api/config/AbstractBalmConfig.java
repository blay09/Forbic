package net.blay09.mods.balm.api.config;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.ConfigReloadedEvent;
import net.blay09.mods.balm.api.event.PlayerLoginEvent;
import net.blay09.mods.balm.api.network.ConfigReflection;
import net.blay09.mods.balm.api.network.SyncConfigMessage;
import net.minecraft.ResourceLocationException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class AbstractBalmConfig implements BalmConfig {

    private final Map<Class<?>, BalmConfigData> activeConfigs = new HashMap<>();
    private final Map<Class<?>, BalmConfigData> defaultConfigs = new HashMap<>();
    private final Map<Class<?>, Function<?, ?>> syncMessageFactories = new HashMap<>();

    public void initialize() {
        Balm.getEvents().onEvent(PlayerLoginEvent.class, event -> {
            for (BalmConfigData config : activeConfigs.values()) {
                SyncConfigMessage<? extends BalmConfigData> message = getConfigSyncMessage(config.getClass());
                if (message != null) {
                    Balm.getNetworking().sendTo(event.getPlayer(), message);
                }
            }
        });

        Balm.getEvents().onEvent(ConfigReloadedEvent.class, event -> {
            MinecraftServer server = Balm.getHooks().getServer();
            if (server != null) {
                for (BalmConfigData config : activeConfigs.values()) {
                    SyncConfigMessage<? extends BalmConfigData> message = getConfigSyncMessage(config.getClass());
                    if (message != null) {
                        Balm.getNetworking().sendToAll(server, message);
                    }
                }
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BalmConfigData> T getActive(Class<T> clazz) {
        return (T) activeConfigs.get(clazz);
    }

    @SuppressWarnings("unchecked")
    public <T extends BalmConfigData> SyncConfigMessage<T> getConfigSyncMessage(Class<T> clazz) {
        Function<BalmConfigData, SyncConfigMessage<BalmConfigData>> factory = getConfigSyncMessageFactory(clazz);
        return factory != null ? (SyncConfigMessage<T>) factory.apply(getBackingConfig(clazz)) : null;
    }

    @SuppressWarnings("unchecked")
    public <T extends BalmConfigData> Function<BalmConfigData, SyncConfigMessage<BalmConfigData>> getConfigSyncMessageFactory(Class<T> clazz) {
        return (Function<BalmConfigData, SyncConfigMessage<BalmConfigData>>) syncMessageFactories.get(clazz);
    }

    public <T extends BalmConfigData> void setActiveConfig(Class<T> clazz, T config) {
        activeConfigs.put(clazz, config);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BalmConfigData> void handleSync(Player player, SyncConfigMessage<T> message) {
        T data = message.getData();
        setActiveConfig((Class<T>) data.getClass(), data);
    }

    @Override
    public <T extends BalmConfigData> void registerConfig(Class<T> clazz, Function<T, SyncConfigMessage<T>> syncMessageFactory) {
        setActiveConfig(clazz, Balm.getConfig().initializeBackingConfig(clazz));
        defaultConfigs.put(clazz, createConfigDataInstance(clazz));
        if (syncMessageFactory != null) {
            registerSyncMessageFactory(clazz, syncMessageFactory);
        }
    }

    private <T> void registerSyncMessageFactory(Class<T> clazz, Function<T, SyncConfigMessage<T>> syncMessageFactory) {
        syncMessageFactories.put(clazz, syncMessageFactory);
    }

    @Override
    public <T extends BalmConfigData> void updateConfig(Class<T> clazz, Consumer<T> consumer) {
        T backingConfig = getBackingConfig(clazz);
        consumer.accept(backingConfig);
        Balm.getConfig().saveBackingConfig(clazz);

        // If active config does not match backing config, apply changes to the active config as well
        // This assumes that the client-side does not use updateConfig to change server-side configs, as that would result in a desync
        T activeConfig = getActive(clazz);
        if (activeConfig != backingConfig) {
            consumer.accept(getActive(clazz));
        }
    }

    @Override
    public <T extends BalmConfigData> void resetToBackingConfig(Class<T> clazz) {
        setActiveConfig(clazz, getBackingConfig(clazz));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void resetToBackingConfigs() {
        for (Class<?> clazz : activeConfigs.keySet()) {
            resetToBackingConfig((Class<? extends BalmConfigData>) clazz);
        }
    }

    @NotNull
    protected <T> T createConfigDataInstance(Class<T> clazz) {
        try {
            return clazz.newInstance();

        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException("Config class or sub-class missing a public no-arg constructor.", e);
        }
    }

    @Override
    public File getConfigFile(String configName) {
        return new File(getConfigDir(), configName + "-common.toml");
    }

    @Override
    public <T extends BalmConfigData> String getConfigName(Class<T> clazz) {
        Config configAnnotation = clazz.getAnnotation(Config.class);
        if (configAnnotation == null) {
            throw new IllegalArgumentException("Config class " + clazz.getName() + " is missing @Config annotation");
        }
        return configAnnotation.value();
    }

    @Override
    public <T extends BalmConfigData> Table<String, String, BalmConfigProperty<?>> getConfigProperties(Class<T> clazz) {
        var backingConfig = Balm.getConfig().getBackingConfig(clazz);
        var defaultConfig = defaultConfigs.get(clazz);
        Table<String, String, BalmConfigProperty<?>> properties = HashBasedTable.create();
        for (Field rootField : ConfigReflection.getAllFields(clazz)) {
            var category = "";
            Class<?> fieldType = rootField.getType();
            if (isPropertyType(fieldType)) {
                var property = rootField.getName();
                properties.put(category, property, createConfigProperty(backingConfig, null, rootField, defaultConfig));
            } else {
                category = rootField.getName();
                for (Field propertyField : ConfigReflection.getAllFields(fieldType)) {
                    var property = propertyField.getName();
                    properties.put(category, property, createConfigProperty(backingConfig, rootField, propertyField, defaultConfig));
                }
            }
        }
        return properties;
    }

    private static BalmConfigProperty<?> createConfigProperty(BalmConfigData configData, Field categoryField, Field propertyField, BalmConfigData defaultConfig) {
        return new BalmConfigPropertyImpl<String>(configData, categoryField, propertyField, defaultConfig);
    }

    private static boolean isPropertyType(Class<?> type) {
        return type.isPrimitive()
                || type == String.class
                || type == Integer.class
                || type == Boolean.class
                || type == Float.class
                || type == Double.class
                || type == List.class
                || type == Set.class
                || type == ResourceLocationException.class
                || Enum.class.isAssignableFrom(type);
    }
}