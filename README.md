# LightTunnel-内网穿透工具

支持TCP、HTTP、HTTPS穿透。

[![Java CI](https://github.com/tuuzed/LightTunnel/workflows/Java%20CI/badge.svg)](https://github.com/tuuzed/LightTunnel.git)
[![LastVersion](https://img.shields.io/badge/LightTunnel-v0.9.0-blue.svg)](https://github.com/tuuzed/LightTunnel.git)
[![Thanks](https://img.shields.io/badge/Thanks-jetbrains.com-green.svg)](https://jetbrains.com)
[![Download](https://img.shields.io/badge/Download-v0.9.0.tar.gz-orange.svg)](https://github.com/tuuzed/LightTunnel/releases/download/v0.9.0/LightTunnel-v0.9.0.tar.gz)


## 0x01 文件说明

- 服务器端文件
```
 lts.exe       - 服务器端windows可执行文件
 lts.sh        - 服务器端*nix可执行文件
 lts.jar       - 服务器端jvm可执行文件
 lts.ini       - 服务器端最小配置文件   
 lts_full.ini  - 服务器端完整配置文件   
 lts.jks       - 服务器端SSL证书
```
- 客户端文件
``` 
 ltc.exe       - 客户端windows可执行文件
 ltc.sh        - 客户端*nix可执行文件
 ltc.jar       - 客户端jvm可执行文件
 ltc.ini       - 客户端最小配置文件
 ltc_full.ini  - 客户端完整配置文件   
 ltc.jks       - 客户端SSL证书
```


## 0x02 启动命令

- windows系统
  
  - 服务器端
  ```shell script
  .\lts.exe -c lts.ini
  ```
  
  - 客户端
  ```shell script
  .\ltc.exe -c ltc.ini
  ```

    
- *nix系统

  - 服务器端
  ```shell script
  ./lts.sh -c lts.ini
  ```
  
  - 客户端
  ```shell script
  ./ltc.sh -c ltc.ini
  ```

- jvm

  - 服务器端
  ```shell script
  java -jar lts.jar -c lts.ini
  ```
  
  - 客户端
  ```shell script
  java -jar ltc.jar -c ltc.ini
  ```
  
## 0x03 配置文件

- 服务器端完整配置参考

  ```ini
    [basic]
    # 绑定IP地址
    bind_addr = 0.0.0.0
    # 绑定端口号
    bind_port = 5080
    # 线程数，值为-1时不限制线程数量
    boss_threads = -1
    worker_threads = -1
    
    # 验证Token
    auth_token = tk123456
    
    # TCP隧道允许使用的端口
    allow_ports = 10000,10001,10002-50000
    
    # ssl
    # ssl绑定端口号，为空时不启用http服务
    ssl_bind_port = 5443
    # ssl jks证书文件，加载失败时会默认使用程序内建的证书
    ssl_jks = lts.jks
    ssl_key_password = ltspass
    ssl_store_password = ltspass
    
    # http
    # http服务绑定端口，为空时不启用http服务
    http_port = 8080
    
    # https
    # https服务绑定端口，为空时不启用https服务
    https_port = 8443
    # https jks证书文件，加载失败时会默认使用程序内建的证书
    https_jks = lts.jks
    https_key_password = ltspass
    https_store_password = ltspass
    
    # 静态文件,启用了http或https服务时有效
    # 路径列表
    plugin_sf_paths = /var/www/html,/usr/shard/nginx/html
    # 需要数量静态文件的host列表
    plugin_sf_hosts = static1.yourdomain.com,static2.yourdomain.com
    
    # Http Rpc绑定端口，为空时不启用Http Rpc
    http_rpc_port = 5081
    
    # 日志
    # 日志等级由低到高 ALL, TRACE, DEBUG, INFO, WARN, ERROR, OFF
    log_level = INFO
    # 日志文件,为空时不保存日志文件
    log_file = ./logs/lts.log
    # 保持的日志文件数量
    log_count = 3
    # 单个日志文件大小，支持KB、MB、GB单位
    log_size = 1MB
  ```

- 客户端完整配置参考

  ```ini
    [basic]
    # 服务器地址
    server_addr = 127.0.0.1
    # 服务器端口
    server_port = 5080
    
    # 线程数，值为-1时不限制线程数量
    worker_threads = -1
    
    # 验证Token需要与服务器一致
    auth_token = tk123456
    
    # ssl jks证书文件，当隧道列表中有使用了ssl时生效，加载失败时会默认使用程序内建的证书
    ssl_server_port = 5443
    ssl_jks = ltc.jks
    ssl_store_password = ltcpass
    
    # Http Rpc绑定端口，为空时不启用Http Rpc
    http_rpc_port = 5082
    
    # 日志
    # 日志等级由低到高 ALL, TRACE, DEBUG, INFO, WARN, ERROR, OFF
    log_level = INFO
    # 日志文件,为空时不保存日志文件
    log_file = ./logs/lts.log
    # 保持的日志文件数量
    log_count = 3
    # 单个日志文件大小，支持KB、MB、GB单位
    log_size = 1MB
    
    
    # 隧道列表
    # 命名空间相同的隧道会被覆盖掉
    [tcp]
    # 隧道类型，支持tcp,http,https
    type = tcp
    # 是否使用ssl连接，true|false
    ssl = true
    # 本地地址
    local_addr = 192.168.1.1
    # 本地端口
    local_port = 80
    # 隧道服务器端口，端口号设置成0时，为随机端口
    remote_port = 10080
    
    [tcp-random]
    type = tcp
    ssl = true
    local_addr = 192.168.1.1
    local_port = 80
    remote_port = 0
    
    [http]
    # 隧道类型，支持tcp,http,https
    type = http
    # 是否使用ssl连接，true|false
    ssl = true
    # 本地地址
    local_addr = 192.168.1.1
    # 本地端口
    local_port = 80
    # 自定义域名，需要域名DNS设置指向服务器地址
    host = t1.tunnel.lo
    # 代理请求头设置，其中$remote_addr为魔法变量，最终会替换成用户的真实IP
    pxy_header_set_X-Real-IP = $remote_addr
    # 代理请求头新增
    pxy_header_add_X-User-Agent = LightTunnel
    # 是否启用登录验证
    auth_enable = true
    # 登录验证信息
    auth_realm = .
    auth_username = guest
    auth_password = guest
    
    [https]
    # 隧道类型，支持tcp,http,https
    type = https
    # 是否使用ssl连接，true|false
    ssl = true
    # 本地地址
    local_addr = 192.168.1.1
    # 本地端口
    local_port = 80
    # 自定义域名，需要域名DNS设置指向服务器地址
    host = t1.tunnel.lo
    # 代理请求头设置，其中$remote_addr为魔法变量，最终会替换成用户的真实IP
    pxy_header_set_X-Real-IP = $remote_addr
    # 代理请求头新增
    pxy_header_add_X-User-Agent = LightTunnel
    # 是否启用登录验证
    auth_enable = true
    # 登录验证信息
    auth_realm = .
    auth_username = guest
    auth_password = guest
  ```

## 0x04 自定义SSL证书生成

- 生成服务端证书
    ```bash
    keytool -genkey \
    -alias ltsalias \
    -keysize 2048 \
    -validity 3650 \
    -keyalg RSA \
    -dname "CN=LTS" \
    -keypass ltspass \
    -storepass ltspass \
    -keystore lts.jks
    
    keytool -importkeystore \
    -srckeystore lts.jks \
    -destkeystore lts.jks \
    -deststoretype pkcs12
    
    keytool -export \
    -alias ltsalias \
    -keystore lts.jks \
    -storepass ltspass \
    -file lts.cer
    ```
- 生成客户端证书
    ```bash
    keytool -genkey \
    -alias ltcalias \
    -keysize 2048 \
    -validity 3650 \
    -keyalg RSA \
    -dname "CN=LTC" \
    -keypass ltcpass \
    -storepass ltcpass \
    -keystore ltc.jks
    
    keytool -importkeystore \
    -srckeystore ltc.jks \
    -destkeystore ltc.jks \
    -deststoretype pkcs12
    
    keytool -import -trustcacerts \
    -alias ttsalias \
    -file lts.cer \
    -storepass ltcpass \
    -keystore ltc.jks
    ```
