package net.blay09.mods.balm.api.config;

import com.google.common.collect.Table;
import net.blay09.mods.balm.api.network.SyncConfigMessage;
import net.minecraft.world.entity.player.Player;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public interface BalmConfig {
    <T extends BalmConfigData> T initializeBackingConfig(Class<T> clazz);

    <T extends BalmConfigData> T getBackingConfig(Class<T> clazz);

    <T extends BalmConfigData> void saveBackingConfig(Class<T> clazz);

    <T extends BalmConfigData> T getActive(Class<T> clazz);

    <T extends BalmConfigData> void handleSync(Player player, SyncConfigMessage<T> message);

    <T extends BalmConfigData> void registerConfig(Class<T> clazz, Function<T, SyncConfigMessage<T>> syncMessageFactory);

    <T extends BalmConfigData> void updateConfig(Class<T> clazz, Consumer<T> consumer);

    <T extends BalmConfigData> void resetToBackingConfig(Class<T> clazz);

    void resetToBackingConfigs();

    File getConfigDir();

    File getConfigFile(String configName);

    <T extends BalmConfigData> Table<String, String, BalmConfigProperty<?>> getConfigProperties(Class<T> clazz);

    <T extends BalmConfigData> String getConfigName(Class<T> clazz);

    List<? extends BalmConfigData> getConfigsByMod(String modId);
}
