package com.tuuzed.tunnel.util;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;

public final class SslContexts {

    @NotNull
    public static SslContext forServer(
        @NotNull String jks,
        @NotNull String storepass,
        @NotNull String keypass
    ) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(jks), storepass.toCharArray());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keyStore, keypass.toCharArray());
        return SslContextBuilder.forServer(kmf).build();
    }

    @NotNull
    public static SslContext forClient(
        @NotNull String jks,
        @NotNull String storepass
    ) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(jks), storepass.toCharArray());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(keyStore);
        return SslContextBuilder.forClient().trustManager(tmf).build();
    }
}
