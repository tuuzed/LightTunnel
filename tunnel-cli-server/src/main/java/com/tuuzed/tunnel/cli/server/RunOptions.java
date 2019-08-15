package com.tuuzed.tunnel.cli.server;

import com.tuuzed.tunnel.cli.common.Option;

public final class RunOptions {

    @Option(name = "c", longName = "config-file", help = "配置文件，当设置了配置文件时优先使用配置文件配置项", order = -1)
    public String configFile = "";

    // =============================== common ================================== //
    @Option(name = "bt", longName = "boss-threads", help = "Boss线程数量", order = 10)
    public int bossThreads = -1;

    @Option(name = "wt", longName = "workerThreads", help = "Worker线程数量", order = 20)
    public int workerThreads = -1;

    @Option(name = "t", longName = "token", help = "令牌", order = 30)
    public String token = "";

    @Option(name = "a", longName = "allow-ports", help = "端口白名单，例如：10000-21000,30000,30001,30003", order = 40)
    public String allowPorts = "1024-65534";

    // =============================== auth ================================== //
    @Option(name = "b", longName = "bind-addr", help = "绑定地址", order = 50)
    public String bindAddr = "0.0.0.0";

    @Option(name = "p", longName = "bind-port", help = "绑定端口", order = 60)
    public int bindPort = 5000;

    // =============================== ssl auth ================================== //
    @Option(name = "ssl", longName = "ssl", help = "是否启用SSL", order = 70)
    public boolean sslEnable = false;

    @Option(name = "sb", longName = "ssl-bind-addr", help = "SSL绑定地址", order = 80)
    public String sslBindAddr = "0.0.0.0";

    @Option(name = "sp", longName = "ssl-bind-port", help = "SSL绑定端口", order = 90)
    public int sslBindPort = 5001;

    @Option(name = "sj", longName = "ssl-jks", help = "ssl jks签名文件", order = 100)
    public String sslJks = "";

    @Option(name = "ss", longName = "ssl-storepass", help = "ssl jks签名文件Store密码", order = 110)
    public String sslStorepass = "";

    @Option(name = "sk", longName = "ssl-keypass", help = "ssl jks签名文件Key密码", order = 120)
    public String sslKeypass = "";

    // =============================== http ================================== //
    @Option(name = "http", longName = "http", help = "是否启用HTTP", order = 130)
    public boolean httpEnable = false;

    @Option(name = "hb", longName = "http-bind-addr", help = "HTTP绑定地址", order = 140)
    public String httpBindAddr = "0.0.0.0";

    @Option(name = "hp", longName = "http-bind-port", help = "HTTP绑定端口", order = 150)
    public int httpBindPort = 5001;


    // =============================== https ================================== //
    @Option(name = "https", longName = "https", help = "是否启用HTTPS", order = 160)
    public boolean httpsEnable = false;

    @Option(name = "hsb", longName = "https-bind-addr", help = "HTTPS绑定地址", order = 170)
    public String httpsBindAddr = "0.0.0.0";

    @Option(name = "hsp", longName = "https-bind-port", help = "HTTPS绑定端口", order = 180)
    public int httpsBindPort = 5001;

    @Option(name = "hsj", longName = "https-jks", help = "https jks签名文件", order = 190)
    public String httpsJks = "";

    @Option(name = "hss", longName = "https-storepass", help = "https jks签名文件Store密码", order = 200)
    public String httpsStorepass = "";

    @Option(name = "hsk", longName = "https-keypass", help = "https jks签名文件Key密码", order = 210)
    public String httpsKeypass = "";


}
