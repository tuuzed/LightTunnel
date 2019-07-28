package com.tuuzed.tunnel.cli.server;

import org.kohsuke.args4j.Option;

class RunOptions {
    @Option(name = "-c", aliases = {"--config-file"}, help = true, metaVar = "<string>", usage = "配置文件，当设置了配置文件时优先使用配置文件配置项")
    public String configFile = "";

    @Option(name = "-b", aliases = {"--bind-addr"}, help = true, metaVar = "<string>", usage = "绑定地址")
    public String bindAddr = "0.0.0.0";

    @Option(name = "-p", aliases = {"--bind-port"}, help = true, metaVar = "<int>", usage = "绑定端口")
    public int bindPort = 5000;

    @Option(name = "-t", aliases = {"--token"}, help = true, metaVar = "<int>", usage = "令牌")
    public String token = "";

    @Option(name = "-a", aliases = {"--allow-ports"}, help = true, metaVar = "<string>", usage = "端口白名单，例如：10000-21000,30000,30001,30003")
    public String allowPorts = "1024-65534";

    @Option(name = "-bt", aliases = {"--boss-threads"}, help = true, metaVar = "<int>", usage = "Boss线程数量")
    public int bossThreads = -1;

    @Option(name = "-wt", aliases = {"--workerThreads"}, help = true, metaVar = "<int>", usage = "Worker线程数量")
    public int workerThreads = -1;

    // http
    @Option(name = "-http-b", aliases = {"--http-bind-addr"}, help = true, metaVar = "<string>", usage = "HTTP绑定地址")
    public String httpBindAddr = "0.0.0.0";

    @Option(name = "-http-p", aliases = {"--http-bind-port"}, help = true, metaVar = "<int>", usage = "HTTP绑定端口")
    public int httpBindPort = 5001;

    // ssl
    @Option(name = "-ssl", aliases = {"--ssl"}, help = true, metaVar = "<boolean>", usage = "是否启用SSL")
    public boolean ssl = false;

    @Option(name = "-ssl-b", aliases = {"--ssl-bind-addr"}, help = true, metaVar = "<string>", usage = "SSL绑定地址")
    public String sslBindAddr = "0.0.0.0";

    @Option(name = "-ssl-p", aliases = {"--ssl-bind-port"}, help = true, metaVar = "<int>", usage = "SSL绑定端口")
    public int sslBindPort = 5001;

    @Option(name = "-ssl-jks", aliases = {"--ssl-jks"}, help = true, metaVar = "<string>", usage = "jks签名文件")
    public String sslJks = "";

    @Option(name = "-ssl-storepass", aliases = {"--ssl-storepass"}, help = true, metaVar = "<string>", usage = "jks签名文件Store密码")
    public String sslStorepass = "";

    @Option(name = "-ssl-keypass", aliases = {"--ssl-keypass"}, help = true, metaVar = "<string>", usage = "jks签名文件Key密码")
    public String sslKeypass = "";

}
