package com.tuuzed.tunnel.cli.server;

import org.kohsuke.args4j.Option;

class RunOptions {
    @Option(name = "-c", aliases = {"--configFile"}, help = true, metaVar = "<string>", usage = "配置文件，当设置了配置文件时优先使用配置文件配置项")
    public String configFile = "";

    @Option(name = "-b", aliases = {"--bindAddr"}, help = true, metaVar = "<string>", usage = "绑定地址")
    public String bindAddr = "";

    @Option(name = "-p", aliases = {"--bindPort"}, help = true, metaVar = "<int>", usage = "绑定端口")
    public int bindPort = 5000;

    @Option(name = "-t", aliases = {"--token"}, help = true, metaVar = "<int>", usage = "令牌")
    public String token = "";

    @Option(name = "-a", aliases = {"--allowPorts"}, help = true, metaVar = "<string>", usage = "端口白名单，例如：10000-21000,30000,30001,30003")
    public String allowPorts = "1024-65534";

    @Option(name = "-bossThreads", aliases = {"--bossThreads"}, help = true, metaVar = "<int>", usage = "Boss线程数量")
    public int bossThreads = -1;

    @Option(name = "-workerThreads", aliases = {"--workerThreads"}, help = true, metaVar = "<int>", usage = "Worker线程数量")
    public int workerThreads = -1;

    // SSL
    @Option(name = "-ssl", aliases = {"--ssl"}, help = true, metaVar = "<boolean>", usage = "是否启用SSL")
    public boolean ssl = false;

    @Option(name = "-ssl-bindPort", aliases = {"--ssl-bindPort"}, help = true, metaVar = "<int>", usage = "SSL绑定端口")
    public int sslBindPort = 5001;

    @Option(name = "-ssl-jks", aliases = {"--ssl-jks"}, help = true, metaVar = "<string>", usage = "jks签名文件")
    public String sslJks = "";

    @Option(name = "-ssl-storepass", aliases = {"--ssl-storepass"}, help = true, metaVar = "<string>", usage = "jks签名文件Store密码")
    public String sslStorepass = "";

    @Option(name = "-ssl-keypass", aliases = {"--ssl-keypass"}, help = true, metaVar = "<string>", usage = "jks签名文件Key密码")
    public String sslKeypass = "";

}
