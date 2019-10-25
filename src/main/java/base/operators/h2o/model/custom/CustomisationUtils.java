package base.operators.h2o.model.custom;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;

public class CustomisationUtils {
    public CustomisationUtils() {
    }

    public static <S extends Enum<S>, T extends Enum<T>> T convertEnum(S source, Class<T> targetClazz) {
        return Enum.valueOf(targetClazz, source.name());
    }

    public static <T> T[] convertArray(Object[] source, Class<T> targetClazz, Map<Class<?>, Class<?>> classConversions, Map<Class<? extends Enum<?>>, Class<? extends Enum<?>>> enumConversions, Collection<Class<?>> ignoredSuperClasses) throws IllegalAccessException, NoSuchFieldException, SecurityException, InstantiationException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
        T[] result = (T[])(Object[])Array.newInstance(targetClazz, source.length);

        for(int i = 0; i < source.length; ++i) {
            result[i] = convertObject(source[i], targetClazz, classConversions, enumConversions, ignoredSuperClasses);
        }

        return result;
    }

    public static <T> T convertObject(Object source, Class<T> targetClazz, Map<Class<?>, Class<?>> classConversions, Map<Class<? extends Enum<?>>, Class<? extends Enum<?>>> enumConversions, Collection<Class<?>> ignoredSuperClasses) throws IllegalAccessException, NoSuchFieldException, SecurityException, InstantiationException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
        Constructor<T> constructor = targetClazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        T target = constructor.newInstance();
        Class<?> sourceClazz = source.getClass();

        for(Class currentTargetClazz = targetClazz; sourceClazz != null && !ignoredSuperClasses.contains(sourceClazz); currentTargetClazz = currentTargetClazz.getSuperclass()) {
            Field[] var9 = sourceClazz.getDeclaredFields();
            int var10 = var9.length;

            for(int var11 = 0; var11 < var10; ++var11) {
                Field sourceField = var9[var11];
                if (!Modifier.isStatic(sourceField.getModifiers())) {
                    sourceField.setAccessible(true);
                    Object value = sourceField.get(source);
                    Field targetField = null;

                    try {
                        targetField = currentTargetClazz.getDeclaredField(sourceField.getName());
                    } catch (NoSuchFieldException var16) {
                        continue;
                    }

                    if (value != null) {
                        if (classConversions.containsKey(value.getClass())) {
                            value = convertObject(value, (Class)classConversions.get(value.getClass()), classConversions, enumConversions, ignoredSuperClasses);
                        }

                        if (value.getClass().isArray() && classConversions.containsKey(value.getClass().getComponentType())) {
                            value = convertArray((Object[])((Object[])value), (Class)classConversions.get(value.getClass().getComponentType()), classConversions, enumConversions, ignoredSuperClasses);
                        }

                        if (enumConversions.containsKey(value.getClass())) {
                            value = convertEnum((Enum)value, (Class)enumConversions.get(value.getClass()));
                        }
                    }

                    targetField.setAccessible(true);
                    targetField.set(target, value);
                }
            }

            sourceClazz = sourceClazz.getSuperclass();
        }

        return target;
    }
}