package com.tuuzed.tunnel.common.protocol;


import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class OpenTunnelRequest {

    public static final String SCHEME_TCP = "tcp";

    public static OpenTunnelRequest create(@NotNull String uri) {
        URI uri0 = URI.create(uri);
        int remotePort = Integer.parseInt(uri0.getFragment());
        Map<String, String> queryMap = parseQuery(uri0);
        return new OpenTunnelRequest(uri0.getScheme(), uri0.getHost(), uri0.getPort(), remotePort, queryMap);
    }

    public final String scheme;
    public final String localAddr;
    public final int localPort;
    public final int remotePort;
    public final Map<String, String> options;

    public OpenTunnelRequest(@NotNull String scheme,
                             @NotNull String localAddr, int localPort,
                             int remotePort,
                             @NotNull Map<String, String> options) {
        this.scheme = scheme;
        this.localAddr = localAddr;
        this.localPort = localPort;
        this.remotePort = remotePort;
        this.options = options;
    }

    @Override
    public String toString() {
        StringBuilder query = new StringBuilder();
        if (options.isEmpty()) {
            return String.format("%s://%s:%d#%d", scheme, localAddr, localPort, remotePort);
        }
        Set<Map.Entry<String, String>> entries = options.entrySet();
        boolean first = true;
        for (Map.Entry<String, String> entry : entries) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (key != null) {
                if (!first) {
                    query.append("&");
                }
                query.append(key);
                if (value != null) {
                    query.append("=");
                    query.append(value);
                }
            }


            first = false;
        }
        return String.format("%s://%s:%d?%s#%d", scheme, localAddr, localPort, query.toString(), remotePort);
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
