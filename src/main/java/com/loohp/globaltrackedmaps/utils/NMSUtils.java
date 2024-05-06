package com.loohp.globaltrackedmaps.utils;

import org.bukkit.Bukkit;

public class NMSUtils {
    public static Class<?> getNMSClass(String path, String... paths) throws ClassNotFoundException {
        String version = Bukkit.getServer().getClass().getPackage().getName().substring(Bukkit.getServer().getClass().getPackage().getName().lastIndexOf('.') + 1);
        ClassNotFoundException error = null;
        try {
            return Class.forName(path.replace("%s", version));
        } catch (ClassNotFoundException e) {
            error = e;
            for (String classpath : paths) {
                try {
                    return Class.forName(classpath.replace("%s", version));
                } catch (ClassNotFoundException classNotFoundException) {
                    error = classNotFoundException;
                }
            }
            throw error;
        }
    }

    @SafeVarargs
    public static <T extends java.lang.reflect.AccessibleObject> T reflectiveLookup(Class<T> lookupType, ReflectionLookupSupplier<T> methodLookup, ReflectionLookupSupplier<T>... methodLookups) throws ReflectiveOperationException {
        ReflectiveOperationException error = null;
        try {
            return methodLookup.lookup();
        } catch (ReflectiveOperationException e) {
            error = e;
            for (ReflectionLookupSupplier<T> supplier : methodLookups) {
                try {
                    return supplier.lookup();
                } catch (ReflectiveOperationException reflectiveOperationException) {
                    error = reflectiveOperationException;
                }
            }
            throw error;
        }
    }

    @FunctionalInterface
    public interface ReflectionLookupSupplier<T> {
        T lookup() throws ReflectiveOperationException;
    }
}
