package com.tuuzed.tunnel.common.proto;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class ProtoRequestBuilder {
    Proto proto;
    String localAddr;
    int localPort;
    Map<String, String> options;

    public ProtoRequestBuilder(@NotNull Proto proto) {
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
    ProtoRequestBuilder setOptionInternal(@NotNull String key, @NotNull String value) {
        if (options == null) {
            options = new HashMap<>();
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