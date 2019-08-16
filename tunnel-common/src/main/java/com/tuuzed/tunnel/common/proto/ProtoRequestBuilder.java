package com.tuuzed.tunnel.common.proto;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


public class ProtoRequestBuilder {
    Proto proto;
    String localAddr;
    int localPort;
    Map<String, String> options;

    ProtoRequestBuilder(@NotNull Proto proto) {
        this.proto = proto;
    }

    @NotNull
    public ProtoRequestBuilder setLocalAddr(@NotNull String localAddr) {
        this.localAddr = localAddr;
        return this;
    }

    @NotNull
    public ProtoRequestBuilder setLocalPort(int localPort) {
        this.localPort = localPort;
        return this;
    }

    @NotNull
    public ProtoRequestBuilder setToken(@NotNull String token) {
        setOptionInternal(ProtoRequest.TOKEN, token);
        return this;
    }

    @NotNull
    public ProtoRequestBuilder setBasicAuth(boolean enable) {
        setBasicAuth(enable, ".");
        return this;
    }

    @NotNull
    public ProtoRequestBuilder setBasicAuth(boolean enable, @NotNull String realm) {
        setOptionInternal(ProtoRequest.BASIC_AUTH, enable ? "1" : "0");
        setOptionInternal(ProtoRequest.BASIC_AUTH_REALM, realm);
        return this;
    }

    @NotNull
    public ProtoRequestBuilder setBasicAuthAccount(@NotNull String username, @NotNull String password) {
        setOptionInternal(ProtoRequest.BASIC_AUTH_USERNAME, username);
        setOptionInternal(ProtoRequest.BASIC_AUTH_PASSWORD, password);
        return this;
    }

    @NotNull
    public ProtoRequestBuilder setAddHeaders(@NotNull Map<String, String> headers) {
        setHeadersOption(ProtoRequest.ADD_HEADERS, headers);
        return this;
    }

    @NotNull
    public ProtoRequestBuilder setSetHeaders(@NotNull Map<String, String> headers) {
        setHeadersOption(ProtoRequest.SET_HEADERS, headers);
        return this;
    }


    private void setHeadersOption(@NotNull String option, @NotNull Map<String, String> headers) {
        boolean isFirst = true;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> it : headers.entrySet()) {
            if (!isFirst) {
                sb.append(";");
            }
            sb.append(it.getKey()).append(":").append(it.getValue());
            isFirst = false;
        }
        setOptionInternal(option, sb.toString());
    }


    @NotNull
    ProtoRequestBuilder setOptionInternal(@NotNull String key, @NotNull String value) {
        if (options == null) {
            options = new LinkedHashMap<>();
        }
        options.put(key, value);
        return this;
    }

    @NotNull
    public ProtoRequestBuilder setOption(@NotNull String key, @NotNull String value) {
        if (key.startsWith("$")) {
            throw new IllegalArgumentException("$打头的key为系统保留的key");
        }
        setOptionInternal(key, value);
        return this;
    }


    @NotNull
    public ProtoRequest build() {
        if (localAddr == null) {
            throw new IllegalArgumentException("localAddr == null");
        }
        if (localPort < 0 || localPort > 65535) {
            throw new IllegalArgumentException("localPort < 0 || localPort > 65535");
        }
        if (options == null) {
            options = Collections.emptyMap();
        }
        switch (proto) {
            case UNKNOWN:
                break;
            case TCP:
                if (!options.containsKey(ProtoRequest.REMOTE_PORT)) {
                    throw new IllegalArgumentException("TCP协议必须设置REMOTE_PORT");
                }
                break;
            case HTTP:
                if (!options.containsKey(ProtoRequest.VHOST)) {
                    throw new IllegalArgumentException("HTTP协议必须设置VHOST");
                }
                break;
            default:
                break;
        }
        return new ProtoRequest(proto, localAddr, localPort, options);
    }

}