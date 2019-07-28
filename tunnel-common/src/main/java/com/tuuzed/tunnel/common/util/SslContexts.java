package com.tuuzed.tunnel.common.util;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;

/**
 * 生成服务端证书
 * keytool -genkey -alias stunnelalias -keysize 2048 -validity 365 -keyalg RSA -dname "CN=tunnel" -keypass stunnelpass -storepass stunnelpass -keystore tunnel-server.jks
 * keytool -importkeystore -srckeystore tunnel-server.jks -destkeystore tunnel-server.jks -deststoretype pkcs12
 * keytool -export -alias stunnelalias -keystore tunnel-server.jks -storepass stunnelpass -file tunnel-server.cer
 * 生成客户端证书
 * keytool -genkey -alias ctunnelalias -keysize 2048 -validity 365 -keyalg RSA -dname "CN=tunnel" -keypass ctunnelpass -storepass ctunnelpass -keystore tunnel-client.jks
 * keytool -importkeystore -srckeystore tunnel-client.jks -destkeystore tunnel-client.jks -deststoretype pkcs12
 * keytool -import -trustcacerts -alias stunnalalias -file tunnel-server.cer -storepass ctunnelpass -keystore tunnel-client.jks
 */
public final class SslContexts {
    @NotNull
    public static SslContext forServer(
            @NotNull String jks,
            @NotNull String storepass,
            @NotNull String keypass
    ) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(jks), storepass.toCharArray());
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keyStore, keypass.toCharArray());
        return SslContextBuilder.forServer(keyManagerFactory).build();
    }

    @NotNull
    public static SslContext forClient(
            @NotNull String jks,
            @NotNull String storepass
    ) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(jks), storepass.toCharArray());
        TrustManagerFactory tf = TrustManagerFactory.getInstance("SunX509");
        tf.init(keyStore);
        return SslContextBuilder.forClient().trustManager(tf).build();
    }
}
