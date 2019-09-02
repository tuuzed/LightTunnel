package tunnel2.t2s


import tunnel2.t2cli.LogLevel
import tunnel2.t2cli.Option

class RunOptions {

    @Option(name = "c", longName = "config-file", help = "配置文件，当设置了配置文件时优先使用配置文件配置项", order = -1)
    var configFile = ""

    // =============================== common ================================== //
    @Option(name = "bt", longName = "boss-threads", help = "Boss线程数量", order = 1001)
    var bossThreads = -1

    @Option(name = "wt", longName = "worker-threads", help = "Worker线程数量", order = 1002)
    var workerThreads = -1

    @Option(name = "at", longName = "auth-token", help = "令牌", order = 1003)
    var authToken = ""

    @Option(name = "ap", longName = "allow-ports", help = "端口白名单，例如：10000-21000,30000,30001,30003", order = 1004)
    var allowPorts = "1024-65534"

    // =============================== log ================================== //
    @Option(name = "lv", longName = "log-level", help = "日志等级", order = 2001)
    var logLevel = LogLevel.ALL

    @Option(name = "lf", longName = "log-file", help = "日志文件", order = 2002)
    var logFile = "./logs/t2s.log"

    @Option(name = "lmfs", longName = "log-max-file-size", help = "日志文件大小", order = 2003)
    var logMaxFileSize = "1MB"

    @Option(name = "lmbi", longName = "log-max-backup-index", help = "日志文件备份数量", order = 2004)
    var logMaxBackupIndex = 3

    // =============================== auth ================================== //
    @Option(name = "b", longName = "bind-addr", help = "绑定地址", order = 3001)
    var bindAddr = "0.0.0.0"

    @Option(name = "p", longName = "bind-port", help = "绑定端口", order = 3002)
    var bindPort = 5000

    // =============================== ssl auth ================================== //
    @Option(name = "ssl", longName = "ssl", help = "是否启用SSL", order = 4001)
    var sslEnable = false

    @Option(name = "sb", longName = "ssl-bind-addr", help = "SSL绑定地址", order = 4002)
    var sslBindAddr = "0.0.0.0"

    @Option(name = "sp", longName = "ssl-bind-port", help = "SSL绑定端口", order = 4003)
    var sslBindPort = 5001

    @Option(name = "sj", longName = "ssl-jks", help = "ssl jks签名文件", order = 4004)
    var sslJks = ""

    @Option(name = "ss", longName = "ssl-storepass", help = "ssl jks签名文件Store密码", order = 4005)
    var sslStorepass = ""

    @Option(name = "sk", longName = "ssl-keypass", help = "ssl jks签名文件Key密码", order = 4006)
    var sslKeypass = ""

    // =============================== http ================================== //
    @Option(name = "http", longName = "http", help = "是否启用HTTP", order = 5001)
    var httpEnable = false

    @Option(name = "hb", longName = "http-bind-addr", help = "HTTP绑定地址", order = 5002)
    var httpBindAddr = "0.0.0.0"

    @Option(name = "hp", longName = "http-bind-port", help = "HTTP绑定端口", order = 5003)
    var httpBindPort = 80


    // =============================== https ================================== //
    @Option(name = "https", longName = "https", help = "是否启用HTTPS", order = 6001)
    var httpsEnable = false

    @Option(name = "hsb", longName = "https-bind-addr", help = "HTTPS绑定地址", order = 6002)
    var httpsBindAddr = "0.0.0.0"

    @Option(name = "hsp", longName = "https-bind-port", help = "HTTPS绑定端口", order = 6003)
    var httpsBindPort = 443

    @Option(name = "hsj", longName = "https-jks", help = "https jks签名文件", order = 6004)
    var httpsJks = ""

    @Option(name = "hss", longName = "https-storepass", help = "https jks签名文件Store密码", order = 6005)
    var httpsStorepass = ""

    @Option(name = "hsk", longName = "https-keypass", help = "https jks签名文件Key密码", order = 6006)
    var httpsKeypass = ""


}
