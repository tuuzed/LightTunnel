package com.tuuzed.tunnel.common.protocol;


import com.tuuzed.tunnel.common.TunnelProtocolException;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class OpenTunnelRequest {

    public static final String TYPE_TCP = "tcp";

    public static OpenTunnelRequest create(@NotNull String uri) throws TunnelProtocolException {
        try {
            URI uri0 = URI.create(uri);
            int remotePort = Integer.parseInt(uri0.getFragment());
            Map<String, String> queryMap = parseQuery(uri0);
            return new OpenTunnelRequest(uri0.getScheme(), uri0.getHost(), uri0.getPort(), remotePort, queryMap);
        } catch (Exception e) {
            throw new TunnelProtocolException("OpenTunnelRequest uri error", e);
        }

    }

    public final String type;
    public final String localAddr;
    public final int localPort;
    public final int remotePort;
    public final Map<String, String> arguments;

    public OpenTunnelRequest(@NotNull String type,
                             @NotNull String localAddr, int localPort,
                             int remotePort,
                             @NotNull Map<String, String> arguments) {
        this.type = type;
        this.localAddr = localAddr;
        this.localPort = localPort;
        this.remotePort = remotePort;
        this.arguments = arguments;
    }

    @NotNull
    public String toUri() {
        StringBuilder query = new StringBuilder();
        if (arguments.isEmpty()) {
            return String.format("%s://%s:%d#%d", type, localAddr, localPort, remotePort);
        }
        Set<Map.Entry<String, String>> entries = arguments.entrySet();
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
        return String.format("%s://%s:%d?%s#%d", type, localAddr, localPort, query.toString(), remotePort);
    }

    @Override
    public String toString() {
        return toUri();
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
