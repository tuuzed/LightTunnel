package com.tuuzed.tunnel.common.proto.internal;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class ProtoUtils {
    @NotNull
    public static String map2String(Map<String, String> originalMap) {
        if (originalMap == null) {
            return "";
        }
        StringBuilder line = new StringBuilder();
        Set<Map.Entry<String, String>> entries = originalMap.entrySet();
        boolean first = true;
        for (Map.Entry<String, String> entry : entries) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key != null) {
                if (!first) {
                    line.append("&");
                }
                line.append(key);
                if (value != null) {
                    line.append("=");
                    line.append(value);
                }
            }
            first = false;
        }
        return line.toString();
    }

    @NotNull
    public static Map<String, String> string2Map(String originalLine) {
        if (originalLine == null) {
            return Collections.emptyMap();
        }
        String[] kvLines = originalLine.split("&");
        Map<String, String> map = new LinkedHashMap<>(kvLines.length);
        for (String it : kvLines) {
            String[] kvLine = it.split("=");
            if (kvLine.length == 1) {
                map.put(kvLine[0], null);
            } else if (kvLine.length == 2) {
                map.put(kvLine[0], kvLine[1]);
            }
        }
        return map;
    }

}
