package net.blay09.mods.balm.forge.config;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.config.AbstractBalmConfig;
import net.blay09.mods.balm.api.config.BalmConfigData;
import net.blay09.mods.balm.api.config.Comment;
import net.blay09.mods.balm.api.config.ExpectedType;
import net.blay09.mods.balm.api.event.ConfigReloadedEvent;
import net.blay09.mods.balm.api.network.ConfigReflection;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ForgeBalmConfig extends AbstractBalmConfig {

    private final Logger logger = LogManager.getLogger();
    private final Map<Class<?>, ModConfig> configs = new HashMap<>();
    private final Multimap<String, Class<?>> configsByMod = ArrayListMultimap.create();
    private final Map<Class<?>, BalmConfigData> configData = new HashMap<>();

    private <T extends BalmConfigData> IConfigSpec<?> createConfigSpec(Class<T> clazz) {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        try {
            buildConfigSpec("", builder, clazz);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Config spec generation unexpectedly failed.", e);
        }
        return builder.build();
    }

    private void buildConfigSpec(String parentPath, ForgeConfigSpec.Builder builder, Class<?> clazz) throws IllegalAccessException {
        List<Field> fields = ConfigReflection.getAllFields(clazz);
        Object defaults = createConfigDataInstance(clazz);
        for (Field field : fields) {
            Class<?> type = field.getType();
            Object defaultValue = field.get(defaults);
            String path = parentPath + field.getName();

            Comment comment = field.getAnnotation(Comment.class);
            if (comment != null) {
                builder.comment(comment.value());
            }

            if (String.class.isAssignableFrom(type)) {
                builder.define(path, (String) defaultValue);
            } else if (ResourceLocation.class.isAssignableFrom(type)) {
                builder.define(path, ((ResourceLocation) defaultValue).toString());
            } else if (Collection.class.isAssignableFrom(type)) {
                ExpectedType expectedType = field.getAnnotation(ExpectedType.class);
                if (expectedType == null) {
                    logger.warn("Config field without expected type, will not validate list content ({} in {})", field.getName(), clazz.getName());
                }

                Supplier<List<?>> defaultSupplier = () -> new ArrayList<>((Collection<?>) defaultValue);
                Predicate<Object> validator = (Object it) -> expectedType == null || expectedType.value()
                        .isAssignableFrom(it.getClass()) || (expectedType.value()
                        .isEnum() && Arrays.stream(
                        expectedType.value().getEnumConstants()).anyMatch(constant -> constant.toString().equals(it)));
                if (expectedType != null && ResourceLocation.class.isAssignableFrom(expectedType.value())) {
                    defaultSupplier = () -> ((Collection<?>) defaultValue).stream().map(it -> ((ResourceLocation) it).toString()).collect(Collectors.toList());
                    validator = (Object it) -> it instanceof String stringValue && ResourceLocation.tryParse(stringValue) != null;
                }

                builder.defineListAllowEmpty(List.of(path.split("\\.")), defaultSupplier, validator);
            } else if (Enum.class.isAssignableFrom(type)) {
                builder.defineEnum(path, (Enum) defaultValue);
            } else if (int.class.isAssignableFrom(type)) {
                builder.defineInRange(path, (int) defaultValue, Integer.MIN_VALUE, Integer.MAX_VALUE);
            } else if (float.class.isAssignableFrom(type)) {
                builder.defineInRange(path, (float) defaultValue, -Float.MAX_VALUE, Float.MAX_VALUE);
            } else if (double.class.isAssignableFrom(type)) {
                builder.defineInRange(path, (double) defaultValue, -Double.MAX_VALUE, Double.MAX_VALUE);
            } else if (boolean.class.isAssignableFrom(type)) {
                builder.define(path, (boolean) defaultValue);
            } else if (long.class.isAssignableFrom(type)) {
                builder.defineInRange(path, (long) defaultValue, Long.MIN_VALUE, Long.MAX_VALUE);
            } else {
                buildConfigSpec(path + ".", builder, type);
            }
        }
    }

    private <T extends BalmConfigData> T readConfigValues(Class<T> clazz, ModConfig config) {
        T instance = createConfigDataInstance(clazz);
        try {
            readConfigValues("", instance, config);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return instance;
    }

    private <T> void readConfigValues(String parentPath, T instance, ModConfig config) throws IllegalAccessException {
        List<Field> fields = ConfigReflection.getAllFields(instance.getClass());
        for (Field field : fields) {
            String path = parentPath + field.getName();
            boolean hasValue = config.getConfigData().contains(path);
            Class<?> type = field.getType();
            try {
                if (hasValue && Integer.TYPE.isAssignableFrom(type)) {
                    field.set(instance, config.getConfigData().getInt(path));
                } else if (hasValue && Long.TYPE.isAssignableFrom(type)) {
                    field.set(instance, config.getConfigData().getLong(path));
                } else if (hasValue && Float.TYPE.isAssignableFrom(type)) {
                    Object value = config.getConfigData().get(path);
                    if (value instanceof Double doubleValue) {
                        field.set(instance, doubleValue.floatValue());
                    } else if (value instanceof Float floatValue) {
                        field.set(instance, floatValue);
                    } else {
                        logger.error("Invalid config value for " + path + ", expected " + type.getName() + " but got " + value.getClass());
                    }
                } else if (hasValue && Double.TYPE.isAssignableFrom(type)) {
                    Object value = config.getConfigData().get(path);
                    if (value instanceof Double doubleValue) {
                        field.set(instance, doubleValue);
                    } else if (value instanceof Float floatValue) {
                        field.set(instance, floatValue.doubleValue());
                    } else {
                        logger.error("Invalid config value for " + path + ", expected " + type.getName() + " but got " + value.getClass());
                    }
                } else if (hasValue && ResourceLocation.class.isAssignableFrom(type)) {
                    field.set(instance, ResourceLocation.parse(config.getConfigData().get(path)));
                } else if (hasValue && (Collection.class.isAssignableFrom(type))) {
                    Object raw = config.getConfigData().getRaw(path);
                    if (raw instanceof List<?> list) {
                        ExpectedType expectedType = field.getAnnotation(ExpectedType.class);
                        Function<Object, Object> mapper = (it) -> it;
                        if (expectedType != null && ResourceLocation.class.isAssignableFrom(expectedType.value())) {
                            mapper = (it) -> ResourceLocation.parse((String) it);
                        } else if (expectedType != null && Enum.class.isAssignableFrom(expectedType.value())) {
                            mapper = (it) -> parseEnumValue(expectedType.value(), (String) it);
                        }
                        try {
                            if (List.class.isAssignableFrom(type)) {
                                field.set(instance, list.stream().map(mapper).collect(Collectors.toList()));
                            } else if (Set.class.isAssignableFrom(type)) {
                                field.set(instance, list.stream().map(mapper).collect(Collectors.toSet()));
                            }
                        } catch (IllegalArgumentException e) {
                            logger.error("Invalid config value for " + path + ", expected " + type.getName() + " but got " + raw.getClass());
                        }
                    } else {
                        logger.error("Null config value for " + path + ", falling back to default");
                    }
                } else if (hasValue && (type.isPrimitive() || String.class.isAssignableFrom(type))) {
                    Object raw = config.getConfigData().getRaw(path);
                    if (raw != null) {
                        try {
                            field.set(instance, raw);
                        } catch (IllegalArgumentException e) {
                            logger.error("Invalid config value for " + path + ", expected " + type.getName() + " but got " + raw.getClass());
                        }
                    } else {
                        logger.error("Null config value for " + path + ", falling back to default");
                    }
                } else if (hasValue && type.isEnum()) {
                    Enum<?> value = config.getConfigData().getEnum(path, (Class<Enum>) type);
                    field.set(instance, value);
                } else {
                    readConfigValues(path + ".", field.get(instance), config);
                }
            } catch (Exception e) {
                logger.error("Unexpected error loading config value for " + path + ", falling back to default", e);
            }
        }
    }

    private static Object parseEnumValue(Class<?> type, String value) {
        for (Object enumConstant : type.getEnumConstants()) {
            if (enumConstant.toString().equalsIgnoreCase(value)) {
                return enumConstant;
            }
        }

        return null;
    }

    private <T extends BalmConfigData> void writeConfigValues(ModConfig config, T configData) {
        try {
            writeConfigValues("", config, configData);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private <T> void writeConfigValues(String parentPath, ModConfig config, T instance) throws IllegalAccessException {
        List<Field> fields = ConfigReflection.getAllFields(instance.getClass());
        for (Field field : fields) {
            String path = parentPath + field.getName();
            Class<?> type = field.getType();
            Object value = field.get(instance);
            if (type.isPrimitive() || Enum.class.isAssignableFrom(type) || String.class.isAssignableFrom(type)) {
                config.getConfigData().set(path, value);
            } else if (ResourceLocation.class.isAssignableFrom(type)) {
                config.getConfigData().set(path, ((ResourceLocation) value).toString());
            } else if (Collection.class.isAssignableFrom(type)) {
                config.getConfigData().set(path, new ArrayList<>((Collection<?>) value));
            } else {
                writeConfigValues(path + ".", config, field.get(instance));
            }
        }
    }

    @Override
    public <T extends BalmConfigData> T initializeBackingConfig(Class<T> clazz) {
        IConfigSpec<?> configSpec = createConfigSpec(clazz);

        FMLJavaModLoadingContext.get().getModEventBus().addListener((ModConfigEvent.Loading event) -> {
            configs.put(clazz, event.getConfig());
            T newConfigData = readConfigValues(clazz, event.getConfig());
            configData.put(clazz, newConfigData);

            setActiveConfig(clazz, newConfigData);
        });

        FMLJavaModLoadingContext.get().getModEventBus().addListener((ModConfigEvent.Reloading event) -> {
            configs.put(clazz, event.getConfig());
            if (event.getConfig().getConfigData() == null) {
                logger.warn("Received a config reload event with a null config data. Ignoring.");
                return;
            }

            T newConfigData = readConfigValues(clazz, event.getConfig());
            configData.put(clazz, newConfigData);

            // Only rewrite active configs with reload if we're the hosting server or there is no syncing
            // TODO would be good if this still applied non-synced properties
            boolean hasSyncMessage = getConfigSyncMessageFactory(clazz) != null;
            boolean isHostingServer = ServerLifecycleHooks.getCurrentServer() != null;
            boolean isIngame = Balm.getProxy().isIngame();
            if (!hasSyncMessage || isHostingServer || !isIngame) {
                setActiveConfig(clazz, newConfigData);
            }

            Balm.getEvents().fireEvent(new ConfigReloadedEvent());
        });

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, configSpec);

        T initialData = createConfigDataInstance(clazz);
        configData.put(clazz, initialData);
        return initialData;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BalmConfigData> T getBackingConfig(Class<T> clazz) {
        return (T) configData.get(clazz);
    }

    @Override
    public <T extends BalmConfigData> void saveBackingConfig(Class<T> clazz) {
        ModConfig modConfig = configs.get(clazz);
        if (modConfig != null) {
            writeConfigValues(modConfig, configData.get(clazz));
            modConfig.save();
        }
    }

    @Override
    public File getConfigDir() {
        return FMLPaths.CONFIGDIR.get().toFile();
    }

    @Override
    public List<? extends BalmConfigData> getConfigsByMod(String modId) {
        return configsByMod.get(modId).stream().map(configData::get).toList();
    }
}
