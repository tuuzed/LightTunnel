package com.tuuzed.tunnel.cli.client;


import org.kohsuke.args4j.Option;

class RunOptions {

    @Option(name = "-c", aliases = {"--config-file"}, help = true, metaVar = "<string>", usage = "配置文件，当设置了配置文件时优先使用配置文件配置项")
    public String configFile = "";

    @Option(name = "-pt", aliases = {"--proto"}, help = true, metaVar = "<string>", usage = "协议, tcp|http")
    public String proto = "tcp";

    // ssl
    @Option(name = "-ssl", aliases = {"--ssl"}, help = true, metaVar = "<boolean>", usage = "是否启用SSL")
    public boolean ssl = false;

    @Option(name = "-sj", aliases = {"--ssl-jks"}, help = true, metaVar = "<string>", usage = "jks签名文件")
    public String sslJks = "";

    @Option(name = "-ss", aliases = {"--ssl-storepass"}, help = true, metaVar = "<string>", usage = "jks签名文件Store密码")
    public String sslStorepass = "";

    // tcp
    @Option(name = "-s", aliases = {"--server-addr"}, help = true, metaVar = "<string>", usage = "服务器地址")
    public String serverAddr = "127.0.0.1";

    @Option(name = "-p", aliases = {"--server-port"}, help = true, metaVar = "<int>", usage = "服务器端口")
    public int serverPort = 5000;

    @Option(name = "-la", aliases = {"--local-addr"}, help = true, metaVar = "<string>", usage = "内网地址")
    public String localAddr = "127.0.0.1";

    @Option(name = "-lp", aliases = {"--local-port"}, help = true, metaVar = "<int>", usage = "内网端口")
    public int localPort = 80;

    @Option(name = "-rp", aliases = {"--remote-port"}, help = true, metaVar = "<int>", usage = "映射外网端口")
    public int remotePort = 10080;

    @Option(name = "-t", aliases = {"--token"}, help = true, metaVar = "<int>", usage = "令牌")
    public String token = "";

    @Option(name = "-wt", aliases = {"--worker-threads"}, help = true, metaVar = "<int>", usage = "Worker线程数量")
    public int workerThreads = -1;

    // http
    @Option(name = "-vh", aliases = {"--vhost"}, help = true, metaVar = "<string>", usage = "域名")
    public String vhost = "";


}