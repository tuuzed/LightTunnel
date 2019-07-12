package com.tuuzed.tunnel.common.protocol;


import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class TunnelUri {


    public static TunnelUri create(@NotNull String uri) {
        URI uri0 = URI.create(uri);
        Map<String, String> queryMap = parseQuery(uri0);
        return new TunnelUri(uri0.getScheme(), uri0.getHost(), uri0.getPort(), queryMap);
    }

    public final String scheme;
    public final String host;
    public final int port;
    public final Map<String, String> queryMap;

    private TunnelUri(@NotNull String scheme, @NotNull String host, int port, @NotNull Map<String, String> queryMap) {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.queryMap = queryMap;
    }

    @Override
    public String toString() {
        StringBuilder query = new StringBuilder();
        Set<Map.Entry<String, String>> entries = queryMap.entrySet();
        boolean first = true;
        for (Map.Entry<String, String> entry : entries) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key != null) {
                query.append(key);
            }
            if (value != null) {
                query.append("=");
                query.append(value);
            }
            if (first) {
                query.append("&");
            }
            first = false;
        }
        return scheme + "://" + host + ":" + port + "?" + query.toString();
    }


    @NotNull
    private static Map<String, String> parseQuery(URI uri) {
        if (uri == null) {
            return Collections.emptyMap();
        }
        String queryString = uri.getQuery();
        if (queryString == null) {
            return Collections.emptyMap();
        }
        String[] kvLines = queryString.split("&");
        Map<String, String> map = new LinkedHashMap<>(kvLines.length);
        for (String line : kvLines) {
            String[] kvLine = line.split("=");
            if (kvLine.length == 1) {
                map.put(kvLine[0], null);
            } else if (kvLine.length == 2) {
                map.put(kvLine[0], kvLine[1]);
            }
        }
        return map;

    }
}
