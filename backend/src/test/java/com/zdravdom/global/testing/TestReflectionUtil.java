package com.zdravdom.global.testing;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/**
 * Reflection-based test utilities for instantiating JPA entities
 * with protected no-args constructors (e.g. domain objects used in tests).
 */
public final class TestReflectionUtil {

    private static final UnsafeGetter UNSAFE;

    static {
        UnsafeGetter u;
        try {
            u = new UnsafeGetter();
        } catch (Exception e) {
            u = null;
        }
        UNSAFE = u;
    }

    private TestReflectionUtil() {}

    /**
     * Create an instance bypassing protected/private constructor.
     * Falls back to Unsafe.allocateInstance for JDK 9+.
     */
    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Class<T> clazz) {
        if (UNSAFE != null) {
            return (T) UNSAFE.allocateInstance(clazz);
        }
        // Fallback: try setAccessible on constructor
        try {
            Constructor<T> c = clazz.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Cannot instantiate " + clazz.getSimpleName() + " — no Unsafe and constructor is inaccessible", e);
        }
    }

    /**
     * Set a private final field via reflection.
     * Used for test setup of entity IDs that are normally auto-generated.
     */
    public static void setId(Object entity, Long id) {
        setField(entity, "id", id);
    }

    /**
     * Set any private field via reflection.
     */
    public static void setField(Object entity, String fieldName, Object value) {
        try {
            Field field = findField(entity.getClass(), fieldName);
            field.setAccessible(true);
            field.set(entity, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set " + fieldName + " on " + entity.getClass().getSimpleName(), e);
        }
    }

    private static Field findField(Class<?> clazz, String name) {
        while (clazz != null) {
            for (Field f : clazz.getDeclaredFields()) {
                if (f.getName().equals(name)) return f;
            }
            clazz = clazz.getSuperclass();
        }
        throw new RuntimeException("Field not found: " + name);
    }

    /**
     * Unsafe-based instance allocator for bypassing constructors (JDK 9+).
     */
    private static final class UnsafeGetter {
        private final Object theUnsafe;
        private final java.lang.reflect.Method allocateInstance;

        UnsafeGetter() throws Exception {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            java.lang.reflect.Field f = unsafeClass.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            theUnsafe = f.get(null);
            allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
        }

        Object allocateInstance(Class<?> clazz) {
            try {
                return allocateInstance.invoke(theUnsafe, clazz);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
