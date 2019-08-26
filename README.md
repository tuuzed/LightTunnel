# Tunnel-内网穿透工具

支持TCP、HTTP、HTTPS穿透。



## 0x01 服务器端配置

```yml
# 绑定地址
bind_addr: 0.0.0.0
# 绑定端口
bind_port: 5000
# 令牌
token: tk123456
# TCP端口白名单
allow_ports: 10000,10001,10002-50000 
# 线程数量
boss_threads: -1                     
worker_threads: -1
# 日志
log:
  # ALL, TRACE, DEBUG, INFO, WARN, ERROR, OFF
  level: INFO
  file: ./logs/tunnels.log
  # KB, MB, GB
  max_file_size: 1MB   
  max_backup_index: 3

# ssl
ssl:
  enable: true
  bind_addr: 0.0.0.0
  bind_port: 5001
  jks: server.jks
  storepass: stunnelpass
  keypass: stunnelpass

# http
http:
  enable: true
  bind_addr: 0.0.0.0
  bind_port: 80

# https
https:
  enable: true
  bind_addr: 0.0.0.0
  bind_port: 443
  jks: server.jks
  storepass: stunnelpass
  keypass: stunnelpass

```



## 0x02 客户端配置

```yml
# 服务器地址
server_addr: 127.0.0.1
# 服务器端口
server_port: 5000
# 令牌
token: tk123456
# 线程数量
worker_threads: -1

log:
  # ALL, TRACE, DEBUG, INFO, WARN, ERROR, OFF
  level: INFO
  file: ./logs/tunnelc.log
  # KB, MB, GB
  max_file_size: 1MB
  max_backup_index: 3

# ssl
ssl:
  server_port: 5001
  jks: client.jks
  storepass: ctunnelpass

# 隧道列表
tunnels:
  # tcp-http
  - proto: tcp
    enable_ssl: true
    local_addr: 192.168.1.1
    local_port: 80
    remote_port: 10080

  # vnc
  - proto: tcp
    enable_ssl: false
    local_addr: 192.168.1.33
    local_port: 5900
    remote_port: 15900

  # ssh
  - proto: tcp
    enable_ssl: false
    local_addr: 127.0.0.1
    local_port: 22
    remote_port: 10022

  # http-vhost
  - proto: http
    enable_ssl: false
    local_addr: 192.168.1.1
    local_port: 80
    vhost: t1.tunnel.lo
    write_headers:
      X-User-Agent: Tunnel
    rewrite_headers:
      X-Real-IP: $remote_addr
    auth:
      enable: true
      realm: User
      username: admin
      password: admin

  # http-vhost
  - proto: http
    enable_ssl: false
    local_addr: 192.168.1.33
    local_port: 10080
    vhost: t2.tunnel.lo
    write_headers:
      X-User-Agent: Tunnel
    rewrite_headers:
      X-Real-IP: $remote_addr
    auth:
      enable: true
      realm: User
      username: admin
      password: admin

  # http-vhost
  - proto: http
    enable_ssl: false
    local_addr: 192.168.1.1
    local_port: 80
    vhost: t1.tunnel.lo
    write_headers:
      X-User-Agent: Tunnel
    rewrite_headers:
      X-Real-IP: $remote_addr   # $remote_addr 将被替换成发起请求的客户端IP
    auth:
      enable: true
      realm: .
      username: admin
      password: admin

  # https-vhost
  - proto: https
    enable_ssl: false
    local_addr: 192.168.1.1
    local_port: 10080
    vhost: t2.tunnel.lo
    write_headers:
      X-User-Agent: Tunnel
    rewrite_headers:
      X-Real-IP: $remote_addr
    auth:
      enable: true
      realm: .
      username: admin
      password: admin
```



## 0x03 SSL证书生成

- 生成服务端证书
```bash

keytool -genkey \
-alias stunnelalias \
-keysize 2048 \
-validity 365 \
-keyalg RSA \
-dname "CN=tunnel" \
-keypass stunnelpass \
-storepass stunnelpass \
-keystore tunnels.jks

keytool -importkeystore \
-srckeystore tunnels.jks \
-destkeystore tunnels.jks \
-deststoretype pkcs12

keytool -export \
-alias stunnelalias \
-keystore tunnels.jks \
-storepass stunnelpass \
-file tunnels.cer

```
- 生成客户端证书
```bash

keytool -genkey \
-alias ctunnelalias \
-keysize 2048 \
-validity 365 \
-keyalg RSA 
-dname "CN=tunnel" \
-keypass ctunnelpass 
-storepass ctunnelpass \
-keystore tunnelc.jks

keytool -importkeystore \
-srckeystore tunnelc.jks \
-destkeystore tunnelc.jks 
-deststoretype pkcs12

keytool -import -trustcacerts \
-alias stunnalalias \
-file tunnels.cer \
-storepass ctunnelpass \
-keystore tunnelc.jks

```
