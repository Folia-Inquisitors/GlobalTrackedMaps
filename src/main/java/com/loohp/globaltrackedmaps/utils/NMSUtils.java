package com.loohp.globaltrackedmaps.utils;

import org.bukkit.Bukkit;

public class NMSUtils {

    public static final boolean IS_FOLIA = isFolia();
    public static final boolean IS_20_6 = isTwentyDotSix();

    public static Class<?> getNMSClass(String path, String... paths) throws ClassNotFoundException {
        String version = (isRelocated())
                ? ""
                : Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
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

    public static boolean isRelocated() {
        return IS_20_6 && !IS_FOLIA;
    }

    private static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
        } catch (ClassNotFoundException e) {
            return false;
        }
        return true;
    }

    private static boolean isTwentyDotSix() {
        String serverVersion = Bukkit.getVersion();
        return serverVersion.substring(serverVersion.indexOf(':') + 1).trim().replace(")", "").equals("1.20.6");
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
