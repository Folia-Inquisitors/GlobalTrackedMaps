package com.loohp.globaltrackedmaps.utils;

import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NMSUtils {
    private static final Pattern VERSION_PATTERN = Pattern.compile("v\\d+_\\d+_R\\d+");
    private static final boolean IS_MOJMAP;
    private static final String PACKAGE_VERSION;

    static {
        Matcher matcher = VERSION_PATTERN.matcher(Bukkit.getServer().getClass().getPackage().getName());
        IS_MOJMAP = !matcher.find();
        PACKAGE_VERSION = IS_MOJMAP ? "" : matcher.group();
    }

    public static Class<?> getNMSClass(String... paths) throws ClassNotFoundException {
        if (paths.length == 0) {
            throw new IllegalArgumentException("Paths cannot be empty");
        }

        ClassNotFoundException error = null;
        for (String path : paths) {
            if (path.contains("%s")) {
                if (IS_MOJMAP) {
                    path = path.replace("%s", "").replace("..", ".");
                } else {
                    path = path.replace("%s", PACKAGE_VERSION);
                }
            }

            try {
                return Class.forName(path);
            } catch (ClassNotFoundException e) {
                error = e;
            }
        }
        throw error;
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
