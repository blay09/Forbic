package net.blay09.mods.balm.fabric.config;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.mojang.logging.LogUtils;
import net.blay09.mods.balm.api.config.BalmConfigData;
import net.blay09.mods.balm.api.config.Comment;
import net.blay09.mods.balm.api.network.ConfigReflection;
import net.blay09.mods.balm.notoml.Notoml;
import net.blay09.mods.balm.notoml.NotomlSerializer;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

public class FabricConfigSaver {

    private static final Logger logger = LogUtils.getLogger();

    public static Notoml toNotoml(BalmConfigData configData) {
        Table<String, String, Object> properties = HashBasedTable.create();
        Table<String, String, String> comments = HashBasedTable.create();
        for (Field rootField : ConfigReflection.getAllFields(configData.getClass())) {
            var isCategory = !isPropertyType(rootField.getType());
            var category = isCategory ? rootField.getName() : "";
            if (isCategory) {
                var categoryComment = rootField.getAnnotation(Comment.class);
                if (categoryComment != null) {
                    comments.put(category, "", categoryComment.value());
                }
                try {
                    var categoryInstance = rootField.get(configData);
                    for (Field propertyField : ConfigReflection.getAllFields(categoryInstance.getClass())) {
                        var property = propertyField.getName();
                        var propertyComment = propertyField.getAnnotation(Comment.class);
                        if (propertyComment != null) {
                            comments.put(category, property, propertyComment.value());
                        }
                        try {
                            var value = propertyField.get(categoryInstance);
                            properties.put(category, property, value);
                        } catch (Exception e) {
                            logger.error("Failed to save config category {}", category, e);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to save config category {}", category, e);
                }
            } else {
                try {
                    var property = rootField.getName();
                    var propertyComment = rootField.getAnnotation(Comment.class);
                    if (propertyComment != null) {
                        comments.put(category, property, propertyComment.value());
                    }
                    var value = rootField.get(configData);
                    properties.put(category, property, value);
                } catch (Exception e) {
                    logger.error("Failed to save config property {}", rootField.getName(), e);
                }
            }
        }
        return new Notoml(properties, comments);
    }

    public static void save(File configFile, BalmConfigData configData) throws IOException {
        var notoml = toNotoml(configData);
        try (FileWriter writer = new FileWriter(configFile)) {
            NotomlSerializer.serialize(writer, notoml);
        }
    }

    private static boolean isPropertyType(Class<?> type) {
        return type.isPrimitive()
                || type == String.class
                || type == Integer.class
                || type == Boolean.class
                || type == Float.class
                || type == Double.class
                || type == List.class
                || Enum.class.isAssignableFrom(type);
    }
}
