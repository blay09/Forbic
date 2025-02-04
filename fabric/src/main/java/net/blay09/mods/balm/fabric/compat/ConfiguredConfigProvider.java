package net.blay09.mods.balm.fabric.compat;

import com.mrcrayfish.configured.api.*;
import com.mrcrayfish.configured.api.util.ConfigScreenHelper;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.config.BalmConfigData;
import net.blay09.mods.balm.api.config.BalmConfigProperty;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.ClassUtils;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ConfiguredConfigProvider implements IModConfigProvider {
    @Override
    public Set<IModConfig> getConfigurationsForMod(ModContext modContext) {
        final var configs = Balm.getConfig().getConfigsByMod(modContext.modId());
        return configs.stream().map(it -> mapConfig(modContext.modId(), it)).collect(Collectors.toSet());
    }

    private static IModConfig mapConfig(String modId, BalmConfigData configData) {
        return new IModConfig() {
            @Override
            public void update(IConfigEntry entry) {
                Balm.getConfig().saveBackingConfig(configData.getClass());
            }

            @Override
            public IConfigEntry getRoot() {
                return mapConfigRoot(modId, configData);
            }

            @Override
            public ConfigType getType() {
                return ConfigType.UNIVERSAL;
            }

            @Override
            public String getFileName() {
                return modId + "-common.toml";
            }

            @Override
            public String getModId() {
                return modId;
            }

            @Override
            public void loadWorldConfig(Path path, Consumer<IModConfig> consumer) {
            }
        };
    }

    private static IConfigEntry mapConfigRoot(String modId, BalmConfigData configData) {
        final var properties = Balm.getConfig().getConfigProperties(configData.getClass());
        final var children = new ArrayList<IConfigEntry>();
        for (final var category : properties.rowKeySet()) {
            if (category.isEmpty()) {
                properties.row(category)
                        .entrySet()
                        .stream()
                        .map(it -> mapConfigProperty(modId, category, it.getKey(), it.getValue()))
                        .forEach(children::add);
            } else {
                children.add(mapConfigCategory(modId, category, properties.row(category)));
            }
        }
        return new IConfigEntry() {
            @Override
            public List<IConfigEntry> getChildren() {
                return children;
            }

            @Override
            public boolean isRoot() {
                return true;
            }

            @Override
            public boolean isLeaf() {
                return false;
            }

            @Override
            public IConfigValue<?> getValue() {
                return null;
            }

            @Override
            public String getEntryName() {
                return "";
            }

            @Override
            public Component getTooltip() {
                return null;
            }

            @Override
            public String getTranslationKey() {
                return "config." + modId + ".title";
            }
        };
    }

    private static IConfigEntry mapConfigCategory(String modId, String category, Map<String, BalmConfigProperty<?>> properties) {
        final var children = properties.keySet().stream().map(it -> mapConfigProperty(modId, category, it, properties.get(it))).toList();
        return new IConfigEntry() {
            @Override
            public List<IConfigEntry> getChildren() {
                return children;
            }

            @Override
            public boolean isRoot() {
                return false;
            }

            @Override
            public boolean isLeaf() {
                return false;
            }

            @Override
            public IConfigValue<?> getValue() {
                return null;
            }

            @Override
            public String getEntryName() {
                return category;
            }

            @Override
            public Component getTooltip() {
                return null;
            }

            @Override
            public String getTranslationKey() {
                return "config." + modId + "." + category;
            }
        };
    }

    private static <T> IConfigEntry mapConfigProperty(String modId, String category, String key, BalmConfigProperty<T> property) {
        final var initialValue = property.getValue();
        return new IConfigEntry() {
            @Override
            public List<IConfigEntry> getChildren() {
                return List.of();
            }

            @Override
            public boolean isRoot() {
                return false;
            }

            @Override
            public boolean isLeaf() {
                return true;
            }

            @Override
            public IConfigValue<?> getValue() {
                return new IConfigValue<T>() {
                    @Override
                    public T get() {
                        return property.getValue();
                    }

                    @Override
                    public T getDefault() {
                        return property.getDefaultValue();
                    }

                    @Override
                    public void set(T o) {
                        property.setValue(o);
                    }

                    @Override
                    public boolean isValid(T o) {
                        return ClassUtils.isAssignable(o.getClass(), property.getType(), true);
                    }

                    @Override
                    public boolean isDefault() {
                        return Objects.equals(property.getDefaultValue(), property.getValue());
                    }

                    @Override
                    public boolean isChanged() {
                        return !Objects.equals(property.getValue(), initialValue);
                    }

                    @Override
                    public void restore() {
                        property.setValue(property.getDefaultValue());
                    }

                    @Override
                    public Component getComment() {
                        return category.isEmpty() ? Component.translatable("config." + modId + "." + key + ".tooltip") : Component.translatable("config." + modId + "." + category + "." + key + ".tooltip");
                    }

                    @Override
                    public String getTranslationKey() {
                        return category.isEmpty() ? "config." + modId + "." + key : "config." + modId + "." + category + "." + key;
                    }

                    @Override
                    public Component getValidationHint() {
                        return null;
                    }

                    @Override
                    public String getName() {
                        return key;
                    }

                    @Override
                    public void cleanCache() {
                    }

                    @Override
                    public boolean requiresWorldRestart() {
                        return false;
                    }

                    @Override
                    public boolean requiresGameRestart() {
                        return false;
                    }
                };
            }

            @Override
            public String getEntryName() {
                return category + "." + key;
            }

            @Override
            public Component getTooltip() {
                return category.isEmpty() ? Component.translatable("config." + modId + "." + key + ".tooltip") : Component.translatable("config." + modId + "." + category + "." + key + ".tooltip");
            }

            @Override
            public String getTranslationKey() {
                return category.isEmpty() ? "config." + modId + "." + key : "config." + modId + "." + category + "." + key;
            }
        };
    }

    public static Screen createConfigScreen(String modId, Screen parent) {
        final var configs = Balm.getConfig().getConfigsByMod(modId);
        final var configsByType = new HashMap<ConfigType, Set<IModConfig>>();
        final var mappedConfigs = configs.stream().map(it -> mapConfig(modId, it)).collect(Collectors.toSet());
        configsByType.put(ConfigType.UNIVERSAL, mappedConfigs);
        return ConfigScreenHelper.createSelectionScreen(parent,
                Component.translatable("config." + modId + ".title"),
                configsByType,
                new ResourceLocation("textures/block/stone.png")
        );
    }
}
