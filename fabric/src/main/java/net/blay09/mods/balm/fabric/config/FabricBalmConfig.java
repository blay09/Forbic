package net.blay09.mods.balm.fabric.config;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.mojang.logging.LogUtils;
import net.blay09.mods.balm.api.config.AbstractBalmConfig;
import net.blay09.mods.balm.api.config.BalmConfigData;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FabricBalmConfig extends AbstractBalmConfig {

    private static final Logger logger = LogUtils.getLogger();
    private final Multimap<String, Class<?>> configsByMod = ArrayListMultimap.create();
    private final Map<Class<?>, BalmConfigData> configs = new HashMap<>();

    @Override
    public <T extends BalmConfigData> T initializeBackingConfig(Class<T> clazz) {
        var configName = getConfigName(clazz);
        var configFile = getConfigFile(configName);
        var configData = createConfigDataInstance(clazz);
        if (configFile.exists()) {
            try {
                FabricConfigLoader.load(configFile, configData);
            } catch (IOException e) {
                logger.error("Failed to load config file {}", configFile, e);
            }
        } else {
            try {
                FabricConfigSaver.save(configFile, configData);
            } catch (IOException e) {
                logger.error("Failed to generate config file {}", configFile, e);
            }
        }
        configs.put(clazz, configData);
        configsByMod.put(configName, clazz);
        return configData;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BalmConfigData> T getBackingConfig(Class<T> clazz) {
        return (T) configs.get(clazz);
    }

    @Override
    public <T extends BalmConfigData> void saveBackingConfig(Class<T> clazz) {
        var configName = getConfigName(clazz);
        var configFile = getConfigFile(configName);
        try {
            FabricConfigSaver.save(configFile, configs.get(clazz));
        } catch (IOException e) {
            logger.error("Failed to save config file {}", configFile, e);
        }
    }

    @Override
    public File getConfigDir() {
        return FabricLoader.getInstance().getConfigDir().toFile();
    }

    @Override
    public List<? extends BalmConfigData> getConfigsByMod(String modId) {
        return configsByMod.get(modId).stream().map(configs::get).toList();
    }
}
