package keep;

import com.tuuzed.tunnel.common.proto.Proto;
import com.tuuzed.tunnel.commoncli.LogLevel;
import com.tuuzed.tunnel.commoncli.Option;

import java.util.Collections;
import java.util.Map;

public final class RunOptions {

    @Option(name = "c", longName = "config-file", help = "配置文件，当设置了配置文件时优先使用配置文件配置项", order = -1)
    public String configFile = "";

    // =============================== common ================================== //
    @Option(name = "wt", longName = "worker-threads", help = "Worker线程数量", order = 1001)
    public int workerThreads = -1;

    @Option(name = "pro", longName = "proto", help = "协议", excludeEnums = {"UNKNOWN"}, order = 1002)
    public Proto proto = Proto.TCP;

    @Option(name = "s", longName = "server-addr", help = "服务器地址", order = 1003)
    public String serverAddr = "127.0.0.1";

    @Option(name = "p", longName = "server-port", help = "服务器端口", order = 1004)
    public int serverPort = 5000;

    @Option(name = "t", longName = "token", help = "令牌", order = 1005)
    public String token = "";

    @Option(name = "ssl", longName = "ssl", help = "是否启用SSL", order = 1006)
    public boolean sslEnable = false;

    @Option(name = "sj", longName = "ssl-jks", help = "jks签名文件", order = 1007)
    public String sslJks = "";

    @Option(name = "ss", longName = "ssl-storepass", help = "jks签名文件Store密码", order = 1008)
    public String sslStorepass = "";

    // =============================== log ================================== //
    @Option(name = "lv", longName = "log-level", help = "日志等级", order = 2001)
    public LogLevel logLevel = LogLevel.ALL;

    @Option(name = "lf", longName = "log-file", help = "日志文件", order = 3001)
    public String logFile = "./logs/tunnel-server.log";

    @Option(name = "lmfs", longName = "log-max-file-size", help = "日志文件大小", order = 2003)
    public String logMaxFileSize = "1MB";

    @Option(name = "lmbi", longName = "log-max-backup-index", help = "日志文件备份数量", order = 2004)
    public int logMaxBackupIndex = 3;


    // =============================== tcp ================================== //
    @Option(name = "la", longName = "local-addr", help = "内网地址", order = 3001)
    public String localAddr = "127.0.0.1";

    @Option(name = "lp", longName = "local-port", help = "内网端口", order = 3002)
    public int localPort = 80;

    @Option(name = "rp", longName = "remote-port", help = "映射外网端口", order = 3003)
    public int remotePort = 10080;

    // =============================== http(s) ================================== //
    @Option(name = "vh", longName = "vhost", help = "域名", order = 4001)
    public String vhost = "";

    @Option(name = "rwh", longName = "rewrite-headers", help = "设置HTTP头", order = 4002)
    public Map<String, String> setHeaders = Collections.emptyMap();

    @Option(name = "wh", longName = "write-headers", help = "新增HTTP头", order = 4003)
    public Map<String, String> addHeaders = Collections.emptyMap();

    @Option(name = "auth", longName = "auth", help = "是否启用HTTP登录验证", order = 4004)
    public boolean authEnable = false;

    @Option(name = "ar", longName = "auth-realm", help = "HTTP登录验证Realm", order = 4005)
    public String authRealm = ".";

    @Option(name = "au", longName = "auth-username", help = "HTTP登录验证用户名", order = 4006)
    public String authUsername = ".";

    @Option(name = "ap", longName = "auth-password", help = "HTTP登录验证用户名", order = 4007)
    public String authPassword = ".";


}