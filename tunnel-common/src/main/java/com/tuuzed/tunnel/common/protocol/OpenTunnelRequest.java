package com.tuuzed.tunnel.common.protocol;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;


public final class OpenTunnelRequest {

    public static final String TYPE_TCP = "tcp";
    public static final String TYPE_HTTP = "http";
    public static final String REMOTE_PORT = "$rp";
    public static final String SUB_DOMAIN = "$sd";

    @NotNull
    private final String type;
    @NotNull
    private final String localAddr;
    private final int localPort;
    @NotNull
    private final Map<String, String> options;

    private OpenTunnelRequest(
            @NotNull String type,
            @NotNull String localAddr, int localPort,
            @NotNull Map<String, String> options
    ) {
        this.type = type;
        this.localAddr = localAddr;
        this.localPort = localPort;
        this.options = options;
    }

    @NotNull
    public String getType() {
        return type;
    }

    @NotNull
    public String getLocalAddr() {
        return localAddr;
    }

    public int getLocalPort() {
        return localPort;
    }

    public int getRemotePort() {
        String remotePort = options.get(REMOTE_PORT);
        if (remotePort == null) {
            throw new NullPointerException("remotePort == null");
        }
        return Integer.parseInt(remotePort);
    }

    @NotNull
    public String getSubDomain() {
        String subDomain = options.get(SUB_DOMAIN);
        if (subDomain == null) {
            throw new NullPointerException("subDomain == null");
        }
        return subDomain;
    }

    @Nullable
    public String getOption(@NotNull String key) {
        return options.get(key);
    }

    public boolean isTcp() {
        return TYPE_TCP.equals(type);
    }

    public boolean isHttp() {
        return TYPE_HTTP.equals(type);
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
    public Builder newTcpBuilder(int remotePort) {
        Builder builder = new Builder(TYPE_TCP);
        builder.localAddr = localAddr;
        builder.localPort = localPort;
        builder.options = options;
        builder.setOptionInternal(REMOTE_PORT, String.valueOf(remotePort));
        return builder;
    }

    @NotNull
    public Builder newHttpBuilder(@NotNull String subDomain) {
        Builder builder = new Builder(TYPE_HTTP);
        builder.localAddr = localAddr;
        builder.localPort = localPort;
        builder.options = options;
        builder.setOptionInternal(SUB_DOMAIN, subDomain);
        return builder;
    }

    @NotNull
    private String toUri() {
        StringBuilder query = new StringBuilder();
        if (options.isEmpty()) {
            return String.format("%s://%s:%d", type, localAddr, localPort);
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
        return String.format("%s://%s:%d?%s", type, localAddr, localPort, query.toString());
    }


    public static OpenTunnelRequest fromBytes(@NotNull byte[] bytes) throws TunnelProtocolException {
        try {
            URI uri = URI.create(new String(bytes, StandardCharsets.UTF_8));
            Map<String, String> queryMap = parseQuery(uri);
            return new OpenTunnelRequest(uri.getScheme(), uri.getHost(), uri.getPort(), queryMap);
        } catch (Exception e) {
            throw new TunnelProtocolException("OpenTunnelRequest bytes error", e);
        }

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


    @NotNull
    public static Builder tcpBuilder(int remotePort) {
        return new Builder(TYPE_TCP).setOptionInternal(REMOTE_PORT, String.valueOf(remotePort));
    }


    @NotNull
    public static Builder httpBuilder(@NotNull String subDomain) {
        return new Builder(TYPE_HTTP).setOptionInternal(SUB_DOMAIN, subDomain);
    }


    public static class Builder {
        private String type;
        private String localAddr;
        private int localPort;
        private Map<String, String> options;

        private Builder(String type) {
            this.type = type;
        }

        @NotNull
        public Builder setLocalAddr(@NotNull String localAddr) {
            this.localAddr = localAddr;
            return this;
        }

        @NotNull
        public Builder setLocalPort(int localPort) {
            this.localPort = localPort;
            return this;
        }

        @NotNull
        private Builder setOptionInternal(@NotNull String key, @NotNull String value) {
            if (options == null) {
                options = new HashMap<>();
            }
            options.put(key, value);
            return this;
        }

        @NotNull
        public Builder setOption(@NotNull String key, @NotNull String value) {
            if (key.startsWith("$")) {
                throw new IllegalArgumentException("Key with the prefix '$' is internally reserved");
            }
            setOptionInternal(key, value);
            return this;
        }

        @NotNull
        public OpenTunnelRequest build() {
            if (localAddr == null) {
                throw new IllegalArgumentException("localAddr == null");
            }
            if (localPort < 0 || localPort > 65535) {
                throw new IllegalArgumentException("localPort range error");
            }
            if (options == null) {
                options = Collections.emptyMap();
            }
            if (TYPE_TCP.equals(type)) {
                if (!options.containsKey(REMOTE_PORT)) {
                    throw new IllegalArgumentException("Not " + REMOTE_PORT + " option");
                }
            } else if (TYPE_HTTP.equals(type)) {
                if (!options.containsKey(SUB_DOMAIN)) {
                    throw new IllegalArgumentException("Not " + SUB_DOMAIN + " option");
                }
            }
            return new OpenTunnelRequest(type, localAddr, localPort, options);
        }

    }


}
