# Tunnel-内网映射工具

## TCP Tunnel

```yml
tunnels:
  - proto: tcp                 // 指定协议为TCP
    enable_ssl: false          // 是否启用SSL
    local_addr: 192.168.1.10   // 映射本地地址
    local_port: 22             // 映射本地端口
    remote_port: 10022         // 映射远程端口
```

## HTTP Tunnel

```yml
tunnels:
  - proto: http                // 指定协议为HTTP
    enable_ssl: false          // 是否启用SSL
    local_addr: apache.org     // 映射本地地址
    local_port: 80             // 映射本地端口
    vhost: t2.tunnel.lo        // 映射域名
    set_headers:               // 设置HTTP响应头
      X-Real-IP: $remote_addr  // $remote_addr将被替换成发起请求的客户端IP
      Host: apache.org
    add_headers:               // 添加HTTP响应头
      X-User-Agent: Tunnel
```

## SSL证书生成
- 生成服务端证书
```bash

keytool -genkey -alias stunnelalias -keysize 2048 -validity 365 -keyalg RSA -dname "CN=tunnel" -keypass stunnelpass -storepass stunnelpass -keystore tunnel-server.jks
keytool -importkeystore -srckeystore tunnel-server.jks -destkeystore tunnel-server.jks -deststoretype pkcs12
keytool -export -alias stunnelalias -keystore tunnel-server.jks -storepass stunnelpass -file tunnel-server.cer

```
- 生成客户端证书
```bash

keytool -genkey -alias ctunnelalias -keysize 2048 -validity 365 -keyalg RSA -dname "CN=tunnel" -keypass ctunnelpass -storepass ctunnelpass -keystore tunnel-client.jks
keytool -importkeystore -srckeystore tunnel-client.jks -destkeystore tunnel-client.jks -deststoretype pkcs12
keytool -import -trustcacerts -alias stunnalalias -file tunnel-server.cer -storepass ctunnelpass -keystore tunnel-client.jks

```
