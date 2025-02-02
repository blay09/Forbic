package net.blay09.mods.balm.neoforge.config;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.config.AbstractBalmConfig;
import net.blay09.mods.balm.api.config.BalmConfigData;
import net.blay09.mods.balm.api.config.Comment;
import net.blay09.mods.balm.api.config.ExpectedType;
import net.blay09.mods.balm.api.event.ConfigReloadedEvent;
import net.blay09.mods.balm.api.network.ConfigReflection;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class NeoForgeBalmConfig extends AbstractBalmConfig {

    private final Logger logger = LogManager.getLogger();
    private final Map<Class<?>, ModConfig> configs = new HashMap<>();
    private final Map<Class<?>, BalmConfigData> configData = new HashMap<>();
    private final Table<Class<?>, String, ModConfigSpec.ConfigValue<?>> configProperties = HashBasedTable.create();

    private <T extends BalmConfigData> IConfigSpec createConfigSpec(Class<T> clazz) {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        try {
            buildConfigSpec("", builder, clazz);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Config spec generation unexpectedly failed.", e);
        }
        return builder.build();
    }

    private void buildConfigSpec(String parentPath, ModConfigSpec.Builder builder, Class<?> clazz) throws IllegalAccessException {
        List<Field> fields = ConfigReflection.getAllFields(clazz);
        Object defaults = createConfigDataInstance(clazz);
        final var modContainer = ModLoadingContext.get().getActiveContainer();
        final var modId = modContainer.getModId();
        for (Field field : fields) {
            Class<?> type = field.getType();
            Object defaultValue = field.get(defaults);
            String path = parentPath + field.getName();

            Comment comment = field.getAnnotation(Comment.class);
            if (comment != null) {
                builder.comment(comment.value());
            }

            builder.translation("config." + modId + "." + path);

            if (String.class.isAssignableFrom(type)) {
                final var property = builder.define(path, (String) defaultValue);
                configProperties.put(clazz, path, property);
            } else if (ResourceLocation.class.isAssignableFrom(type)) {
                final var property = builder.define(path, ((ResourceLocation) defaultValue).toString());
                configProperties.put(clazz, path, property);
            } else if (Collection.class.isAssignableFrom(type)) {
                ExpectedType expectedType = field.getAnnotation(ExpectedType.class);
                if (expectedType == null) {
                    logger.warn("Config field without expected type, will not validate list content ({} in {})", field.getName(), clazz.getName());
                }

                Supplier<List<?>> defaultSupplier = () -> expectedType != null && expectedType.value()
                        .isEnum() ? ((Collection<?>) defaultValue).stream().map(Object::toString).toList() : new ArrayList<>((Collection<?>) defaultValue);
                Predicate<Object> validator = (Object it) -> expectedType == null || expectedType.value()
                        .isAssignableFrom(it.getClass()) || (expectedType.value()
                        .isEnum() && Arrays.stream(
                        expectedType.value().getEnumConstants()).anyMatch(constant -> constant.toString().equals(it)));
                if (expectedType != null && ResourceLocation.class.isAssignableFrom(expectedType.value())) {
                    defaultSupplier = () -> ((Collection<?>) defaultValue).stream().map(it -> ((ResourceLocation) it).toString()).collect(Collectors.toList());
                    validator = (Object it) -> it instanceof String stringValue && ResourceLocation.tryParse(stringValue) != null;
                }

                final var property = builder.defineListAllowEmpty(List.of(path.split("\\.")), defaultSupplier, validator);
                configProperties.put(clazz, path, property);
            } else if (Enum.class.isAssignableFrom(type)) {
                final var property = builder.defineEnum(path, (Enum) defaultValue);
                configProperties.put(clazz, path, property);
            } else if (int.class.isAssignableFrom(type)) {
                final var property = builder.defineInRange(path, (int) defaultValue, Integer.MIN_VALUE, Integer.MAX_VALUE);
                configProperties.put(clazz, path, property);
            } else if (float.class.isAssignableFrom(type)) {
                final var property = builder.defineInRange(path, (float) defaultValue, -Float.MAX_VALUE, Float.MAX_VALUE);
                configProperties.put(clazz, path, property);
            } else if (double.class.isAssignableFrom(type)) {
                final var property = builder.defineInRange(path, (double) defaultValue, -Double.MAX_VALUE, Double.MAX_VALUE);
                configProperties.put(clazz, path, property);
            } else if (boolean.class.isAssignableFrom(type)) {
                final var property = builder.define(path, (boolean) defaultValue);
                configProperties.put(clazz, path, property);
            } else if (long.class.isAssignableFrom(type)) {
                final var property = builder.defineInRange(path, (long) defaultValue, Long.MIN_VALUE, Long.MAX_VALUE);
                configProperties.put(clazz, path, property);

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
            final var spec = ((ModConfigSpec) config.getSpec()).getValues();
            boolean hasValue = spec.contains(path);
            Class<?> type = field.getType();
            try {
                if (hasValue && Integer.TYPE.isAssignableFrom(type)) {
                    final var value = (ModConfigSpec.IntValue) spec.get(path);
                    field.set(instance, value.get());
                } else if (hasValue && Long.TYPE.isAssignableFrom(type)) {
                    final var value = (ModConfigSpec.LongValue) spec.get(path);
                    field.set(instance, value.get());
                } else if (hasValue && Float.TYPE.isAssignableFrom(type)) {
                    Object value = spec.get(path);
                    if (value instanceof ModConfigSpec.DoubleValue doubleValue) {
                        field.set(instance, doubleValue.get().floatValue());
                    } else {
                        logger.error("Invalid config value for {}, expected {} but got {}", path, type.getName(), value.getClass());
                    }
                } else if (hasValue && Double.TYPE.isAssignableFrom(type)) {
                    Object value = spec.get(path);
                    if (value instanceof ModConfigSpec.DoubleValue doubleValue) {
                        field.set(instance, doubleValue.getAsDouble());
                    } else {
                        logger.error("Invalid config value for {}, expected {} but got {}", path, type.getName(), value.getClass());
                    }
                } else if (hasValue && ResourceLocation.class.isAssignableFrom(type)) {
                    final var value = (ModConfigSpec.ConfigValue<String>) spec.get(path);
                    field.set(instance, ResourceLocation.parse(value.get()));
                } else if (hasValue && (Collection.class.isAssignableFrom(type))) {
                    final var value = (ModConfigSpec.ConfigValue<?>) spec.getRaw(path);
                    final var raw = value.get();
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
                    final var value = spec.get(path);
                    if (value instanceof ModConfigSpec.ConfigValue<?> stringValue) {
                        try {
                            field.set(instance, stringValue.get());
                        } catch (IllegalArgumentException e) {
                            logger.error("Invalid config value for " + path + ", expected " + type.getName() + " but got " + value.getClass());
                        }
                    } else {
                        logger.error("Null config value for " + path + ", falling back to default");
                    }
                } else if (hasValue && type.isEnum()) {
                    final var value = (ModConfigSpec.EnumValue<?>) spec.get(path);
                    field.set(instance, value.get());
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

    private <T extends BalmConfigData> void writeConfigValues(Class<?> clazz, T configData) {
        try {
            writeConfigValues("", clazz, configData);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private <T> void writeConfigValues(String parentPath, Class<?> clazz, T instance) throws IllegalAccessException {
        List<Field> fields = ConfigReflection.getAllFields(instance.getClass());
        for (Field field : fields) {
            String path = parentPath + field.getName();
            Class<?> type = field.getType();
            Object value = field.get(instance);
            if (type.isPrimitive() || Enum.class.isAssignableFrom(type) || String.class.isAssignableFrom(type)) {
                final var property = (ModConfigSpec.ConfigValue<Object>) configProperties.get(clazz, path);
                if (property != null) {
                    property.set(value);
                }
            } else if (ResourceLocation.class.isAssignableFrom(type)) {
                final var property = (ModConfigSpec.ConfigValue<Object>) configProperties.get(clazz, path);
                if (property != null) {
                    property.set(((ResourceLocation) value).toString());
                }
            } else if (Collection.class.isAssignableFrom(type)) {
                final var property = (ModConfigSpec.ConfigValue<Object>) configProperties.get(clazz, path);
                if (property != null) {
                    property.set(new ArrayList<>((Collection<?>) value));
                }
            } else {
                writeConfigValues(path + ".", field.getType(), field.get(instance));
            }
        }
    }

    @Override
    public <T extends BalmConfigData> T initializeBackingConfig(Class<T> clazz) {
        IConfigSpec configSpec = createConfigSpec(clazz);
        final var modContainer = ModLoadingContext.get().getActiveContainer();

        modContainer.getEventBus().addListener((ModConfigEvent.Loading event) -> {
            configs.put(clazz, event.getConfig());
            T newConfigData = readConfigValues(clazz, event.getConfig());
            configData.put(clazz, newConfigData);

            setActiveConfig(clazz, newConfigData);
        });

        modContainer.getEventBus().addListener((ModConfigEvent.Reloading event) -> {
            configs.put(clazz, event.getConfig());
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

        modContainer.registerConfig(ModConfig.Type.COMMON, configSpec);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            initializeConfigurationScreen(modContainer);
        }

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
            writeConfigValues(clazz, configData.get(clazz));
            ((ModConfigSpec) modConfig.getSpec()).save();
        }
    }

    @Override
    public File getConfigDir() {
        return FMLPaths.CONFIGDIR.get().toFile();
    }

    private static void initializeConfigurationScreen(ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
