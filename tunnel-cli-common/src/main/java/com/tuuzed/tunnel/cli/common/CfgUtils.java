package com.tuuzed.tunnel.cli.common;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class CfgUtils {

    @NotNull
    public static String getString(@NotNull Map src, @NotNull String key, @NotNull String def) {
        Object o = src.get(key);
        if (o == null) {
            return def;
        }
        if (o instanceof String) {
            return (String) o;
        } else {
            return o.toString();
        }
    }

    @NotNull
    public static Map getMap(@NotNull Map src, @NotNull String key) {
        Object o = src.get(key);
        if (o instanceof Map) {
            return (Map) o;
        } else {
            return Collections.emptyMap();
        }
    }

    @NotNull
    public static List<Map> getListMap(@NotNull Map src, @NotNull String key) {
        Object o = src.get(key);
        if (o instanceof List) {
            return (List) o;
        } else {
            return Collections.emptyList();
        }
    }

    public static int getInt(@NotNull Map src, @NotNull String key, int def) {
        Object o = src.get(key);
        if (o == null) {
            return def;
        }
        try {
            return Integer.parseInt(src.get(key).toString());
        } catch (Exception e) {
            return def;
        }
    }

    public static boolean getBoolean(@NotNull Map src, @NotNull String key, boolean def) {
        Object o = src.get(key);
        if (o == null) {
            return def;
        }
        try {
            return Boolean.parseBoolean(src.get(key).toString());
        } catch (Exception e) {
            return def;
        }
    }
}
