package tunnel2.t2c

import tunnel2.common.TunnelType
import tunnel2.t2cli.LogLevel
import tunnel2.t2cli.Option

class RunOptions {

    @Option(name = "c", longName = "config-file", help = "配置文件，当设置了配置文件时优先使用配置文件配置项", order = -1)
    var configFile = ""

    // =============================== common ================================== //
    @Option(name = "wt", longName = "worker-threads", help = "Worker线程数量", order = 1001)
    var workerThreads = -1

    @Option(name = "tp", longName = "type", help = "隧道类型", excludeEnums = ["UNKNOWN", "UDP"], order = 1002)
    var type = TunnelType.TCP

    @Option(name = "s", longName = "server-addr", help = "服务器地址", order = 1003)
    var serverAddr = "127.0.0.1"

    @Option(name = "p", longName = "server-port", help = "服务器端口", order = 1004)
    var serverPort = 5000

    @Option(name = "at", longName = "auth-token", help = "令牌", order = 1005)
    var authToken = ""

    @Option(name = "ssl", longName = "ssl", help = "是否启用SSL", order = 1006)
    var sslEnable = false

    @Option(name = "sj", longName = "ssl-jks", help = "jks签名文件", order = 1007)
    var sslJks = ""

    @Option(name = "ss", longName = "ssl-storepass", help = "jks签名文件Store密码", order = 1008)
    var sslStorepass = ""

    // =============================== log ================================== //
    @Option(name = "lv", longName = "log-level", help = "日志等级", order = 2001)
    var logLevel = LogLevel.ALL

    @Option(name = "lf", longName = "log-file", help = "日志文件", order = 3001)
    var logFile = "./logs/t2c.log"

    @Option(name = "lmfs", longName = "log-max-file-size", help = "日志文件大小", order = 2003)
    var logMaxFileSize = "1MB"

    @Option(name = "lmbi", longName = "log-max-backup-index", help = "日志文件备份数量", order = 2004)
    var logMaxBackupIndex = 3


    // =============================== tcp ================================== //
    @Option(name = "la", longName = "local-addr", help = "内网地址", order = 3001)
    var localAddr = "127.0.0.1"

    @Option(name = "lp", longName = "local-port", help = "内网端口", order = 3002)
    var localPort = 80

    @Option(name = "rp", longName = "remote-port", help = "映射外网端口", order = 3003)
    var remotePort = 10080

    // =============================== http(s) ================================== //
    @Option(name = "vh", longName = "vhost", help = "域名", order = 4001)
    var vhost = ""

    @Option(name = "psh", longName = "proxy-set-headers", help = "设置HTTP头", order = 4002)
    var proxySetHeaders = emptyMap<String, String>()

    @Option(name = "pah", longName = "proxy-add-headers", help = "新增HTTP头", order = 4003)
    var proxyAddHeaders = emptyMap<String, String>()

    @Option(name = "ha", longName = "http-auth", help = "是否启用HTTP登录验证", order = 4004)
    var httpAuthEnable = false

    @Option(name = "har", longName = "http-auth-realm", help = "HTTP登录验证Realm", order = 4005)
    var httpAuthRealm = "."

    @Option(name = "hau", longName = "http-auth-username", help = "HTTP登录验证用户名", order = 4006)
    var httpAuthUsername = "tunnel"

    @Option(name = "hap", longName = "http-auth-password", help = "HTTP登录验证用户名", order = 4007)
    var httpAuthPassword = "tunnel"


}