package com.tuuzed.tunnel.cli.server;

import com.tuuzed.tunnel.cli.common.Option;

public final class RunOptions {

    @Option(name = "c", longName = "config-file", help = "配置文件，当设置了配置文件时优先使用配置文件配置项", order = 10)
    public String configFile = "";

    @Option(name = "bt", longName = "boss-threads", help = "Boss线程数量", order = 20)
    public int bossThreads = -1;

    @Option(name = "wt", longName = "workerThreads", help = "Worker线程数量", order = 30)
    public int workerThreads = -1;

    @Option(name = "b", longName = "bind-addr", help = "绑定地址", order = 40)
    public String bindAddr = "0.0.0.0";

    @Option(name = "p", longName = "bind-port", help = "绑定端口", order = 50)
    public int bindPort = 5000;

    @Option(name = "t", longName = "token", help = "令牌", order = 60)
    public String token = "";

    @Option(name = "a", longName = "allow-ports", help = "端口白名单，例如：10000-21000,30000,30001,30003", order = 70)
    public String allowPorts = "1024-65534";

    // http
    @Option(name = "hb", longName = "http-bind-addr", help = "HTTP绑定地址", order = 80)
    public String httpBindAddr = "0.0.0.0";

    @Option(name = "hp", longName = "http-bind-port", help = "HTTP绑定端口", order = 90)
    public int httpBindPort = 5001;

    // ssl
    @Option(name = "ssl", longName = "ssl", help = "是否启用SSL", order = 100)
    public boolean ssl = false;

    @Option(name = "sb", longName = "ssl-bind-addr", help = "SSL绑定地址", order = 110)
    public String sslBindAddr = "0.0.0.0";

    @Option(name = "sp", longName = "ssl-bind-port", help = "SSL绑定端口", order = 120)
    public int sslBindPort = 5001;

    @Option(name = "sj", longName = "ssl-jks", help = "jks签名文件", order = 130)
    public String sslJks = "";

    @Option(name = "ss", longName = "ssl-storepass", help = "jks签名文件Store密码", order = 140)
    public String sslStorepass = "";

    @Option(name = "sk", longName = "ssl-keypass", help = "jks签名文件Key密码", order = 150)
    public String sslKeypass = "";

}
