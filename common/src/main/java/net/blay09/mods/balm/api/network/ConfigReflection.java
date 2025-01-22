package net.blay09.mods.balm.api.network;

import net.blay09.mods.balm.api.config.Synced;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class ConfigReflection {

    public static List<Field> getAllFields(Class<?> clazz) {
        return Arrays.stream(clazz.getFields()).filter(it -> !Modifier.isFinal(it.getModifiers()) && !Modifier.isStatic(it.getModifiers())).toList();
    }

    public static List<Field> getSyncedFields(Class<?> clazz) {
        List<Field> syncedFields = new ArrayList<>();
        Field[] fields = clazz.getFields();
        for (Field field : fields) {
            if (isSyncedFieldOrObject(field)) {
                syncedFields.add(field);
            }
        }
        return syncedFields;
    }

    public static boolean isSyncedFieldOrObject(Field field) {
        boolean hasSyncedAnnotation = field.getAnnotation(Synced.class) != null;
        boolean isObject = !field.getType().isPrimitive() && !field.getType().isEnum() && field.getType() != String.class && field.getType() != List.class && field.getType() != Set.class && field.getType() != ResourceLocation.class;
        return hasSyncedAnnotation || isObject;
    }

    public static Object deepCopy(Object from, Object to) {
        Field[] fields = from.getClass().getFields();
        for (Field field : fields) {
            if (Modifier.isFinal(field.getModifiers())) {
                continue;
            }

            Class<?> type = field.getType();
            try {
                if (String.class.isAssignableFrom(type) || ResourceLocation.class.isAssignableFrom(type) || Enum.class.isAssignableFrom(type) || type.isPrimitive()) {
                    field.set(to, field.get(from));
                } else if (List.class.isAssignableFrom(type)) {
                    field.set(to, new ArrayList((Collection) field.get(from)));
                } else if (Set.class.isAssignableFrom(type)) {
                    field.set(to, new HashSet(((Collection) field.get(from))));
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
