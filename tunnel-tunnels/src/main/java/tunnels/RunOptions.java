package tunnels;

import com.tuuzed.tunnelcli.LogLevel;
import com.tuuzed.tunnelcli.Option;

public final class RunOptions {

    @Option(name = "c", longName = "config-file", help = "配置文件，当设置了配置文件时优先使用配置文件配置项", order = -1)
    public String configFile = "";

    // =============================== common ================================== //
    @Option(name = "bt", longName = "boss-threads", help = "Boss线程数量", order = 1001)
    public int bossThreads = -1;

    @Option(name = "wt", longName = "workerThreads", help = "Worker线程数量", order = 1002)
    public int workerThreads = -1;

    @Option(name = "t", longName = "token", help = "令牌", order = 1003)
    public String token = "";

    @Option(name = "a", longName = "allow-ports", help = "端口白名单，例如：10000-21000,30000,30001,30003", order = 1004)
    public String allowPorts = "1024-65534";

    // =============================== log ================================== //
    @Option(name = "lv", longName = "log-level", help = "日志等级", order = 2001)
    public LogLevel logLevel = LogLevel.ALL;

    @Option(name = "lf", longName = "log-file", help = "日志文件", order = 2002)
    public String logFile = "./logs/tunnels.log";

    @Option(name = "lmfs", longName = "log-max-file-size", help = "日志文件大小", order = 2003)
    public String logMaxFileSize = "1MB";

    @Option(name = "lmbi", longName = "log-max-backup-index", help = "日志文件备份数量", order = 2004)
    public int logMaxBackupIndex = 3;

    // =============================== auth ================================== //
    @Option(name = "b", longName = "bind-addr", help = "绑定地址", order = 3001)
    public String bindAddr = "0.0.0.0";

    @Option(name = "p", longName = "bind-port", help = "绑定端口", order = 3002)
    public int bindPort = 5000;

    // =============================== ssl auth ================================== //
    @Option(name = "ssl", longName = "ssl", help = "是否启用SSL", order = 4001)
    public boolean sslEnable = false;

    @Option(name = "sb", longName = "ssl-bind-addr", help = "SSL绑定地址", order = 4002)
    public String sslBindAddr = "0.0.0.0";

    @Option(name = "sp", longName = "ssl-bind-port", help = "SSL绑定端口", order = 4003)
    public int sslBindPort = 5001;

    @Option(name = "sj", longName = "ssl-jks", help = "ssl jks签名文件", order = 4004)
    public String sslJks = "";

    @Option(name = "ss", longName = "ssl-storepass", help = "ssl jks签名文件Store密码", order = 4005)
    public String sslStorepass = "";

    @Option(name = "sk", longName = "ssl-keypass", help = "ssl jks签名文件Key密码", order = 4006)
    public String sslKeypass = "";

    // =============================== http ================================== //
    @Option(name = "http", longName = "http", help = "是否启用HTTP", order = 5001)
    public boolean httpEnable = false;

    @Option(name = "hb", longName = "http-bind-addr", help = "HTTP绑定地址", order = 5002)
    public String httpBindAddr = "0.0.0.0";

    @Option(name = "hp", longName = "http-bind-port", help = "HTTP绑定端口", order = 5003)
    public int httpBindPort = 80;


    // =============================== https ================================== //
    @Option(name = "https", longName = "https", help = "是否启用HTTPS", order = 6001)
    public boolean httpsEnable = false;

    @Option(name = "hsb", longName = "https-bind-addr", help = "HTTPS绑定地址", order = 6002)
    public String httpsBindAddr = "0.0.0.0";

    @Option(name = "hsp", longName = "https-bind-port", help = "HTTPS绑定端口", order = 6003)
    public int httpsBindPort = 443;

    @Option(name = "hsj", longName = "https-jks", help = "https jks签名文件", order = 6004)
    public String httpsJks = "";

    @Option(name = "hss", longName = "https-storepass", help = "https jks签名文件Store密码", order = 6005)
    public String httpsStorepass = "";

    @Option(name = "hsk", longName = "https-keypass", help = "https jks签名文件Key密码", order = 6006)
    public String httpsKeypass = "";


}
