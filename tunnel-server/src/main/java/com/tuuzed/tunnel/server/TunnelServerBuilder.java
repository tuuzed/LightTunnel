package com.tuuzed.tunnel.server;

import com.tuuzed.tunnel.common.interceptor.HttpRequestInterceptor;
import com.tuuzed.tunnel.common.interceptor.ProtoRequestInterceptor;
import io.netty.handler.ssl.SslContext;
import org.jetbrains.annotations.NotNull;

public class TunnelServerBuilder {

    // common
    /* package */ int bossThreads = -1;
    /* package */ int workerThreads = -1;

    // auth
    /* package */ String bindAddr = null;
    /* package */ int bindPort = 5000;
    /* package */ ProtoRequestInterceptor protoRequestInterceptor = ProtoRequestInterceptor.DEFAULT;

    // ssl auth
    /* package */ boolean sslEnable;
    /* package */ SslContext sslContext;
    /* package */ String sslBindAddr = null;
    /* package */ int sslBindPort = 5001;

    // http
    /* package */ boolean httpEnable;
    /* package */ String httpBindAddr = null;
    /* package */ int httpBindPort = 5080;
    /* package */ HttpRequestInterceptor httpRequestInterceptor = HttpRequestInterceptor.DEFAULT;

    // https
    /* package */ boolean httpsEnable;
    /* package */ SslContext httpsContext;
    /* package */ String httpsBindAddr = null;
    /* package */ int httpsBindPort = 5443;
    /* package */ HttpRequestInterceptor httpsRequestInterceptor = HttpRequestInterceptor.DEFAULT;


    /* package */ TunnelServerBuilder() {
    }

    // ============================= common =============================== //
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

    // ============================= tcp auth =============================== //
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
    public TunnelServerBuilder setProtoRequestInterceptor(@NotNull ProtoRequestInterceptor interceptor) {
        this.protoRequestInterceptor = interceptor;
        return this;
    }


    // ============================= ssl tcp auth =============================== //

    @NotNull
    public TunnelServerBuilder setSslEnable(boolean enable) {
        this.sslEnable = enable;
        return this;
    }

    @NotNull
    public TunnelServerBuilder setSslContext(SslContext context) {
        this.sslContext = context;
        return this;
    }

    @NotNull
    public TunnelServerBuilder setSslBindAddr(String bindAddr) {
        this.sslBindAddr = bindAddr;
        return this;
    }

    @NotNull
    public TunnelServerBuilder setSslBindPort(int bindPort) {
        this.sslBindPort = bindPort;
        return this;
    }

    // ============================= http =============================== //

    @NotNull
    public TunnelServerBuilder setHttpEnable(boolean enable) {
        this.httpEnable = enable;
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
    public TunnelServerBuilder setHttpRequestInterceptor(@NotNull HttpRequestInterceptor interceptor) {
        this.httpRequestInterceptor = interceptor;
        return this;
    }

    // ============================= https =============================== //
    @NotNull
    public TunnelServerBuilder setHttpsEnable(boolean enable) {
        this.httpsEnable = enable;
        return this;
    }

    @NotNull
    public TunnelServerBuilder setHttpsContext(SslContext context) {
        this.httpsContext = context;
        return this;
    }

    @NotNull
    public TunnelServerBuilder setHttpsBindAddr(String bindAddr) {
        this.httpsBindAddr = bindAddr;
        return this;
    }

    @NotNull
    public TunnelServerBuilder setHttpsBindPort(int bindPort) {
        this.httpsBindPort = bindPort;
        return this;
    }

    @NotNull
    public TunnelServerBuilder setHttpsRequestInterceptor(@NotNull HttpRequestInterceptor interceptor) {
        this.httpsRequestInterceptor = interceptor;
        return this;
    }

    @NotNull
    public TunnelServer build() {
        return new TunnelServer(this);
    }
}
