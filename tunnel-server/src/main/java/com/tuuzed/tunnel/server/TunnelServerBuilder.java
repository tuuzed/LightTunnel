package com.tuuzed.tunnel.server;

import com.tuuzed.tunnel.common.proto.ProtoRequestInterceptor;
import com.tuuzed.tunnel.server.http.HttpRequestInterceptor;
import io.netty.handler.ssl.SslContext;
import org.jetbrains.annotations.NotNull;

public class TunnelServerBuilder {

    int bossThreads = -1;
    int workerThreads = -1;

    String bindAddr = null;
    int bindPort = 5000;

    // ssl
    boolean enableSsl;
    SslContext sslContext;
    String sslBindAddr = null;
    int sslBindPort = 5001;
    // http
    String httpBindAddr = null;
    int httpBindPort = 5080;

    //
    ProtoRequestInterceptor protoRequestInterceptor = ProtoRequestInterceptor.DEFAULT;

    HttpRequestInterceptor httpRequestInterceptor = HttpRequestInterceptor.DEFAULT;

    TunnelServerBuilder() {
    }

    @NotNull
    public TunnelServerBuilder setBindAddr(String bindAddr) {
        this.bindAddr = bindAddr;
        return this;
    }

    @NotNull
    public TunnelServerBuilder setBindPort(int bindPort) {
        this.bindPort = bindPort;
        return this;
    }

    @NotNull
    public TunnelServerBuilder setBossThreads(int bossThreads) {
        this.bossThreads = bossThreads;
        return this;
    }

    @NotNull
    public TunnelServerBuilder setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
        return this;
    }


    @NotNull
    public TunnelServerBuilder enableSsl(@NotNull SslContext sslContext, int bindPort) {
        this.enableSsl = true;
        this.sslContext = sslContext;
        this.sslBindPort = bindPort;
        return this;
    }

    @NotNull
    public TunnelServerBuilder enableSsl(@NotNull SslContext sslContext, String bindAddr, int bindPort) {
        this.enableSsl = true;
        this.sslContext = sslContext;
        this.sslBindAddr = bindAddr;
        this.sslBindPort = bindPort;
        return this;
    }

    @NotNull
    public TunnelServerBuilder setHttpBindAddr(String bindAddr) {
        this.httpBindAddr = bindAddr;
        return this;
    }

    @NotNull
    public TunnelServerBuilder setHttpBindPort(int bindPort) {
        this.httpBindPort = bindPort;
        return this;
    }

    @NotNull
    public TunnelServerBuilder setProtoRequestInterceptor(@NotNull ProtoRequestInterceptor interceptor) {
        this.protoRequestInterceptor = interceptor;
        return this;
    }

    @NotNull
    public TunnelServerBuilder setHttpRequestInterceptor(@NotNull HttpRequestInterceptor interceptor) {
        this.httpRequestInterceptor = interceptor;
        return this;
    }

    @NotNull
    public TunnelServer build() {
        return new TunnelServer(this);
    }
}
