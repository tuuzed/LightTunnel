# Tunnel-内网穿透工具

支持TCP、HTTP、HTTPS穿透。



## 0x01 服务器端配置

```yml
## t2s.yml

# 绑定地址
bind_addr: 0.0.0.0
# 绑定端口
bind_port: 5000
# 令牌
auth_token: tk123456
# TCP端口白名单
allow_ports: 10000,10001,10002-50000
# 线程数量
boss_threads: -1
worker_threads: -1

# 日志
log:
  # ALL, TRACE, DEBUG, INFO, WARN, ERROR, OFF
  level: INFO
  file: ./logs/t2s.log
  # KB, MB, GB
  max_file_size: 1MB
  max_backup_index: 3

# ssl
ssl:
  enable: true
  bind_addr: 0.0.0.0
  bind_port: 5001
  jks: t2s.jks
  storepass: t2spass
  keypass: t2spass

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
  jks: t2s.jks
  storepass: t2spass
  keypass: t2spass

```



## 0x02 客户端配置

```yml
## t2c.yml

# 服务器地址
server_addr: 127.0.0.1
# 服务器端口
server_port: 5000
# 令牌
auth_token: tk123456
# 线程数量
worker_threads: -1

log:
  # ALL, TRACE, DEBUG, INFO, WARN, ERROR, OFF
  level: DEBUG
  file: ./logs/t2c.log
  # KB, MB, GB
  max_file_size: 1MB
  max_backup_index: 3

# ssl
ssl:
  server_port: 5001
  jks: t2c.jks
  storepass: t2cpass

# 隧道列表
tunnels:
  # tcp-http
  - type: tcp
    enable_ssl: true
    local_addr: 192.168.1.1
    local_port: 80
    remote_port: 10080

  # vnc
  - type: tcp
    enable_ssl: false
    local_addr: 192.168.1.33
    local_port: 5900
    remote_port: 15900

  # ssh
  - type: tcp
    enable_ssl: false
    local_addr: 127.0.0.1
    local_port: 22
    remote_port: 10022

  # http-vhost
  - type: http
    enable_ssl: false
    local_addr: 192.168.1.1
    local_port: 80
    host: t1.tunnel.lo
    proxy_set_headers:
      X-Real-IP: $remote_addr
    proxy_add_headers:
      X-User-Agent: Tunnel
    auth:
      enable: true
      realm: User
      username: admin
      password: admin

  # http-vhost
  - type: http
    enable_ssl: false
    local_addr: 111.230.198.37
    local_port: 10080
    host: t2.tunnel.lo
    proxy_set_headers:
      X-Real-IP: $remote_addr
    proxy_add_headers:
      X-User-Agent: Tunnel
    auth:
      enable: true
      realm: User
      username: admin
      password: admin

  # https-vhost
  - type: https
    enable_ssl: false
    local_addr: 192.168.1.1
    local_port: 80
    host: t1.tunnel.lo
    proxy_set_headers:
      X-Real-IP: $remote_addr # $remote_addr 将被替换成发起请求的客户端IP
    proxy_add_headers:
      X-User-Agent: Tunnel
    auth:
      enable: true
      realm: .
      username: admin
      password: admin

  # https-vhost
  - type: https
    enable_ssl: false
    local_addr: 192.168.1.1
    local_port: 10080
    host: t2.tunnel.lo
    proxy_set_headers:
      X-Real-IP: $remote_addr
    proxy_add_headers:
      X-User-Agent: Tunnel
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
-alias t2salias \
-keysize 2048 \
-validity 3650 \
-keyalg RSA \
-dname "CN=tunnel" \
-keypass t2spass \
-storepass t2spass \
-keystore t2s.jks

keytool -importkeystore \
-srckeystore t2s.jks \
-destkeystore t2s.jks \
-deststoretype pkcs12

keytool -export \
-alias t2salias \
-keystore t2s.jks \
-storepass t2spass \
-file t2s.cer

```
- 生成客户端证书
```bash

keytool -genkey \
-alias t2calias \
-keysize 2048 \
-validity 3650 \
-keyalg RSA \
-dname "CN=tunnel" \
-keypass t2cpass \
-storepass t2cpass \
-keystore t2c.jks

keytool -importkeystore \
-srckeystore t2c.jks \
-destkeystore t2c.jks \
-deststoretype pkcs12

keytool -import -trustcacerts \
-alias t2salias \
-file t2s.cer \
-storepass t2cpass \
-keystore t2c.jks

```
