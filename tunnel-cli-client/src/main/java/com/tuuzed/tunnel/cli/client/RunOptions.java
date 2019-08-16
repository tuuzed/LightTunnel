package com.tuuzed.tunnel.cli.client;

import com.tuuzed.tunnel.cli.common.Option;
import com.tuuzed.tunnel.common.proto.Proto;

import java.util.Collections;
import java.util.Map;

public final class RunOptions {

    @Option(name = "c", longName = "config-file", help = "配置文件，当设置了配置文件时优先使用配置文件配置项", order = -1)
    public String configFile = "";

    @Option(name = "wt", longName = "worker-threads", help = "Worker线程数量", order = 10)
    public int workerThreads = -1;

    @Option(name = "pro", longName = "proto", help = "协议", order = 20, excludeEnums = {"UNKNOWN"})
    public Proto proto = Proto.TCP;

    // ssl
    @Option(name = "ssl", longName = "ssl", help = "是否启用SSL", order = 40)
    public boolean sslEnable = false;

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

    // http and https
    @Option(name = "vh", longName = "vhost", help = "域名", order = 130)
    public String vhost = "";

    @Option(name = "sh", longName = "set-headers", help = "设置HTTP头", order = 140)
    public Map<String, String> setHeaders = Collections.emptyMap();

    @Option(name = "ah", longName = "add-headers", help = "新增HTTP头", order = 150)
    public Map<String, String> addHeaders = Collections.emptyMap();

    @Option(name = "auth", longName = "auth", help = "是否启用HTTP登录验证", order = 160)
    public boolean authEnable = false;

    @Option(name = "ar", longName = "auth-realm", help = "HTTP登录验证Realm", order = 170)
    public String authRealm = ".";

    @Option(name = "au", longName = "auth-username", help = "HTTP登录验证用户名", order = 180)
    public String authUsername = ".";

    @Option(name = "ap", longName = "auth-password", help = "HTTP登录验证用户名", order = 190)
    public String authPassword = ".";


}