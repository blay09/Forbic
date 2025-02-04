package net.blay09.mods.balm.fabric.config;

import com.mojang.logging.LogUtils;
import net.blay09.mods.balm.api.config.BalmConfigData;
import net.blay09.mods.balm.api.config.ExpectedType;
import net.blay09.mods.balm.notoml.Notoml;
import net.blay09.mods.balm.notoml.NotomlError;
import net.blay09.mods.balm.notoml.NotomlParser;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class FabricConfigLoader {

    private static final Logger logger = LogUtils.getLogger();

    public static void load(File configFile, BalmConfigData configData) throws IOException {
        Notoml notoml = new Notoml();
        if (configFile.exists()) {
            try {
                String input = Files.readString(configFile.toPath());
                notoml = NotomlParser.parse(input);
            } catch (IOException e) {
                logger.error("Failed to load config file {}", configFile, e);
            }
        }
        for (String category : notoml.getProperties().rowKeySet()) {
            Object categoryInstance;
            if (category.isEmpty()) {
                categoryInstance = configData;
            } else {
                try {
                    var categoryField = configData.getClass().getField(category);
                    categoryInstance = categoryField.get(configData);
                } catch (NoSuchFieldException e) {
                    notoml.addError(new NotomlError("Unknown config category: '" + category + "'"));
                    continue;
                } catch (IllegalAccessException e) {
                    notoml.addError(new NotomlError("Error loading config category: '" + category + "'", e));
                    continue;
                }
            }

            var properties = notoml.getProperties().row(category);
            for (Map.Entry<String, Object> propertyEntry : properties.entrySet()) {
                var property = propertyEntry.getKey();
                var value = propertyEntry.getValue();
                try {
                    var propertyField = categoryInstance.getClass().getField(property);
                    var expectedTypeAnnotation = propertyField.getAnnotation(ExpectedType.class);
                    Class<?> innerType = expectedTypeAnnotation != null ? expectedTypeAnnotation.value() : null;
                    try {
                        Object convertedValue = convertValue(value, propertyField.getType(), innerType);
                        if (convertedValue != null) {
                            propertyField.set(categoryInstance, convertedValue);
                        }
                    } catch (IllegalArgumentException e) {
                        String expectedValueType = getExpectedValueTypeMessage(propertyField.getType(), innerType);
                        notoml.addError(new NotomlError("Invalid value for config property [" + category + "] '" + property + "': '" + value + "', expected " + expectedValueType));
                    }
                } catch (NoSuchFieldException e) {
                    notoml.addError(new NotomlError("Unknown config property: [" + category + "] '" + property + "'"));
                } catch (Exception e) {
                    notoml.addError(new NotomlError("Error loading config property [" + category + "] '" + property + "'", e));
                }
            }
        }

        if (notoml.hasErrors()) {
            logger.error("Errors were encountered when loading the config file {}:", configFile.getName());
            for (NotomlError error : notoml.getErrors()) {
                if (error.hasLine()) {
                    logger.error("- {} near line {}", error.getMessage(), error.getLine(), error.getCause());
                } else {
                    logger.error("- {}", error.getMessage(), error.getCause());
                }
            }
            File backupFile = getBackupConfigFile(configFile);
            configFile.renameTo(backupFile);
            FabricConfigSaver.save(configFile, configData);
            logger.error("The affected properties have been reset to their defaults and a backup of the corrupted version was created under {}",
                    backupFile.getName());
        } else {
            Notoml updated = FabricConfigSaver.toNotoml(configData);
            if (!notoml.containsProperties(updated)) {
                logger.info("The config file {} is missing some properties.", configFile.getName());
                File backupFile = getBackupConfigFile(configFile);
                configFile.renameTo(backupFile);
                FabricConfigSaver.save(configFile, configData);
                logger.info("The missing properties have been added and a backup of the previous version was created under {}", backupFile.getName());
            }
        }
    }

    private static File getBackupConfigFile(File configFile) {
        // Find first non-existing file with the same name ending in .bak.1, .bak.2, etc.
        File backupFile;
        int i = 1;
        do {
            backupFile = new File(configFile.getParentFile(), configFile.getName() + ".bak" + i);
            i++;
        } while (backupFile.exists());
        return backupFile;
    }

    private static String getExpectedValueTypeMessage(Class<?> type, Class<?> innerType) {
        if (type == Integer.class || type == Integer.TYPE) {
            return "integer";
        } else if (type == Long.class || type == Long.TYPE) {
            return "integer";
        } else if (type == Double.class || type == Double.TYPE || type == Float.class || type == Float.TYPE) {
            return "floating point number";
        } else if (type == Boolean.class || type == Boolean.TYPE) {
            return "boolean (true or false)";
        } else if (type == String.class) {
            return "string";
        } else if (type == ResourceLocation.class) {
            return "resource location";
        } else if (type == List.class) {
            return "list of " + getExpectedValueTypeMessage(innerType, null);
        } else if (type == Set.class) {
            return "set of " + getExpectedValueTypeMessage(innerType, null);
        } else if (Enum.class.isAssignableFrom(type)) {
            Enum<?>[] enumConstants = (Enum<?>[]) type.getEnumConstants();
            return "enum value (" + String.join(", ", Arrays.stream(enumConstants).map(Enum::name).toArray(String[]::new)) + ")";
        }
        return null;
    }

    private static Object convertValue(Object value, Class<?> type, Class<?> innerType) {
        if (type == Integer.class || type == Integer.TYPE) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            } else if (value instanceof String) {
                try {
                    return Integer.parseInt((String) value);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid integer value: '" + value + "'", e);
                }
            }
        } else if (type == Long.class || type == Long.TYPE) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            } else if (value instanceof String) {
                try {
                    return Long.parseLong((String) value);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid integer value: '" + value + "'", e);
                }
            }
        } else if (type == Double.class || type == Double.TYPE) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else if (value instanceof String stringValue) {
                try {
                    return Double.parseDouble(stringValue.replace(',', '.'));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid floating point value: '" + value + "'", e);
                }
            }
        } else if (type == Float.class || type == Float.TYPE) {
            if (value instanceof Number) {
                return ((Number) value).floatValue();
            } else if (value instanceof String stringValue) {
                try {
                    return Float.parseFloat(stringValue.replace(',', '.'));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid floating point value: '" + value + "'", e);
                }
            }
        } else if (type == String.class) {
            if (value instanceof String) {
                return value;
            } else {
                return value.toString();
            }
        } else if (type == ResourceLocation.class) {
            if (value instanceof String stringValue) {
                return ResourceLocation.parse(stringValue);
            } else {
                return ResourceLocation.parse(value.toString());
            }
        } else if (type == Boolean.class || type == Boolean.TYPE) {
            if (value instanceof Number) {
                return ((Number) value).intValue() != 0;
            } else if (value instanceof String stringValue) {
                if (stringValue.equalsIgnoreCase("true")) {
                    return true;
                } else if (stringValue.equalsIgnoreCase("false")) {
                    return false;
                } else {
                    throw new IllegalArgumentException("Invalid boolean value: '" + value + "'");
                }
            } else if (value instanceof Boolean) {
                return value;
            }
        } else if (type == List.class) {
            if (value instanceof List<?> list) {
                List<Object> convertedList = new ArrayList<>();
                for (Object entry : list) {
                    Object convertedEntry = convertValue(entry, innerType, null);
                    if (convertedEntry != null) {
                        convertedList.add(convertedEntry);
                    }
                }
                return convertedList;
            } else if (value.getClass() == innerType) {
                List<Object> list = new ArrayList<>();
                list.add(value);
                return list;
            } else {
                throw new IllegalArgumentException("Invalid list value: '" + value + "'");
            }
        } else if (type == Set.class) {
            if (value instanceof Collection<?> collection) {
                Set<Object> convertedSet = new HashSet<>();
                for (Object entry : collection) {
                    Object convertedEntry = convertValue(entry, innerType, null);
                    if (convertedEntry != null) {
                        convertedSet.add(convertedEntry);
                    }
                }
                return convertedSet;
            } else if (value.getClass() == innerType) {
                Set<Object> list = new HashSet<>();
                list.add(value);
                return list;
            } else {
                throw new IllegalArgumentException("Invalid list value: '" + value + "'");
            }
        } else if (Enum.class.isAssignableFrom(type)) {
            if (value instanceof String) {
                try {
                    return parseEnumValue(type, (String) value);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid enum value: '" + value + "'", e);
                }
            } else {
                throw new IllegalArgumentException("Invalid enum value: '" + value + "'");
            }
        }
        return null;
    }

    private static Object parseEnumValue(Class<?> type, String value) {
        for (Object enumConstant : type.getEnumConstants()) {
            if (enumConstant.toString().equalsIgnoreCase(value)) {
                return enumConstant;
            }
        }

        return null;
    }

}