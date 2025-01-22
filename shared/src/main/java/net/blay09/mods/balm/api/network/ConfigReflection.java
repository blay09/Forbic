package net.blay09.mods.balm.api.network;

import net.blay09.mods.balm.api.config.IgnoreConfig;
import net.blay09.mods.balm.api.config.Synced;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ConfigReflection {

    public static boolean isConfigDataField(Field field) {
        return !Modifier.isFinal(field.getModifiers())
                && !Modifier.isStatic(field.getModifiers())
                && field.getAnnotation(IgnoreConfig.class) == null;
    }

    public static List<Field> getAllFields(Class<?> clazz) {
        return Arrays.stream(clazz.getFields()).filter(ConfigReflection::isConfigDataField).toList();
    }

    public static List<Field> getSyncedFields(Class<?> clazz) {
        List<Field> syncedFields = new ArrayList<>();
        Field[] fields = clazz.getFields();
        for (Field field : fields) {
            if (isSyncedFieldOrObject(field) && isConfigDataField(field)) {
                syncedFields.add(field);
            }
        }
        return syncedFields;
    }

    public static boolean isSyncedFieldOrObject(Field field) {
        boolean hasSyncedAnnotation = field.getAnnotation(Synced.class) != null;
        boolean isObject = !field.getType().isPrimitive() && !field.getType().isEnum() && field.getType() != String.class && field.getType() != List.class;
        return hasSyncedAnnotation || isObject;
    }

    public static Object deepCopy(Object from, Object to) {
        Field[] fields = from.getClass().getFields();
        for (Field field : fields) {
            if (!isConfigDataField(field)) {
                continue;
            }

            Class<?> type = field.getType();
            try {
                if (String.class.isAssignableFrom(type) || Enum.class.isAssignableFrom(type) || type.isPrimitive()) {
                    field.set(to, field.get(from));
                } else if (List.class.isAssignableFrom(type)) {
                    field.set(to, new ArrayList((Collection) field.get(from)));
                } else {
                    field.set(to, deepCopy(field.get(from), field.get(to)));
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        return to;
    }

}
