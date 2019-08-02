package com.tuuzed.tunnel.cli.client;

import com.tuuzed.tunnel.cli.common.Option;

public final class RunOptions {

    @Option(name = "c", longName = "config-file", help = "配置文件，当设置了配置文件时优先使用配置文件配置项", order = 10)
    public String configFile = "";

    @Option(name = "pt", longName = "proto", help = "协议", order = 20)
    public String proto = "tcp";

    @Option(name = "wt", longName = "worker-threads", help = "Worker线程数量", order = 30)
    public int workerThreads = -1;

    // ssl
    @Option(name = "ssl", longName = "ssl", help = "是否启用SSL", order = 40)
    public boolean ssl = false;

    @Option(name = "sj", longName = "ssl-jks", help = "jks签名文件", order = 50)
    public String sslJks = "";

    @Option(name = "ss", longName = "ssl-storepass", help = "jks签名文件Store密码", order = 60)
    public String sslStorepass = "";

    // tcp
    @Option(name = "s", longName = "server-addr", help = "服务器地址", order = 70)
    public String serverAddr = "127.0.0.1";

    @Option(name = "p", longName = "server-port", help = "服务器端口", order = 80)
    public int serverPort = 5000;

    @Option(name = "la", longName = "local-addr", help = "内网地址", order = 90)
    public String localAddr = "127.0.0.1";

    @Option(name = "lp", longName = "local-port", help = "内网端口", order = 100)
    public int localPort = 80;

    @Option(name = "rp", longName = "remote-port", help = "映射外网端口", order = 110)
    public int remotePort = 10080;

    @Option(name = "t", longName = "token", help = "令牌", order = 120)
    public String token = "";

    // http
    @Option(name = "vh", longName = "vhost", help = "域名", order = 130)
    public String vhost = "";


}