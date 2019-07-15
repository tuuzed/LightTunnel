package com.tuuzed.tunnel.common.protocol;


import com.tuuzed.tunnel.common.TunnelProtocolException;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


public final class OpenTunnelRequest {

    public static final String TYPE_TCP = "tcp";

    public static OpenTunnelRequest fromBytes(@NotNull byte[] bytes) throws TunnelProtocolException {
        try {
            URI uri = URI.create(new String(bytes, StandardCharsets.UTF_8));
            int remotePort = Integer.parseInt(uri.getFragment());
            Map<String, String> queryMap = parseQuery(uri);
            return new OpenTunnelRequest(uri.getScheme(), uri.getHost(), uri.getPort(), remotePort, queryMap);
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
    public byte[] toBytes() {
        return toUri().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return toUri();
    }

    @NotNull
    private String toUri() {
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
